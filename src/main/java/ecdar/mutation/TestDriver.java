package ecdar.mutation;
import com.google.common.util.concurrent.SimpleTimeLimiter;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * A test driver that runs testcases on a system under test (sut).
 */
public class TestDriver implements ConcurrentJobsHandler {
    private List<String> passed;
    private List<String> failed;
    private List<String> inconclusive;
    private final MutationTestPlan testPlan;
    private final long timeUnit;
    private final int bound;
    private final List<MutationTestCase> mutationTestCases;
    private final Consumer<Text> progressWriterText;
    private Instant generationStart;
    private ConcurrentJobsDriver jobsDriver;

    private enum Verdict {NONE, INCONCLUSIVE, PASS, FAIL}

    TestDriver(final List<MutationTestCase> mutationTestCases, final MutationTestPlan testPlan, final Consumer<Text> progressWriterText, final long timeUnit, final int bound) {
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
            final Process sut;
            final BufferedWriter output;
            final InputStream inputStream;
            final BufferedReader input;
            ObjectProperty<Instant> lastUpdateTime = new SimpleObjectProperty<>();
            lastUpdateTime.setValue(Instant.now());
            NonRefinementStrategy strategy = testCase.getStrategy();
            SimpleComponentSimulation testModelSimulation = new SimpleComponentSimulation(testCase.getTestModel());
            SimpleComponentSimulation mutantSimulation = new SimpleComponentSimulation(testCase.getMutant());

            //Try to start the process
            try {
                sut = Runtime.getRuntime().exec("java -jar " + Ecdar.projectDirectory.get() + File.separator + getPlan().getSutPath().replace("/", File.separator));
                output = new BufferedWriter(new OutputStreamWriter(sut.getOutputStream()));
                inputStream = sut.getInputStream();
                input = new BufferedReader(new InputStreamReader(inputStream));
            } catch (final IOException e) {
                e.printStackTrace();
                return;
            }

            //Begin the new test
            System.out.println("\nNew test: " + testCase.getId());
            Verdict verdict = Verdict.NONE;
            int i = 0;
            while(verdict.equals(Verdict.NONE) && i <= bound) { //TODO While loop der stopper hvis vi har fået en verdict
                // Get rule and check if its empty
                StrategyRule rule = strategy.getRule(testModelSimulation, mutantSimulation);
                if (rule == null) {
                    verdict = Verdict.INCONCLUSIVE;
                } else {
                    try {
                        //Check if rule is an delay rule or output action rule, if it is either, perform delay,
                        // if it is an input action perform input
                        if (rule instanceof DelayRule || (rule instanceof ActionRule && ((ActionRule) rule).getStatus() == EdgeStatus.OUTPUT)) {
                            verdict = delay(rule, testModelSimulation, mutantSimulation, testCase, lastUpdateTime, inputStream, input);
                        } else if (rule instanceof ActionRule) {
                            System.out.println("Input");
                            String sync = ((ActionRule) rule).getSync();
                            testModelSimulation.runInputAction(sync);
                            mutantSimulation.runInputAction(sync);
                            writeToSut(sync, output, sut);
                        }
                    } catch (MutationTestingException e) {
                        e.printStackTrace();
                    }
                }
                i++;
            }

            onTestDone(output, sut, verdict, testCase);
        }).start();
    }

    /**
     * Is triggered when a test-case execution is done.
     * It updates UI labels to tell user about the progress.
     * It also updates the jobsDriver about the job progress.
     */
    private synchronized void onTestDone(BufferedWriter output, Process sut, Verdict verdict, MutationTestCase testCase) {
        //We treat a none verdict the same as inconclusive, as it should only be none if the bound has been surpassed
        switch (verdict) {
            case NONE:
            case INCONCLUSIVE:
                inconclusive.add(testCase.getId());
                Platform.runLater(() -> getPlan().setInconclusiveText("Inconclusive: " + inconclusive.size()));
                break;
            case PASS:
                passed.add(testCase.getId());
                Platform.runLater(() -> getPlan().setPassedText("Passed: " + passed.size()));
                break;
            case FAIL:
                failed.add(testCase.getId());
                Platform.runLater(() -> getPlan().setFailedText("Failed: " + failed.size()));
                break;
        }
        writeToSut("Done", output, sut);

        /*Platform.runLater(() -> getPlan().setTestCasesText("Test-cases: " + mutationTestCases.size() + " - Execution time: " +
                MutationTestPlanPresentation.readableFormat(Duration.between(generationStart, Instant.now()))));*/

        jobsDriver.onJobDone();
    }

    /**
     * performs a delay on the simulations and itself and checks if the system under test made an output during the delay.
     * @param testModelSimulation simulation representing the test model.
     * @param mutantSimulation simulation representing the mutated model.
     * @param testCase that is being performed.
     * @return a verdict, it is NONE if no verdict were reached from this delay.
     */
    private Verdict delay(final StrategyRule rule, final SimpleComponentSimulation testModelSimulation, final SimpleComponentSimulation mutantSimulation, final MutationTestCase testCase, ObjectProperty<Instant> lastUpdateTime, final InputStream inputStream, final BufferedReader input) throws MutationTestingException {
        System.out.println("begin delay");
        try {
            Instant delayDuration = Instant.now();
            Map<String, Double> clockValuations = getClockValuations(testModelSimulation, mutantSimulation);


            while((rule.isSatisfied(clockValuations))) {
                System.out.println("Time: " + Duration.between(delayDuration, Instant.now()).toMillis());
                if (!(Duration.between(delayDuration, Instant.now()).toMillis()/(double)timeUnit <= testPlan.getOutputWaitTime())){
                    return Verdict.INCONCLUSIVE;
                }
                try {
                    String output = SimpleTimeLimiter.create(Executors.newSingleThreadExecutor()).callWithTimeout(input::readLine, timeUnit/4, TimeUnit.MILLISECONDS);
                    if (!simulateDelay(testModelSimulation, mutantSimulation, testCase, lastUpdateTime)) {
                        return Verdict.FAIL;
                    }
                    System.out.println("Output");
                    return simulateOutput(testModelSimulation, mutantSimulation, output);
                } catch (TimeoutException e) {
                    System.out.println("delay");
                    if (!simulateDelay(testModelSimulation, mutantSimulation, testCase, lastUpdateTime)) {
                        return Verdict.FAIL;
                    }
                }
                clockValuations = getClockValuations(testModelSimulation, mutantSimulation);
            }
            //Todo make sure the program gives inconclusive veridct when while loop is exitied through other means.
            return Verdict.NONE;
        } catch(InterruptedException | MutationTestingException |  ExecutionException e){
            throw new MutationTestingException(e.getMessage());
            //Todo stop testing and print error
        }
    }

    private Map<String, Double> getClockValuations(SimpleComponentSimulation testModelSimulation, SimpleComponentSimulation mutantSimulation) {
        Map<String, Double> clockValuations = new HashMap<>();
        clockValuations.putAll(testModelSimulation.getFullyQuantifiedClockValuations());
        clockValuations.putAll(mutantSimulation.getFullyQuantifiedClockValuations());
        return clockValuations;
    }

    private Verdict simulateOutput(SimpleComponentSimulation testModelSimulation, SimpleComponentSimulation mutantSimulation, String output) throws MutationTestingException {
        if (output == null){
            return Verdict.INCONCLUSIVE;
        } else if (!testModelSimulation.runOutputAction(output)){
            return Verdict.FAIL;
        } else if (!mutantSimulation.runOutputAction(output)){
            return Verdict.PASS;
        }
        return Verdict.NONE;
    }

    private boolean simulateDelay(SimpleComponentSimulation testModelSimulation, SimpleComponentSimulation mutantSimulation, MutationTestCase testCase, ObjectProperty<Instant> lastUpdateTime) throws MutationTestingException {
        final double waitedTimeUnits = Duration.between(lastUpdateTime.get(), Instant.now()).toMillis() / (double) timeUnit;
        lastUpdateTime.setValue(Instant.now());
        if (!testModelSimulation.delay(waitedTimeUnits)) {
            return false;
        } else {
            if (!mutantSimulation.delay(waitedTimeUnits)) {
                throw new MutationTestingException("Mutant could not be delayed, however simulation could");
                //Todo
            }
        }
        return true;
    }

    /**
     * Writes to the system.in of the system under test.
     * @param outputBroadcast the string to write to the system under test.
     */
    private void writeToSut(final String outputBroadcast, final BufferedWriter output, final Process sut) {
        System.out.println("Write input " + outputBroadcast);
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
