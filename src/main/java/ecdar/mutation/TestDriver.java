package ecdar.mutation;

import ecdar.Ecdar;
import ecdar.abstractions.EdgeStatus;
import ecdar.mutation.models.*;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A driver for running model-based mutation testing for a single test-case.
 */
public class TestDriver {
    private final MutationTestCase testCase;
    private final MutationTestPlan plan;
    private AsyncInputReader reader;
    private final Consumer<TestResult> resultConsumer;
    private Process sut;
    private final SimpleComponentSimulation testModelSimulation, mutantSimulation;
    private BufferedWriter writer;
    private MutationTestTimeHandler timeHandler;

    /**
     * Constructor.
     * @param testCase test-case to run
     * @param plan test plan to fetch information about how to test
     * @param resultConsumer a consumer to be called when testing is done.
     *                       This is always called exactly once.
     *                       If an error happens, or if the test plan signals to stop,
     *                       the consumer is called with null as argument
     */
    TestDriver(final MutationTestCase testCase, final MutationTestPlan plan, final Consumer<TestResult> resultConsumer) {
        this.testCase = testCase;
        this.plan = plan;
        this.resultConsumer = resultConsumer;

        testModelSimulation = new SimpleComponentSimulation(testCase.getTestModel());
        mutantSimulation = new SimpleComponentSimulation(testCase.getMutant());
    }


    /* Properties */

    private MutationTestPlan getPlan() {
        return plan;
    }

    private int getStepBounds() {
        return getPlan().getStepBounds();
    }

    private int getTimeUnitInMs() {
        return getPlan().getTimeUnit();
    }


    /* Other */

    /**
     * If this should stop testing.
     * @return true iff should stop
     */
    private boolean shouldStop() {
        return getPlan().getStatus().equals(MutationTestPlan.Status.STOPPING) ||
                getPlan().getStatus().equals(MutationTestPlan.Status.ERROR);
    }

    /**
     * Starts testing in new thread.
     * When done, the result consumer is called.
     */
    public void start() {
        new Thread(() -> {
            TestResult result = null;

            try {
                result = test();
            } catch (MutationTestingException | IOException | InterruptedException e) {
                if (getPlan().getStatus().equals(MutationTestPlan.Status.WORKING)) {
                    getPlan().setStatus(MutationTestPlan.Status.ERROR);
                    Platform.runLater(() -> {
                        final String errorMessage = "Error while running test-case " + testCase.getId() + ": " + e.getMessage();
                        final Text text = new Text(errorMessage);
                        text.setFill(Color.RED);
                        writeProgress(text);
                        Ecdar.showToast(errorMessage);
                        e.printStackTrace();
                    });
                }
            } finally {
                resultConsumer.accept(result);
                tearDown();
            }
        }).start();
    }

    /**
     * tests.
     * @return the test result, or null if stopped
     * @throws IOException if an IO error happens
     * @throws MutationTestingException if an error related to mutation testing happens
     * @throws InterruptedException if an error happens when trying to sleep
     */
    private TestResult test() throws IOException, MutationTestingException, InterruptedException {
        final NonRefinementStrategy strategy = testCase.getStrategy();

        // Start process
        sut = Runtime.getRuntime().exec("java -jar " + Ecdar.projectDirectory.get() + File.separator + getPlan().getSutPath().replace("/", File.separator));
        writer = new BufferedWriter(new OutputStreamWriter(sut.getOutputStream()));

        reader = new AsyncInputReader(sut);

        timeHandler = MutationTestTimeHandler.getHandler(getPlan(), this::writeToSut, reader);
        timeHandler.onTestStart();

        // Begin the new test
        int step = 0;
        while (step < getStepBounds()) {
            // Get rule and check if its empty
            final StrategyRule rule = strategy.getRule(testModelSimulation, mutantSimulation);
            if (rule == null) {
                return makeResult(TestResult.Verdict.NO_RULE, "No rule to perform.");
            } else {
                // Check if rule is an delay rule or output action rule, if it is either, perform delay,
                // if it is an input action perform input
                if (rule instanceof DelayRule || (rule instanceof ActionRule && ((ActionRule) rule).getStatus() == EdgeStatus.OUTPUT)) {
                    final TestResult result = delay(rule);
                    if (result != null) return result;
                } else if (rule instanceof  ActionRule){
                    final String sync = ((ActionRule) rule).getSync();
                    if (!testModelSimulation.isDeterministic(sync, EdgeStatus.INPUT)) {
                        return makeResult(TestResult.Verdict.NON_DETERMINISM, "Non-deterministic choice for test model with input " + sync + ".");
                    } else if (!mutantSimulation.isDeterministic(sync, EdgeStatus.INPUT)) {
                        return makeResult(TestResult.Verdict.NON_DETERMINISM, "Non-deterministic choice for mutant with input " + sync + ".");
                    } else {
                        testModelSimulation.runInputAction(sync);
                        mutantSimulation.runInputAction(sync);
                        writeToSut(sync);
                    }
                } else {
                    throw new MutationTestingException("Rule " + rule + " is neither a delay nor an action rule.");
                }
            }

            if (shouldStop()) return null;

            step++;
        }

        // Finish test, we ran out of step bounds
        return makeResult(TestResult.Verdict.OUT_OF_BOUNDS, "Out of bounds.");
    }

