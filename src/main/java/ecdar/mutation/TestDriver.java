package ecdar.mutation;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.EdgeStatus;
import ecdar.abstractions.Location;
import ecdar.backend.UPPAALDriver;
import ecdar.mutation.models.*;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.function.Consumer;

public class TestDriver {

    private String SUTPath;
    private String stringInput;
    private BufferedReader input;
    private InputStream inputStream;
    private BufferedWriter output;
    private Process SUT;
    private List<Verdict> results;
    private int passed;
    private int failed;
    private int inconclusive;
    private MutationTestPlan testPlan;
    private int timeUnit;
    private int bound;
    private int generationJobsStarted;
    private int generationJobsEnded;
    private List<MutationTestCase> mutationTestCases;
    private Consumer<Text> progressWriterText;
    private Consumer<String> progressWriterString;
    private Instant generationStart;

    private enum Verdict {NONE, INCONCLUSIVE, PASS, FAIL}

    public TestDriver(List<MutationTestCase> mutationTestCases, MutationTestPlan testPlan, final Consumer<Text> progressWriterText, final Consumer<String> progressWriterString, String SUTPath, int timeUnit, int bound) {
        this.mutationTestCases = mutationTestCases;
        this.progressWriterText = progressWriterText;
        this.progressWriterString = progressWriterString;
        this.testPlan = testPlan;
        this.SUTPath = SUTPath;
        this.timeUnit = timeUnit;
        this.bound = bound;
    }

    public void start(){
        generationStart = Instant.now();
        updateJobs();
    }

    private void performTest(MutationTestCase testCase){

        Verdict verdict = Verdict.NONE;
        new Thread(() -> {
            int step = 0;
            NonRefinementStrategy strategy = testCase.getStrategy();
            ComponentSimulation testModelSimulation = new ComponentSimulation(testCase.getTestModel());
            ComponentSimulation mutantSimulation = new ComponentSimulation(testCase.getMutant());
            initializeAndRunProcess();
            int step = 0;

            while (true) {

                if (step == bound) {
                    inconclusive++;
                    testPlan.setPassedText("Inconclusive: " + inconclusive);
                    results.add(Verdict.INCONCLUSIVE);
                    Platform.runLater(this::onJobDone);
                    return;
                }

                StrategyRule rule = strategy.getRule(testModelSimulation.getCurrentLocation().getId(), mutantSimulation.getCurrentLocation().getId(), testModelSimulation.getValuations(), mutantSimulation.getValuations());
                if (rule == null) {
                    inconclusive++;
                    testPlan.setPassedText("Inconclusive: " + inconclusive);
                    results.add(Verdict.INCONCLUSIVE);
                    Platform.runLater(this::onJobDone);
                    return;
                }

                if (rule instanceof ActionRule) {
                    if (((ActionRule) rule).getStatus() == EdgeStatus.OUTPUT) {
                        results.add(delay(testModelSimulation, mutantSimulation, timeUnit));
                        Platform.runLater(this::onJobDone);
                        return;
                    } else {
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
                    results.add(delay(testModelSimulation, mutantSimulation, timeUnit));
                    Platform.runLater(this::onJobDone);
                    return;
                }
                step++;
            }
        }).start();
    }

    private void initializeAndRunProcess(){
        try {
            SUT = Runtime.getRuntime().exec("java -jar C:\\Users\\Chres\\Desktop\\SimpleMutationProgram\\out\\artifacts\\test\\test.jar");
            output = new BufferedWriter(new OutputStreamWriter(SUT.getOutputStream()));
            inputStream = SUT.getInputStream();
            input = new BufferedReader(new InputStreamReader(inputStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized void updateJobs(){
        if (testPlan.getStatus().equals(MutationTestPlan.Status.STOPPING)) {
            if (getGenerationJobsRunning() == 0) {
                testPlan.setStatus(MutationTestPlan.Status.IDLE);
            }
            return;
        }

        // If we are done, clean up and move on
        if (generationJobsEnded == mutationTestCases.size()) {
            final Text text = new Text("Done");
            text.setFill(Color.GREEN);
            progressWriterText.accept(text);
            testPlan.setStatus(MutationTestPlan.Status.IDLE);
            return;
        }

        progressWriterString.accept("Testcase: " + generationJobsEnded + "/" + mutationTestCases.size());
        // while we have not reach the maximum allowed threads and there are still jobs to start
        while (getGenerationJobsRunning() < testPlan.getConcurrentSutInstances() &&
                generationJobsStarted < mutationTestCases.size()) {
            performTest(mutationTestCases.get(generationJobsStarted));
            generationJobsStarted++;
        }
    }

    /**
     * Gets the number of generation jobs currently running.
     * @return the number of jobs running
     */
    private synchronized int getGenerationJobsRunning() {
        return generationJobsStarted - generationJobsEnded;
    }

    /**
     * Is triggered when a test-case execution is done.
     * It updates UI labels to tell user about the progress.
     *
     * This method should be called in a JavaFX thread, since it updates JavaFX elements.
     */
    private synchronized void onJobDone() {
        generationJobsEnded++;

        testPlan.setTestCasesText("Test-cases: " + mutationTestCases.size() + " - Execution time: " +
                MutationTestPlanPresentation.readableFormat(Duration.between(generationStart, Instant.now())));

        updateJobs();
    }

    private Verdict delay(ComponentSimulation testModelSimulation, ComponentSimulation mutantSimulation, int timeUnit){
        Instant delayStart = Instant.now();
        try {
            //Check if any output is ready, if there is none, do delay
            if(inputStream.available() == 0){
                Thread.sleep(timeUnit);
                //Do Delay
                long seconds = Duration.between(delayStart, Instant.now()).getSeconds();
                if(!testModelSimulation.delay(seconds)){
                    failed++;
                    testPlan.setPassedText("Failed: " + failed);
                    return Verdict.FAIL;
                } else {
                    mutantSimulation.delay(Duration.between(delayStart, Instant.now()).getSeconds());
                }
            }

            //Do output if any output happened when sleeping
            if(inputStream.available() != 0){
                String outputFromSUT = readFromSUT();
                if(!testModelSimulation.runAction(outputFromSUT, EdgeStatus.OUTPUT)){
                    System.out.println("Fail Output " + outputFromSUT);
                    failed++;
                    testPlan.setPassedText("Failed: " + failed);
                    return Verdict.FAIL;
                } else if (!mutantSimulation.runAction(outputFromSUT, EdgeStatus.OUTPUT)) {
                    passed++;
                    testPlan.setPassedText("Passed: " + failed);
                    return Verdict.PASS;
                }
            }
            return Verdict.NONE;
        } catch (InterruptedException e) {
            e.printStackTrace();
            inconclusive++;
            testPlan.setPassedText("Inconclusive: " + inconclusive);
            return Verdict.INCONCLUSIVE;
        } catch (IOException e) {
            e.printStackTrace();
            inconclusive++;
            testPlan.setPassedText("Inconclusive: " + inconclusive);
            return Verdict.INCONCLUSIVE;
        } catch (MutationTestingException e) {
            e.printStackTrace();
            inconclusive++;
            testPlan.setPassedText("Inconclusive: " + inconclusive);
            return Verdict.INCONCLUSIVE;
        }
    }

    private void writeToSUT(String outputBroadcast){
        try {
            //Write if process is alive, else act like the process accepts but ignore all inputs.
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

    public String getSUTPath() {
        return SUTPath;
    }

    public void setSUTPath(String SUTPath) {
        this.SUTPath = SUTPath;
    }

    public String getStringInput() { return stringInput; }
}
