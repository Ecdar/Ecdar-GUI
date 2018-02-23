package ecdar.mutation;
import ecdar.abstractions.EdgeStatus;
import ecdar.mutation.models.*;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TestDriver implements ConcurrentJobsHandler {

    private String SUTPath;
    private String stringInput;
    private BufferedReader input;
    private InputStream inputStream;
    private BufferedWriter output;
    private Process SUT;
    private List<String> passed;
    private List<String> failed;
    private List<String> inconclusive;
    private MutationTestPlan testPlan;
    private int timeUnit;
    private int bound;
    private List<MutationTestCase> mutationTestCases;
    private Consumer<Text> progressWriterText;
    private Instant generationStart;
    private ConcurrentJobsDriver jobsDriver;

    private enum Verdict {NONE, INCONCLUSIVE, PASS, FAIL}

    public TestDriver(List<MutationTestCase> mutationTestCases, MutationTestPlan testPlan, final Consumer<Text> progressWriterText, String SUTPath, int timeUnit, int bound) {
        this.mutationTestCases = mutationTestCases;
        this.progressWriterText = progressWriterText;
        this.testPlan = testPlan;
        this.SUTPath = SUTPath;
        this.timeUnit = timeUnit;
        this.bound = bound;
    }

    public void start(){
        generationStart = Instant.now();
        inconclusive = new ArrayList<>();
        passed = new ArrayList<>();
        failed = new ArrayList<>();
        jobsDriver = new ConcurrentJobsDriver(this, mutationTestCases.size());
        jobsDriver.start();
    }

    private void performTest(final MutationTestCase testCase){
        new Thread(() -> {
            System.out.println(testCase.getId());
            int step = 0;
            NonRefinementStrategy strategy = testCase.getStrategy();
            ComponentSimulation testModelSimulation = new ComponentSimulation(testCase.getTestModel());
            ComponentSimulation mutantSimulation = new ComponentSimulation(testCase.getMutant());
            initializeAndRunProcess();

            while (true) {
                // Check bounds
                if (step == bound) {
                    System.out.println("out of bounds");
                    inconclusive.add(testCase.getId());
                    onTestDone();
                    return;
                }

                // Get rule and check if its empty
                StrategyRule rule = strategy.getRule(testModelSimulation.getCurrentLocation().getId(), mutantSimulation.getCurrentLocation().getId(), testModelSimulation.getValuations(), mutantSimulation.getValuations());
                if (rule == null) {
                    System.out.println("Empty Rule");
                    inconclusive.add(testCase.getId());
                    onTestDone();
                    return;
                }

                //Check if rule is an action rule, if it is an output action perform delay,
                //If it is an input perform it
                if (rule instanceof ActionRule) {
                    if (((ActionRule) rule).getStatus() == EdgeStatus.OUTPUT) {
                        Verdict verdict = delay(testModelSimulation, mutantSimulation, testCase, getPlan().getOutputWaittime());
                        if(!verdict.equals(Verdict.NONE)) {
                            onTestDone();
                            return;
                        }
                    } else {
                        System.out.println("Input");
                        try {
                            testModelSimulation.runAction(((ActionRule) rule).getSync(), ((ActionRule) rule).getStatus());
                            mutantSimulation.runAction(((ActionRule) rule).getSync(), ((ActionRule) rule).getStatus());
                        } catch (MutationTestingException e) {
                            e.printStackTrace();
                        }
                        String sync = ((ActionRule) rule).getSync();
                        writeToSUT(sync);
                    }
                } else if (rule instanceof DelayRule) {
                    System.out.println("Delay");
                    Verdict verdict = delay(testModelSimulation, mutantSimulation, testCase, 0);
                    if(!verdict.equals(Verdict.NONE)) {
                        onTestDone();
                        return;
                    }
                }
                step++;
            }
        }).start();
    }

    private void initializeAndRunProcess(){
        try {
            SUT = Runtime.getRuntime().exec("java -jar samples\\SimpleMutationProgram\\out\\artifacts\\test\\test.jar");
            output = new BufferedWriter(new OutputStreamWriter(SUT.getOutputStream()));
            inputStream = SUT.getInputStream();
            input = new BufferedReader(new InputStreamReader(inputStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Is triggered when a test-case execution is done.
     * It updates UI labels to tell user about the progress.
     * It also updates the jobsDriver about the job progress
     */
    private synchronized void onTestDone() {
        Platform.runLater(() -> getPlan().setTestCasesText("Test-cases: " + mutationTestCases.size() + " - Execution time: " +
                MutationTestPlanPresentation.readableFormat(Duration.between(generationStart, Instant.now()))));
        Platform.runLater(() -> getPlan().setInconclusiveText("Inconclusive: " + inconclusive.size()));
        Platform.runLater(() -> getPlan().setPassedText("Passed: " + passed.size()));
        Platform.runLater(() -> getPlan().setFailedText("Failed: " + failed.size()));

        jobsDriver.onJobDone();
    }

    private Verdict delay(ComponentSimulation testModelSimulation, ComponentSimulation mutantSimulation, MutationTestCase testCase, int timeToDelay){
        Instant delayStart = Instant.now();
        try {
            boolean isOutputting = false;
            int waitedTimeUnits = 0;

            while (isOutputting == false && waitedTimeUnits < getPlan().getOutputWaittime()) {
                //Check if any output is ready, if there is none, do delay
                if (inputStream.available() == 0) {
                    Thread.sleep(timeUnit);
                    waitedTimeUnits++;
                    //Do Delay
                    long seconds = Duration.between(delayStart, Instant.now()).getSeconds();
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
                    String outputFromSUT = readFromSUT();
                    if (!testModelSimulation.runAction(outputFromSUT, EdgeStatus.OUTPUT)) {
                        failed.add(testCase.getId());
                        return Verdict.FAIL;
                    } else if (!mutantSimulation.runAction(outputFromSUT, EdgeStatus.OUTPUT)) {
                        passed.add(testCase.getId());
                        return Verdict.PASS;
                    }
                    isOutputting = true;
                }
            }

            return Verdict.NONE;
            } catch(InterruptedException e){
                e.printStackTrace();
                inconclusive.add(testCase.getId());
                return Verdict.INCONCLUSIVE;
            } catch(IOException e){
                e.printStackTrace();
                inconclusive.add(testCase.getId());
                return Verdict.INCONCLUSIVE;
            } catch(MutationTestingException e){
                e.printStackTrace();
                inconclusive.add(testCase.getId());
                return Verdict.INCONCLUSIVE;
            }
    }

    private void writeToSUT(String outputBroadcast){
        try {
            //Write to process if it is alive, else act like the process accepts but ignore all inputs.
            if(SUT.isAlive()) {
                output.write(outputBroadcast + "\n");
                output.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readFromSUT(){
        try {
            return stringInput = input.readLine();
        } catch (IOException e) {
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
    public void writeProgress(int jobsEnded) {
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
    public void startJob(int index) {
        performTest(mutationTestCases.get(index));
    }

    private Consumer<Text> getProgressWriter(){ return progressWriterText; }

    private MutationTestPlan getPlan() {return testPlan; }

    public String getSUTPath() {
        return SUTPath;
    }

    public void setSUTPath(String SUTPath) {
        this.SUTPath = SUTPath;
    }

    public String getStringInput() { return stringInput; }
}
