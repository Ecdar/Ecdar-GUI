package ecdar.mutation;
import ecdar.Ecdar;
import ecdar.abstractions.EdgeStatus;
import ecdar.mutation.models.*;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A test driver that runs testcases on a system under test (sut).
 */
public class TestDriver implements ConcurrentJobsHandler {

    private BufferedReader input;
    private InputStream inputStream;
    private BufferedWriter output;
    private Process sut;
    private List<String> passed;
    private List<String> failed;
    private List<String> inconclusive;
    private final MutationTestPlan testPlan;
    private final int timeUnit;
    private final int bound;
    private final List<MutationTestCase> mutationTestCases;
    private final Consumer<Text> progressWriterText;
    private Instant generationStart;
    private ConcurrentJobsDriver jobsDriver;

    private enum Verdict {NONE, INCONCLUSIVE, PASS, FAIL}

    TestDriver(final List<MutationTestCase> mutationTestCases, final MutationTestPlan testPlan, final Consumer<Text> progressWriterText, final int timeUnit, final int bound) {
        this.mutationTestCases = mutationTestCases;
        this.progressWriterText = progressWriterText;
        this.testPlan = testPlan;
        this.timeUnit = timeUnit;
        this.bound = bound;
    }

    /**
     * Starts the test driver.
     */
    public void start() {
        generationStart = Instant.now();
        inconclusive = new ArrayList<>();
        passed = new ArrayList<>();
        failed = new ArrayList<>();
        jobsDriver = new ConcurrentJobsDriver(this, mutationTestCases.size());
        jobsDriver.start();
    }

    /**
     * Performs a testcase on the testplans system under test(sut).
     * @param testCase to perform.
     */
    private void performTest(final MutationTestCase testCase) {
        new Thread(() -> {
            NonRefinementStrategy strategy = testCase.getStrategy();
            ComponentSimulation testModelSimulation = new ComponentSimulation(testCase.getTestModel());
            ComponentSimulation mutantSimulation = new ComponentSimulation(testCase.getMutant());
            initializeAndRunProcess();

            for(int step = 0; step < bound; step++) {
                // Get rule and check if its empty
                StrategyRule rule = strategy.getRule(testModelSimulation.getCurrentLocation().getId(), mutantSimulation.getCurrentLocation().getId(), testModelSimulation.getValuations(), mutantSimulation.getValuations());
                if (rule == null) {
                    inconclusive.add(testCase.getId());
                    onTestDone();
                    return;
                }

                //Check if rule is an action rule, if it is an output action perform delay,
                //If it is an input perform it
                if (rule instanceof ActionRule) {
                    if (((ActionRule) rule).getStatus() == EdgeStatus.OUTPUT) {
                        Verdict verdict = delay(testModelSimulation, mutantSimulation, testCase);
                        if(!verdict.equals(Verdict.NONE)) {
                            onTestDone();
                            return;
                        }
                    } else {
                        try {
                            testModelSimulation.runAction(((ActionRule) rule).getSync(), EdgeStatus.INPUT);
                            mutantSimulation.runAction(((ActionRule) rule).getSync(), EdgeStatus.INPUT);
                        } catch (MutationTestingException e) {
                            e.printStackTrace();
                        }
                        String sync = ((ActionRule) rule).getSync();
                        writeToSut(sync);
                    }
                } else if (rule instanceof DelayRule) {
                    Verdict verdict = delay(testModelSimulation, mutantSimulation, testCase);
                    if(!verdict.equals(Verdict.NONE)) {
                        onTestDone();
                        return;
                    }
                }
            }
            inconclusive.add(testCase.getId());
            onTestDone();
        }).start();
    }

    /**
     * Runs the system under test and initialises the streams.
     */
    private void initializeAndRunProcess() {
        try {
            sut = Runtime.getRuntime().exec("java -jar " + Ecdar.projectDirectory.get() + File.separator + getPlan().getSutPath().replace("/", File.separator));
            output = new BufferedWriter(new OutputStreamWriter(sut.getOutputStream()));
            inputStream = sut.getInputStream();
            input = new BufferedReader(new InputStreamReader(inputStream));
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Is triggered when a test-case execution is done.
     * It updates UI labels to tell user about the progress.
     * It also updates the jobsDriver about the job progress.
     */
    private synchronized void onTestDone() {
        Platform.runLater(() -> getPlan().setTestCasesText("Test-cases: " + mutationTestCases.size() + " - Execution time: " +
                MutationTestPlanPresentation.readableFormat(Duration.between(generationStart, Instant.now()))));
        Platform.runLater(() -> getPlan().setInconclusiveText("Inconclusive: " + inconclusive.size()));
        Platform.runLater(() -> getPlan().setPassedText("Passed: " + passed.size()));
        Platform.runLater(() -> getPlan().setFailedText("Failed: " + failed.size()));

        jobsDriver.onJobDone();
    }

    /**
     * performs a delay on the simulations and itself and checks if the system under test made an output during the delay.
     * @param testModelSimulation simulation representing the test model.
     * @param mutantSimulation simulation representing the mutated model.
     * @param testCase that is being performed.
     * @return a verdict, it is NONE if no verdict were reached from this delay.
     */
    private Verdict delay(final ComponentSimulation testModelSimulation, final ComponentSimulation mutantSimulation, final MutationTestCase testCase) {
        final Instant delayStart = Instant.now();
        try {
            //Check if any output is ready, if there is none, do delay
            if (inputStream.available() == 0) {
                Thread.sleep(timeUnit);
                //Do Delay
                final long seconds = Duration.between(delayStart, Instant.now()).getSeconds();
                if (!testModelSimulation.delay(seconds)) {
                    failed.add(testCase.getId());
                    return Verdict.FAIL;
                } else {
                    if (!mutantSimulation.delay(Duration.between(delayStart, Instant.now()).getSeconds())) {
                        //Todo Handle exception
                    }
                }
            }

            //Do output if any output happened when sleeping
            if (inputStream.available() != 0) {
                final String outputFromSut = readFromSut();
                if (!testModelSimulation.runOutAction(outputFromSut)) {
                    failed.add(testCase.getId());
                    return Verdict.FAIL;
                } else if (!mutantSimulation.runOutAction(outputFromSut)) {
                    passed.add(testCase.getId());
                    return Verdict.PASS;
                }
            }

            return Verdict.NONE;
            } catch(InterruptedException | MutationTestingException | IOException e) {
                e.printStackTrace();
                inconclusive.add(testCase.getId());
                //Todo stop testing and print error
                return Verdict.INCONCLUSIVE;
            }
    }

    /**
     * Writes to the system.in of the system under test.
     * @param outputBroadcast the string to write to the system under test.
     */
    private void writeToSut(final String outputBroadcast) {
        try {
            //Write to process if it is alive, else act like the process accepts but ignore all inputs.
            if(sut.isAlive()) {
                output.write(outputBroadcast + "\n");
                output.flush();
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads from the system under tests output stream.
     * @return the string read from the system under test.
     */
    private String readFromSut() {
        try {
            return input.readLine();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return null;
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
        writeProgress("Testcase: " + jobsEnded + "/" + mutationTestCases.size());
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

    private Consumer<Text> getProgressWriter() { return progressWriterText; }

    private MutationTestPlan getPlan() {return testPlan; }
}
