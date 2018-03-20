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
 * A test driver that runs test-cases on a system under test (sut).
 */
public class TestingHandler implements AdjustableConcurrentJobsHandler {
    private final MutationTestPlan testPlan;
    private List<MutationTestCase> mutationTestCases;
    private Instant testStart;
    private final AdjustableConcurrentJobsDriver jobsDriver;


    /**
     * Constructs.
     * @param testPlan
     */
    TestingHandler(final MutationTestPlan testPlan) {
        this.testPlan = testPlan;
        this.jobsDriver = new AdjustableConcurrentJobsDriver(this);
    }


    /* Properties */

    @Override
    public int getMaxConcurrentJobs() {
        return getPlan().getConcurrentSutInstances();
    }

    private Consumer<Text> getProgressWriter() { return text -> getPlan().writeProgress(text); }

    private MutationTestPlan getPlan() {return testPlan; }


    /* Other */

    public void retest(final List<MutationTestCase> cases) {
        cases.forEach(testCase -> jobsDriver.addJob(() -> performTest(testCase)));

        testStart = null; // Do not measure time when retesting
        jobsDriver.start();
    }

    /**
     * Starts the test driver.
     */
    public void testFromScratch(final List<MutationTestCase> cases) {
        cases.forEach(testCase -> jobsDriver.addJob(() -> performTest(testCase)));

        Platform.runLater(() -> {
            getPlan().setInconclusiveText("Inconclusive: " + 0);
            getPlan().setPassedText("Passed: " + 0);
            getPlan().setFailedText("Failed: " + 0);
        });

        testStart = Instant.now();

        jobsDriver.start();
    }

    /**
     * Performs a test-case on the test plans system under test(sut).
     * @param testCase to perform.
     */
    private void performTest(final MutationTestCase testCase) {
        new TestDriver(testCase, getPlan(), this::onTestDone).start();
    }

    /**
     * Is triggered when a test-case execution is done.
     * It updates UI labels to tell user about the progress.
     * It also updates the jobsDriver about the job progress.
     * @param result the test result
     */
    private synchronized void onTestDone(final TestResult result) {
        if (result != null) {
            switch (result.getVerdict()) {
                case INCONCLUSIVE:
                    Platform.runLater(() -> {
                        getPlan().getInconclusiveMessageList().add(result);
                        getPlan().setInconclusiveText("Inconclusive: " + getPlan().getInconclusiveMessageList().size());
                    });
                    break;
                case PASS:
                    Platform.runLater(() -> {
                        getPlan().getPassedResults().add(result);
                        getPlan().setPassedText("Passed: " + getPlan().getPassedResults().size());
                    });
                    break;
                case FAIL:
                    Platform.runLater(() -> {
                        getPlan().getFailedMessageList().add(result);
                        getPlan().setFailedText("Failed: " + getPlan().getFailedMessageList().size());
                    });
                    break;
            }

            Platform.runLater(() -> getPlan().setTestTimeText(
                    "Testing time: " + MutationTestPlanPresentation.readableFormat(Duration.between(testStart, Instant.now()))
            ));
        }

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
    public void writeProgress(final int jobsEnded, final int totalJobs) {
        writeProgress("Testing... (" + jobsEnded + "/" + totalJobs + " test-cases)");
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
}
