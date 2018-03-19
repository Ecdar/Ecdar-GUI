package ecdar.mutation;

import ecdar.Ecdar;
import ecdar.abstractions.EdgeStatus;
import ecdar.mutation.models.*;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestDriver {
    private final MutationTestCase testCase;
    private final int timeUnit, stepBound;
    private final MutationTestPlan plan;
    private boolean done = false;
    private AsyncInputReader reader;
    private final Consumer<TestResult> resultConsumer;
    private Process sut;
    private final SimpleComponentSimulation testModelSimulation, mutantSimulation;
    private BufferedWriter writer;

    TestDriver(final MutationTestCase testCase, final int timeUnit, final int stepBound, final MutationTestPlan plan, final Consumer<TestResult> resultConsumer) {
        this.testCase = testCase;
        this.timeUnit = timeUnit;
        this.stepBound = stepBound;
        this.plan = plan;
        this.resultConsumer = resultConsumer;

        testModelSimulation = new SimpleComponentSimulation(testCase.getTestModel());
        mutantSimulation = new SimpleComponentSimulation(testCase.getMutant());
    }

    /* Properties */

    private MutationTestPlan getPlan() {
        return plan;
    }

    private int getStepBound() {
        return stepBound;
    }

    private boolean isDone() {
        return done;
    }

    /* Other */

    public void start() {
        new Thread(() -> {
            try {
                test();
            } catch (MutationTestingException | IOException | InterruptedException e) {
                if (getPlan().getStatus().equals(MutationTestPlan.Status.WORKING)) {
                    getPlan().setStatus(MutationTestPlan.Status.ERROR);
                    Platform.runLater(() -> {
                        final String errorMessage = "Error while running test-case " + testCase.getId() + ", " +
                                testCase.getDescription() + ": " + e.getMessage();
                        final Text text = new Text(errorMessage);
                        text.setFill(Color.RED);
                        writeProgress(text);
                        Ecdar.showToast(errorMessage);
                        e.printStackTrace();
                    });
                }
            }
        }).start();
    }

    private void test() throws IOException, MutationTestingException, InterruptedException {
        final ObjectProperty<Instant> lastUpdateTime = new SimpleObjectProperty<>();
        final NonRefinementStrategy strategy = testCase.getStrategy();

        //Start process
        sut = Runtime.getRuntime().exec("java -jar " + Ecdar.projectDirectory.get() + File.separator + getPlan().getSutPath().replace("/", File.separator));
        lastUpdateTime.setValue(Instant.now());
        writer = new BufferedWriter(new OutputStreamWriter(sut.getOutputStream()));

        reader = new AsyncInputReader(sut);

        //Begin the new test
        int step = 0;
        while (!isDone() && step < getStepBound()) {
            // Get rule and check if its empty
            final StrategyRule rule = strategy.getRule(testModelSimulation, mutantSimulation);
            if (rule == null) {
                handIn(TestResult.Verdict.INCONCLUSIVE, "No rule to perform.");
            } else {
                // Check if rule is an delay rule or output action rule, if it is either, perform delay,
                // if it is an input action perform input
                if (rule instanceof DelayRule || (rule instanceof ActionRule && ((ActionRule) rule).getStatus() == EdgeStatus.OUTPUT)) {
                    delay(rule, testModelSimulation, mutantSimulation, lastUpdateTime);
                } else if (rule instanceof  ActionRule){
                    final String sync = ((ActionRule) rule).getSync();
                    if (!testModelSimulation.isDeterministic(sync, EdgeStatus.INPUT) || !mutantSimulation.isDeterministic(sync, EdgeStatus.OUTPUT)){
                        handIn(TestResult.Verdict.INCONCLUSIVE, "Non-deterministic choice with input " + sync + ".");
                    } else {
                        testModelSimulation.runInputAction(sync);
                        mutantSimulation.runInputAction(sync);
                        writeToSut(sync, sut);
                    }
                } else {
                    throw new MutationTestingException("Rule " + rule + " is neither a delay nor an action rule.");
                }
            }
            step++;
        }

        //Finish test, we now know that either the verdict has been set or the time ran out
        handIn(TestResult.Verdict.INCONCLUSIVE, "Out of bounds.");
    }

    /**
     * performs a delay on the simulations and itself and checks if the system under test made an output during the delay.
     * @param testModelSimulation simulation representing the test model.
     * @param mutantSimulation simulation representing the mutated model.
     */
    private void delay(final StrategyRule rule, final SimpleComponentSimulation testModelSimulation,
                       final SimpleComponentSimulation mutantSimulation, final ObjectProperty<Instant> lastUpdateTime) throws MutationTestingException, InterruptedException, IOException {
        final Instant delayDuration = Instant.now();
        Map<String, Double> clockValuations = getClockValuations(testModelSimulation, mutantSimulation);

        // Keep delaying until the rule has been satisfied,
        // Will return inside while loop if maximum wait time is exceeded or an output has been given
        while ((rule.isSatisfied(clockValuations))) {
            //Check if the maximum wait time has been exceeded, if it is, give inconclusive verdict
            if (!(Duration.between(delayDuration, Instant.now()).toMillis()/(double)timeUnit <= getPlan().getOutputWaitTime())) {
                handIn(TestResult.Verdict.INCONCLUSIVE, "Maximum wait time reached without recieving an output.");
                return;
            }

            if (reader.isException()) throw reader.getException();

            if (reader.ready()) {
                final String output = reader.read();

                simulateDelay(testModelSimulation, mutantSimulation, lastUpdateTime);

                //Catch SUT debug commands
                final Matcher match = Pattern.compile("Debug: (.*)").matcher(output);
                if (match.find()) {
                    System.out.println(match.group(0));
                } else {
                    simulateOutput(testModelSimulation, mutantSimulation, output);
                    if (isDone()) return;
                }
            } else {
                sleep();
                simulateDelay(testModelSimulation, mutantSimulation, lastUpdateTime);
                if (isDone()) return;
            }
            clockValuations = getClockValuations(testModelSimulation, mutantSimulation);
        }
    }

    private void sleep() throws InterruptedException {
        Thread.sleep(timeUnit / 4);
    }

    /**
     * Gets the clock valuations from the simulated test model and simulated mutant model
     * @param testModelSimulation test model.
     * @param mutantSimulation mutant model.
     * @return a map of clock valuations, their id and value.
     */
    private static Map<String, Double> getClockValuations(final SimpleComponentSimulation testModelSimulation,
                                                          final SimpleComponentSimulation mutantSimulation) {
        final Map<String, Double> clockValuations = new HashMap<>();
        clockValuations.putAll(testModelSimulation.getFullyQuantifiedClockValuations());
        clockValuations.putAll(mutantSimulation.getFullyQuantifiedClockValuations());
        return clockValuations;
    }



    /**
     * Simulates an output on the test model and mutant model.
     * @param testModelSimulation the test model.
     * @param mutantSimulation the mutant model.
     * @param lastUpdateTime the time we determine the delay length from.
     */
    private void simulateDelay(final SimpleComponentSimulation testModelSimulation, final SimpleComponentSimulation mutantSimulation,
                               final ObjectProperty<Instant> lastUpdateTime) {
        final double waitedTimeUnits = Duration.between(lastUpdateTime.get(), Instant.now()).toMillis() / (double) timeUnit;
        lastUpdateTime.setValue(Instant.now());

        if (!testModelSimulation.delay(waitedTimeUnits)) {
            handIn(TestResult.Verdict.FAIL, "Failed simulating delay on test model.");
        } else if (!mutantSimulation.delay(waitedTimeUnits)) {
            handIn(TestResult.Verdict.PASS, null);
        }

    }

    /**
     * Simulates an output on the test model and mutant model.
     * @param testModelSimulation the test model
     * @param mutantSimulation the mutant model
     * @param output the output to simulate
     * @throws MutationTestingException if an exception occured
     */
    private void simulateOutput(final SimpleComponentSimulation testModelSimulation,
                                final SimpleComponentSimulation mutantSimulation, final String output) throws MutationTestingException {
        if(!testModelSimulation.isDeterministic(output, EdgeStatus.OUTPUT) || !mutantSimulation.isDeterministic(output, EdgeStatus.OUTPUT)){
            handIn(TestResult.Verdict.INCONCLUSIVE, "Non-deterministic choice with output " + output + ".");
        } else if (!testModelSimulation.runOutputAction(output)){
            handIn(TestResult.Verdict.FAIL, "Failed simulating output " + output + " on test model.");
        } else if (!mutantSimulation.runOutputAction(output)){
            handIn(TestResult.Verdict.PASS, null);
        }
    }

    /**
     * Writes to the system.in of the system under test.
     * @param outputBroadcast the string to write to the system under test.
     */
    private void writeToSut(final String outputBroadcast, final Process sut) throws IOException {
        //Write to process if it is alive, else act like the process accepts but ignore all inputs.
        if (sut.isAlive()) {
            writer.write(outputBroadcast + "\n");
            writer.flush();
        }
    }

    /**
     * Writes progress in a java fx thread.
     * @param text the text describing the progress
     */
    private void writeProgress(final Text text) {
        Platform.runLater(() -> getPlan().writeProgress(text));
    }

    private void handIn(final TestResult.Verdict verdict, final String reason) {
        done = true;
        resultConsumer.accept(new TestResult(testCase.getId(), testCase.getDescription(), reason, testModelSimulation, mutantSimulation, verdict));

        tearDown();
    }

    private void tearDown() {
        sut.destroy();
    }
}