    /**
     * Sleeps and simulates the delay on the two simulations and checks if the system under test made an output during the delay.
     * @param rule strategy rule to check with. This methods sleeps only as long as this is satisfied.
     * @return the test result (if this concludes the test), or null (if it does not)
     * @throws MutationTestingException if an error related to mutation testing happens
     * @throws IOException if an IO error happens
     * @throws InterruptedException if an error happens when trying to sleep
     */
    private TestResult delay(final StrategyRule rule) throws MutationTestingException, InterruptedException, IOException {
        timeHandler.onNewDelayRule();
        Map<String, Double> clockValuations = getClockValuations();

        // Keep delaying until the rule has been satisfied,
        // Will return inside while loop if maximum wait time is exceeded or an output has been given
        while ((rule.isSatisfied(clockValuations))) {
            //Check if the maximum wait time has been exceeded, if it is, give inconclusive verdict
            if (timeHandler.isMaxWaitTimeExceeded()) {
                return makeResult(TestResult.Verdict.MAX_WAIT, "Maximum wait time reached without receiving an output.");
            }

            if (reader.ready()) {
                final String output = reader.consume();

                final TestResult delayResult = simulateDelay();
                if (delayResult != null) return delayResult;

                final TestResult outputResult = simulateOutput(output);
                if (outputResult != null) return outputResult;
            } else {
                sleep();
                final TestResult result = simulateDelay();
                if (result != null) return result;
            }

            if (shouldStop()) return null;

            clockValuations = getClockValuations();
        }

        return null;
    }

    /**
     * Sleeps for 1/4 of a time unit.
     * @throws InterruptedException if an error occurs
     * @throws IOException if an error occurs
     * @throws MutationTestingException if an error occurs
     */
    private void sleep() throws InterruptedException, IOException, MutationTestingException {
        timeHandler.sleep();
    }

    /**
     * Gets the clock valuations from the simulated test model and simulated mutant model
     * @return a map of clock valuations, their id and value.
     */
    private Map<String, Double> getClockValuations() {
        final Map<String, Double> clockValuations = new HashMap<>();
        clockValuations.putAll(testModelSimulation.getFullyQuantifiedClockValuations());
        clockValuations.putAll(mutantSimulation.getFullyQuantifiedClockValuations());
        return clockValuations;
    }

    /**
     * Simulates an output on the test model and mutant model.
     * @return the test result (if this concludes the test), or null (if it does not)
     */
    private TestResult simulateDelay() {
        final double waitedTimeUnits = timeHandler.getTimeSinceLastTime();

        if (!testModelSimulation.delay(waitedTimeUnits)) {
            return makeResult(TestResult.Verdict.FAIL_NORMAL, "Failed simulating delay on test model.");
        } else if (!mutantSimulation.delay(waitedTimeUnits)) {
            return makeResult(TestResult.Verdict.INCONCLUSIVE, "Could not simulate delay on mutant");
        }

        return null;
    }

    /**
     * Simulates an output on the test model and mutant model.
     * @param output the output to simulate
     * @throws MutationTestingException if an exception occurred
     * @return the test result (if this concludes the test), or null (if it does not)
     */
    private TestResult simulateOutput(final String output) throws MutationTestingException {
        if (!testModelSimulation.isDeterministic(output, EdgeStatus.OUTPUT)) {
            return makeResult(TestResult.Verdict.NON_DETERMINISM, "Non-deterministic choice for test model with output " + output + ".");
        } else if (!mutantSimulation.isDeterministic(output, EdgeStatus.OUTPUT)) {
            return makeResult(TestResult.Verdict.NON_DETERMINISM, "Non-deterministic choice with mutant output " + output + ".");
        } else if (!testModelSimulation.runOutputAction(output)){
            return makeResult(TestResult.Verdict.FAIL_NORMAL, "Failed simulating output " + output + " on test model.");
        } else if (!mutantSimulation.runOutputAction(output)){
            return makeResult(TestResult.Verdict.PASS, null);
        }
        return null;
    }

    /**
     * Writes to the system.in of the system under test.
     * @param outputBroadcast the string to write to the system under test.
     * @throws IOException if an IO error occurs
     */
    public void writeToSut(final String outputBroadcast) throws IOException {
        // Write to process if it is alive, else act like the process accepts but ignore all inputs.
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

    /**
     * Constructs a test result.
     * @param verdict the verdict of the test
     * @param reason the reason for that verdict
     * @return the test result
     */
    private TestResult makeResult(final TestResult.Verdict verdict, final String reason) {
        return new TestResult(testCase, reason, testModelSimulation, mutantSimulation, verdict);
    }

    /**
     * Destroys the system under test.
     * This will automatically close the reader that reads outputs from the system under test.
     */
    private void tearDown() {
        sut.destroy();
    }
}
