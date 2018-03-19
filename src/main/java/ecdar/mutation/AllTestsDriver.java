package ecdar.mutation;

import ecdar.mutation.models.MutationTestCase;
import ecdar.mutation.models.MutationTestPlan;
import ecdar.mutation.models.TestResult;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

/**
 * A test driver that runs testcases on a system under test (sut).
 */
public class AllTestsDriver implements ConcurrentJobsHandler {
    private int passedNum;
    private final MutationTestPlan testPlan;
    private final int timeUnit;
    private final int bound;
    private final List<MutationTestCase> mutationTestCases;
    private ConcurrentJobsDriver jobsDriver;
    private Instant jobsStart;


    /**
     * Constructor for the test driver, needs a list of mutation test cases, a test plan, a consumer to write progress to, an long representing a time units length in miliseconds and a bound
     */
    AllTestsDriver(final List<MutationTestCase> mutationTestCases, final MutationTestPlan testPlan, final int timeUnit, final int bound) {
        this.mutationTestCases = mutationTestCases;
        this.testPlan = testPlan;
        this.timeUnit = timeUnit;
        this.bound = bound;
    }

    /**
     * Starts the test driver.
     */
    public void start() {
        passedNum = 0;
        jobsDriver = new ConcurrentJobsDriver(this, mutationTestCases.size());

        Platform.runLater(() -> {
            getPlan().setInconclusiveText("Inconclusive: " + 0);
            getPlan().setPassedText("Passed: " + 0);
            getPlan().setFailedText("Failed: " + 0);
        });

        jobsStart = Instant.now();

        jobsDriver.start();
    }

    /**
     * Performs a testcase on the testplans system under test(sut).
     * @param testCase to perform.
     */
    private void performTest(final MutationTestCase testCase) {
        new TestDriver(testCase, timeUnit, bound, getPlan(), this::onTestDone).start();
    }

    /**
     * Is triggered when a test-case execution is done.
     * It updates UI labels to tell user about the progress.
     * It also updates the jobsDriver about the job progress.
     * @param result the test result
     */
    private synchronized void onTestDone(final TestResult result) {
        switch (result.getVerdict()) {
            case INCONCLUSIVE:
                Platform.runLater(() -> {
                    getPlan().getInconclusiveMessageList().add(result);
                    getPlan().setInconclusiveText("Inconclusive: " + getPlan().getInconclusiveMessageList().size());
                });
                break;
            case PASS:
                passedNum++;
                Platform.runLater(() -> getPlan().setPassedText("Passed: " + passedNum));
                break;
            case FAIL:
                Platform.runLater(() -> {
                    getPlan().getFailedMessageList().add(result);
                    getPlan().setFailedText("Failed: " + getPlan().getFailedMessageList().size());
                });
                break;
        }

        Platform.runLater(() -> getPlan().setTestTimeText(
                "Testing time: " + MutationTestPlanPresentation.readableFormat(Duration.between(jobsStart, Instant.now()))
        ));

        jobsDriver.onJobDone();
    }

    @Override
    public boolean shouldStop() {
        return getPlan().getStatus().equals(MutationTestPlan.Status.STOPPING) ||
                getPlan().getStatus().equals(MutationTestPlan.Status.ERROR);
    }

    @Override
    public void onStopped() { Platform.runLater(() -> getPlan().setStatus(MutationTestPlan.Status.IDLE)); }

    @Override
    public void onAllJobsSuccessfullyDone() {
        final Text text = new Text("Done");
        text.setFill(Color.GREEN);
        writeProgress(text);
        getPlan().setStatus(MutationTestPlan.Status.IDLE);
    }

    @Override
    public void writeProgress(final int jobsEnded) {
        writeProgress("Testing... (" + jobsEnded + "/" + mutationTestCases.size() + " test-cases)");
    }

    /**
     * Writes progress.
     * @param message the message describing the progress
     */
    private void writeProgress(final String message) {
        final Text text = new Text(message);
        text.setFill(Color.web("#333333"));
        writeProgress(text);
    }

    /**
     * Writes progress in a java fx thread.
     * @param text the text describing the progress
     */
    private void writeProgress(final Text text) {
        Platform.runLater(() -> getProgressWriter().accept(text));
    }

    @Override
    public int getMaxConcurrentJobs() {
        return getPlan().getConcurrentSutInstances();
    }

    @Override
    public void startJob(final int index) {
        performTest(mutationTestCases.get(index));
    }

    private Consumer<Text> getProgressWriter() { return text -> getPlan().writeProgress(text); }

    private MutationTestPlan getPlan() {return testPlan; }
}
