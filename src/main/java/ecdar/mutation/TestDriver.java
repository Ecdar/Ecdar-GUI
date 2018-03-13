package ecdar.mutation;
import ecdar.Ecdar;
import ecdar.abstractions.EdgeStatus;
import ecdar.mutation.models.*;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import javax.sound.midi.SysexMessage;
import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * Constructor for the test driver, needs a list of mutation test cases, a test plan, a consumer to write progress to, an long representing a time units length in miliseconds and a bound
     */
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

        Platform.runLater(() -> {
            getPlan().setInconclusiveText("Inconclusive: " + 0);
            getPlan().setPassedText("Passed: " + 0);
            getPlan().setFailedText("Failed: " + 0);
        });

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
            StringProperty message = new SimpleStringProperty("");
            ObjectProperty<Instant> lastUpdateTime = new SimpleObjectProperty<>();
            NonRefinementStrategy strategy = testCase.getStrategy();
            SimpleComponentSimulation testModelSimulation = new SimpleComponentSimulation(testCase.getTestModel());
            SimpleComponentSimulation mutantSimulation = new SimpleComponentSimulation(testCase.getMutant());

            try {
                //Start process
                sut = Runtime.getRuntime().exec("java -jar " + Ecdar.projectDirectory.get() + File.separator + getPlan().getSutPath().replace("/", File.separator));
                lastUpdateTime.setValue(Instant.now());
                output = new BufferedWriter(new OutputStreamWriter(sut.getOutputStream()));
                inputStream = sut.getInputStream();
                input = new BufferedReader(new InputStreamReader(inputStream));

                //Begin the new test
                Verdict verdict = Verdict.NONE;
                int i = 0;
                while(verdict.equals(Verdict.NONE) && i <= bound) {
                    // Get rule and check if its empty
                    StrategyRule rule = strategy.getRule(testModelSimulation, mutantSimulation);
                    if (rule == null) {
                        message.setValue("No rule to perform\n");
                        verdict = Verdict.INCONCLUSIVE;
                    } else {
                            //Check if rule is an delay rule or output action rule, if it is either, perform delay,
                            // if it is an input action perform input
                            if (rule instanceof DelayRule || (rule instanceof ActionRule && ((ActionRule) rule).getStatus() == EdgeStatus.OUTPUT)) {
                                verdict = delay(rule, testModelSimulation, mutantSimulation, lastUpdateTime, inputStream, input, message);
                            } else {
                                String sync = ((ActionRule) rule).getSync();
                                testModelSimulation.runInputAction(sync);
                                mutantSimulation.runInputAction(sync);
                                writeToSut(sync, output, sut);
                            }
                    }
                    i++;
                }

                //Finish test, we now know that either the verdict has been set or the time ran out
                onTestDone(output, sut, verdict, testCase, message, testModelSimulation, mutantSimulation);
            } catch (MutationTestingException | IOException e) {
                if (getPlan().getStatus().equals(MutationTestPlan.Status.WORKING)) {
                    getPlan().setStatus(MutationTestPlan.Status.ERROR);
                    Platform.runLater(() -> {
                        final String errorMessage = "Error while running test-case " + testCase.getId() + ", " +
                                testCase.getDescription() + ": " + e.getMessage();
                        final Text text = new Text(errorMessage);
                        text.setFill(Color.RED);
                        writeProgress(text);
                        Ecdar.showToast(errorMessage);
                    });
                }
            }
        }).start();
    }

    /**
     * Is triggered when a test-case execution is done.
     * It updates UI labels to tell user about the progress.
     * It also updates the jobsDriver about the job progress.
     * @param output the buffered writer to write the output to
     * @param sut the process to write to
     * @param verdict the verdict that was given at the end of the test
     * @param testCase the testcase that was running on the system under test
     * @param message the message given by the result of this test
     * @param testModelSimulation the test model simulation
     * @param mutantSimulation the mutant model simulation
     * @throws IOException
     */
    private synchronized void onTestDone(BufferedWriter output, Process sut, Verdict verdict, MutationTestCase testCase, StringProperty message, SimpleComponentSimulation testModelSimulation, SimpleComponentSimulation mutantSimulation) throws IOException {
        //We treat a none verdict the same as inconclusive, as it should only be none if the bound has been surpassed
        switch (verdict) {
            case NONE:
            case INCONCLUSIVE:
                inconclusive.add(testCase.getId());
                Platform.runLater(() -> {
                    getPlan().setInconclusiveText("Inconclusive: " + inconclusive.size());
                    getPlan().getInconclusiveMessageList().add(testCase.getId() + " " + testCase.getDescription() + ":\n" + "Reached inconclusive with message: " + message.get() + "Test model is in location: " + testModelSimulation.getCurrentLocation().getId() + " with values: " + testModelSimulation.getAllValuations() + "\nMutant is in location: " + mutantSimulation.getCurrentLocation().getId() + " with values: " + mutantSimulation.getAllValuations());
                });
                break;
            case PASS:
                passed.add(testCase.getId());
                Platform.runLater(() -> getPlan().setPassedText("Passed: " + passed.size()));
                break;
            case FAIL:
                failed.add(testCase.getId());
                Platform.runLater(() -> {
                    getPlan().setFailedText("Failed: " + failed.size());
                    getPlan().getFailedMessageList().add(testCase.getId() + " " + testCase.getDescription() + ":\n" + "Failed with message: " + message.get() + "Test model is in location: " + testModelSimulation.getCurrentLocation().getId() + " with values: " + testModelSimulation.getAllValuations() + "\nMutant is in location: " + mutantSimulation.getCurrentLocation().getId() + " with values: " + mutantSimulation.getAllValuations());
                });
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
     * @return a verdict, it is NONE if no verdict were reached from this delay.
     */
    private Verdict delay(final StrategyRule rule, final SimpleComponentSimulation testModelSimulation, final SimpleComponentSimulation mutantSimulation, ObjectProperty<Instant> lastUpdateTime, final InputStream inputStream, final BufferedReader input, final StringProperty message) throws MutationTestingException {
        try {
            Instant delayDuration = Instant.now();
            Map<String, Double> clockValuations = getClockValuations(testModelSimulation, mutantSimulation);

            //Keep delaying until the rule has been satisfied,
            //Will return inside while loop if maximum wait time is exceeded or an output has been given
            while((rule.isSatisfied(clockValuations))) {

                //Check if the maximum wait time has been exceeded, if it is, give inconclusive verdict
                if (!(Duration.between(delayDuration, Instant.now()).toMillis()/(double)timeUnit <= testPlan.getOutputWaitTime())){
                    message.setValue("Maximum wait time reached without recieving an output.\n");
                    return Verdict.INCONCLUSIVE;
                }
                if(inputStream.available() != 0){
                    String output = input.readLine();

                    if (output == null) {
                        message.setValue("Program terminated before we reached a proper verdict.\n");
                        return Verdict.INCONCLUSIVE;
                    }
                    //Catch SUT debug commands
                    Matcher match = Pattern.compile("Debug: (.*)").matcher(output);
                    if(match.find()) {
                        System.out.println(match.group(1));
                    } else {
                        return simulateOutput(testModelSimulation, mutantSimulation, output, message);
                    }

                } else {
                    Thread.sleep(timeUnit / 4);
                    if (!simulateDelay(testModelSimulation, mutantSimulation, lastUpdateTime)) {
                        message.setValue("Failed simulating delay on test model\n");
                        return Verdict.FAIL;
                    }
                }
                clockValuations = getClockValuations(testModelSimulation, mutantSimulation);
            }
            return Verdict.NONE;
        } catch(IOException e){
            throw new MutationTestingException("An error occured when reading from the stream");
        } catch (InterruptedException e) {
            throw new MutationTestingException("Sleep was interrupted unexpectedly");
        }
    }

    /**
     * Gets the clock valuations from the simulated test model and simulated mutant model
     * @param testModelSimulation test model.
     * @param mutantSimulation mutant model.
     * @return a map of clock valuations, their id and value.
     */
    private Map<String, Double> getClockValuations(SimpleComponentSimulation testModelSimulation, SimpleComponentSimulation mutantSimulation) {
        Map<String, Double> clockValuations = new HashMap<>();
        clockValuations.putAll(testModelSimulation.getFullyQuantifiedClockValuations());
        clockValuations.putAll(mutantSimulation.getFullyQuantifiedClockValuations());
        return clockValuations;
    }

    /**
     * Simulates an output on the test model and mutant model.
     * @param testModelSimulation the test model
     * @param mutantSimulation the mutant model
     * @param output the output to simulate
     * @return INCONCLUSIVE if the output is empty, FAIL if the output failed on the test model, PASS if it failed on the mutant,
     * NONE if both simulations succeeded
     * @throws MutationTestingException if an exception occured
     */
    private Verdict simulateOutput(SimpleComponentSimulation testModelSimulation, SimpleComponentSimulation mutantSimulation, String output, StringProperty message) throws MutationTestingException {
        if (!testModelSimulation.runOutputAction(output)){
            message.setValue("Failed simulating output " + output + " on test model.\n");
            return Verdict.FAIL;
        } else if (!mutantSimulation.runOutputAction(output)){
            return Verdict.PASS;
        }
        return Verdict.NONE;
    }

    /**
     * Simulates an output on the test model and mutant model.
     * @param testModelSimulation the test model.
     * @param mutantSimulation the mutant model.
     * @param lastUpdateTime the time we determine the delay length from.
     * @return true if the delay was simulated successfully, false if it failed on the test model.
     * @throws MutationTestingException if the simulation failed on the mutant model.
     */
    private boolean simulateDelay(SimpleComponentSimulation testModelSimulation, SimpleComponentSimulation mutantSimulation, ObjectProperty<Instant> lastUpdateTime) throws MutationTestingException {
        final double waitedTimeUnits = Duration.between(lastUpdateTime.get(), Instant.now()).toMillis() / (double) timeUnit;
        lastUpdateTime.setValue(Instant.now());
        if (!testModelSimulation.delay(waitedTimeUnits)) {
            return false;
        } else {
            if (!mutantSimulation.delay(waitedTimeUnits)) {
                throw new MutationTestingException("Mutant could not be delayed, while the simulation could, this should not happen");
            }
        }
        return true;
    }

    /**
     * Writes to the system.in of the system under test.
     * @param outputBroadcast the string to write to the system under test.
     */
    private void writeToSut(final String outputBroadcast, final BufferedWriter output, final Process sut) throws IOException {
        //Write to process if it is alive, else act like the process accepts but ignore all inputs.
        if(sut.isAlive()) {
            output.write(outputBroadcast + "\n");
            output.flush();
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
