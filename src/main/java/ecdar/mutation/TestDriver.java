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
    private int step = 0;

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
        return getPlan().shouldStop();
    }

    /**
     * Starts testing in new thread.
     * When done, the result consumer is called.
     */
    public void start() {
        // Start process
        try {
            sut = Runtime.getRuntime().exec("java -jar " + Ecdar.projectDirectory.get() + File.separator + getPlan().getSutPath().replace("/", File.separator));
        } catch (IOException e) {
            handleException(e);
            return;
        }

        writer = new BufferedWriter(new OutputStreamWriter(sut.getOutputStream()));
        reader = new AsyncInputReader(sut);

        timeHandler = MutationTestTimeHandler.getHandler(getPlan(), this::handleException, this::writeToSut, reader);
        timeHandler.onTestStart();

        runStep();
    }

    private NonRefinementStrategy getStrategy() {
        return testCase.getStrategy();
    }

    private void runStep() {
        if (step >= getStepBounds()) {
            handIn(TestResult.Verdict.OUT_OF_BOUNDS, "Out of bounds.");
            return;
        }

        if (shouldStop()) {
            resultConsumer.accept(null);
            tearDown();
            return;
        }

        step++;

        // Get rule and check if its empty
        final StrategyRule rule = getStrategy().getRule(testModelSimulation, mutantSimulation);
        if (rule == null) {
            handIn(TestResult.Verdict.NO_RULE, "No rule to perform.");
            return;
        }

        // Check if rule is an delay rule or output action rule, if it is either, perform delay,
        // if it is an input action perform input
        if (rule instanceof DelayRule || (rule instanceof ActionRule && ((ActionRule) rule).getStatus() == EdgeStatus.OUTPUT)) {
            runDelayRule(rule);
        } else if (rule instanceof  ActionRule){
            runInputRule(rule);
        } else {
            handleException(new MutationTestingException("Rule " + rule + " is neither a delay nor an action rule."));
        }

    }

    private void runInputRule(final StrategyRule rule) {
        final String sync = ((ActionRule) rule).getSync();
        if (!testModelSimulation.isDeterministic(sync, EdgeStatus.INPUT)) {
            handIn(TestResult.Verdict.NON_DETERMINISM, "Non-deterministic choice for test model with input " + sync + ".");
        } else if (!mutantSimulation.isDeterministic(sync, EdgeStatus.INPUT)) {
            handIn(TestResult.Verdict.NON_DETERMINISM, "Non-deterministic choice for mutant with input " + sync + ".");
        } else {
            try {
                testModelSimulation.runInputAction(sync);
                mutantSimulation.runInputAction(sync);
                writeToSut(sync);
                runStep();
            } catch (MutationTestingException | IOException e) {
                handleException(e);
            }
        }
    }

    private void runDelayRule(final StrategyRule rule) {
        timeHandler.onNewDelayRule();
        delay(rule);
    }

    /**
     * Sleeps and simulates the delay on the two simulations and checks if the system under test made an output during the delay.
     * @param rule strategy rule to check with. This methods sleeps only as long as this is satisfied.
     * @return the test result (if this concludes the test), or null (if it does not)
     * @throws MutationTestingException if an error related to mutation testing happens
     * @throws IOException if an IO error happens
     * @throws InterruptedException if an error happens when trying to sleep
     */
    private void delay(final StrategyRule rule) {
        Map<String, Double> clockValuations = getClockValuations();

        if (shouldStop()) {
            resultConsumer.accept(null);
            tearDown();
            return;
        }

        // If rule is no longer satisfied, run a new step
        if (!rule.isSatisfied(getClockValuations())) {
            runStep();
            return;
        }

        // Check if the maximum wait time has been exceeded, if it is, give inconclusive verdict
        if (timeHandler.isMaxWaitTimeExceeded()) {
            handIn(TestResult.Verdict.MAX_WAIT, "Maximum wait time reached without receiving an output.");
            return;
        }

        // If SUT has an output
        try {
            if (reader.ready()) {
                final String output = reader.consume();

                final TestResult delayResult = simulateDelay();
                if (delayResult != null) {
                    handIn(delayResult);
                    return;
                }

                final TestResult outputResult = simulateOutput(output);
                if (outputResult != null) {
                    handIn(outputResult);
                    return;
                }

                // Output from SUT was OK, try delaying again
                delay(rule);
                return;
            }
        } catch (IOException | MutationTestingException e) {
            handleException(e);
        }

        timeHandler.sleep(new SingleRunnable(() -> {
            final TestResult delayResult = simulateDelay();
            if (delayResult != null) {
                handIn(delayResult);
                return;
            }

            delay(rule);
        }));
    }

    private void handleException(final Exception exception) {
        synchronized (getPlan()) {
            if (getPlan().getStatus().equals(MutationTestPlan.Status.WORKING)) {
                getPlan().setStatus(MutationTestPlan.Status.ERROR);
                Platform.runLater(() -> {
                    final String errorMessage = "Error while running test-case " + testCase.getId() + ": " + exception.getMessage();
                    final Text text = new Text(errorMessage);
                    text.setFill(Color.RED);
                    writeProgress(text);
                    Ecdar.showToast(errorMessage);
                });
            }
        }

        exception.printStackTrace();

        tearDown();
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
            final String reason = "Failed simulating delay on test model";
            if(mutantSimulation.delay(waitedTimeUnits)) return makeResult(TestResult.Verdict.FAIL_PRIMARY, reason);
            else return makeResult(TestResult.Verdict.FAIL_NORMAL, reason);
        } else if (!mutantSimulation.delay(waitedTimeUnits)) {
            return makeResult(TestResult.Verdict.MUT_NO_DELAY, "Could not simulate delay on mutant");
        }

        return null;
    }

    /**
     * Simulates an output on the test model and mutant model.
     * @param output the output to simulate
     * @throws MutationTestingException if an exception occurred
     * @return the test result (if this concludes the test), or null (if it does not)
     */
    private TestResult simulateOutput(final String output) {
        try {
            if (!testModelSimulation.isDeterministic(output, EdgeStatus.OUTPUT)) {
                return makeResult(TestResult.Verdict.NON_DETERMINISM, "Non-deterministic choice for test model with output " + output + ".");
            } else if (!mutantSimulation.isDeterministic(output, EdgeStatus.OUTPUT)) {
                return makeResult(TestResult.Verdict.NON_DETERMINISM, "Non-deterministic choice with mutant output " + output + ".");
            } else if (!testModelSimulation.runOutputAction(output)){
                final String reason = "Failed simulating output " + output + " on test model.";
                if (mutantSimulation.runOutputAction(output)) return makeResult(TestResult.Verdict.FAIL_PRIMARY, reason);
                else return makeResult(TestResult.Verdict.FAIL_NORMAL, reason);
            } else if (!mutantSimulation.runOutputAction(output)){
                return makeResult(TestResult.Verdict.PASS, null);
            }
        } catch (MutationTestingException e) {
            e.printStackTrace();
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



    private void handIn(final TestResult.Verdict verdict, final String reason) {
        handIn(makeResult(verdict, reason));
    }

    private void handIn(final TestResult result) {
        resultConsumer.accept(result);
        tearDown();
    }

    /**
     * Constructs a test result.
     * @param verdict the verdict of the test
     * @param reason the reason for that verdict
     * @return the test result
     */
    private TestResult makeResult(final TestResult.Verdict verdict, final String reason) {
        return new TestResult(testCase, reason, testModelSimulation, verdict);
    }

    /**
     * Destroys the system under test.
     * This will automatically close the reader that reads outputs from the system under test.
     */
    private void tearDown() {
        sut.destroy();
    }
}
