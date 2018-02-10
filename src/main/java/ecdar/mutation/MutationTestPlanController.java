package ecdar.mutation;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXTextField;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Project;
import ecdar.backend.BackendException;
import ecdar.backend.UPPAALDriver;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for a test plan with model-based mutation testing.
 */
public class MutationTestPlanController {
    private final static String TEST_MODEL_NAME = "S";
    private final static String MUTANT_NAME = "M";

    // UI elements
    public JFXComboBox<Label> modelPicker;
    public JFXButton testButton;
    public JFXButton stopButton;
    public Label mutantsText;
    public Label testCasesText;
    public JFXTextField generationThreadsField;
    public VBox operatorsArea;
    public JFXComboBox<Label> actionPicker;
    public VBox testDependentArea;
    public JFXButton selectSutButton;
    public VBox exportDependantArea;
    public VBox sutDependentArea;
    public Label sutPathLabel;
    public JFXComboBox<Label> formatPicker;
    public VBox modelDependentArea;
    public VBox resultsArea;
    public VBox progressAres;
    public JFXTextField suvInstancesField;
    public JFXCheckBox demonicCheckBox;
    public JFXCheckBox angelicBox;
    public JFXButton storeMutantsButton;
    public TextFlow progressTextFlow;
    public HBox selectSutArea;
    public VBox demonicArea;

    // Mutation objects
    private Map<JFXCheckBox, MutationOperator> operatorMap;
    private MutationTestPlan plan;
    private Component testModel;
    private List<Component> mutants;
    private List<MutationTestCase> testCases;

    // Progress fields
    private Instant generationStart;
    private String queryFilePath;
    private int generationJobsStarted;
    private int generationJobsEnded;

    public MutationTestPlanController() {
        initializeOperators();
    }

    public Map<JFXCheckBox, MutationOperator> getOperatorMap() {
        return operatorMap;
    }

    private void initializeOperators() {
        operatorMap = new LinkedHashMap<>(); // This preserves the order
        final List<MutationOperator> operators = new ArrayList<>();
        operators.add(new ChangeSourceOperator());
        operators.add(new ChangeTargetOperator());
        for (final MutationOperator operator : operators) {
            final JFXCheckBox checkBox = new JFXCheckBox(operator.getText());
            checkBox.setSelected(true);
            operatorMap.put(checkBox, operator);
        }
    }

    public MutationTestPlan getPlan() {
        return plan;
    }

    public void setPlan(final MutationTestPlan plan) {
        this.plan = plan;
    }

    /**
     * Triggered when pressed the test button.
     * Conducts the test.
     */
    public void onTestButtonPressed() {
        getPlan().setStatus(MutationTestPlan.Status.WORKING);

        // Find test model from test model picker
        // Clone it, because we want to change its name
        testModel = Ecdar.getProject().findComponent(modelPicker.getValue().getText()).cloneForVerification();
        testModel.setName(TEST_MODEL_NAME);
        testModel.updateIOList();

        // Mutate and make input-enabled with angelic completion
        final Instant start = Instant.now();
        mutants = new ChangeSourceOperator().computeMutants(testModel);
        mutants.addAll(new ChangeTargetOperator().computeMutants(testModel));
        mutants.forEach(Component::applyAngelicCompletion);
        plan.setMutantsText("Mutants: " + mutants.size() + " - Execution time: " + readableFormat(Duration.between(start, Instant.now())));

        // If chosen, apply demonic completion
        if (getPlan().isDemonic()) testModel.applyDemonicCompletion();

        // Create test cases
        generationJobsStarted = 0;
        generationJobsEnded = 0;
        generationStart = Instant.now();
        testCases = Collections.synchronizedList(new ArrayList<>()); // use synchronized to be thread safe

        try {
            queryFilePath = UPPAALDriver.storeQuery("refinement: " + MUTANT_NAME + "<=" + TEST_MODEL_NAME, "query");
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
            Ecdar.showToast("Error: " + e.getMessage());
            return;
        }

        updateGenerationJobs();
    }

    /**
     * Triggered when pressed the stop button.
     * Signals that this test plan should stop doing jobs.
     */
    public void onStopButtonPressed() {
        getPlan().setStatus(MutationTestPlan.Status.STOPPING);
    }

    /**
     * Converts a duration to a human readable format, e.g. 0.293s.
     * @param duration the duration
     * @return a string in a human readable format
     */
    private static String readableFormat(final Duration duration) {
        return duration.toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase();
    }

    /**
     * Updates what test-case generation jobs to run.
     */
    private synchronized void updateGenerationJobs() {
        if (getPlan().getStatus().equals(MutationTestPlan.Status.STOPPING)) {
            if (getGenerationJobsRunning() == 0) {
                getPlan().setStatus(MutationTestPlan.Status.IDLE);
            }

            return;
        }

        // If we are done, clean up and move on
        if (generationJobsEnded == mutants.size()) {
            try {
                FileUtils.cleanDirectory(new File(UPPAALDriver.getTempDirectoryAbsolutePath()));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
                Ecdar.showToast("Error: " + e.getMessage());
                return;
            }

            final Text text = new Text("Done");
            text.setFill(Color.GREEN);
            writeProgress(text);
            getPlan().setStatus(MutationTestPlan.Status.IDLE);

            return;
        }

        writeProgress("Generating test-cases... (" + generationJobsEnded + "/" + mutants.size() + " mutants processed)");

        // while we have not reach the maximum allowed threads and there are still jobs to start
        while (getGenerationJobsRunning() < getMaxConcurrentGenerationJobs() &&
                generationJobsStarted < mutants.size()) {
            generateTestCase(testModel, mutants.get(generationJobsStarted), generationJobsStarted);
            generationJobsStarted++;
        }
    }

    public void writeProgress(final String message) {
        final Text text = new Text(message);
        text.setFill(Color.web("#333333"));
        writeProgress(text);
    }

    private void writeProgress(final Text text) {
        progressTextFlow.getChildren().clear();
        progressTextFlow.getChildren().add(text);
    }

    /**
     * Gets the number of generation jobs currently running.
     * @return the number of jobs running
     */
    private synchronized int getGenerationJobsRunning() {
        return generationJobsStarted - generationJobsEnded;
    }

    /**
     * Gets the maximum allowed concurrent threads for generating test-cases.
     * @return the maximum allowed threads
     */
    private int getMaxConcurrentGenerationJobs() {
        if (generationThreadsField.getText().isEmpty())
            return 1;
        else
            return Integer.parseInt(generationThreadsField.getText());
    }

    /**
     * Generates a test-case.
     * @param testModel test model
     * @param mutant mutant used for generating
     * @param mutationIndex index of the mutant used for generating
     */
    private void generateTestCase(final Component testModel, final Component mutant, final int mutationIndex) {
        // make a project with the test model and the mutant
        final Project project = new Project();
        mutant.setName(MUTANT_NAME);
        project.getComponents().addAll(testModel, mutant);
        project.setGlobalDeclarations(Ecdar.getProject().getGlobalDeclarations());
        mutant.updateIOList(); // Update io in order to get the right system declarations for the mutant
        project.setSystemDeclarations(new TwoComponentSystemDeclarations(testModel, mutant));

        new Thread(() -> {
            final Process process = startVerifytgaProcess(mutationIndex, project);
            if (process == null) return;

            List<String> lines = getVerifytgaInputLines(process);
            if (lines == null) return;

            // If refinement, no test-case to generate.
            // I use endsWith rather than contains,
            // since verifytga sometimes output some weird symbols at the start of this line.
            if (lines.stream().anyMatch(line -> line.endsWith(" -- Property is satisfied."))) {
                Platform.runLater(this::onGenerationJobDone);
                return;
            }

            // Verifytga should output that the property is not satisfied
            // If it does not, then this is an error
            if (lines.stream().noneMatch(line -> line.endsWith(" -- Property is NOT satisfied."))) {
                throw new RuntimeException("Output from verifytga not understood: " + String.join("\n", lines) + "\n" +
                        "Mutation index: " + mutationIndex);
            }

            int strategyIndex = lines.indexOf("Strategy for the attacker:");

            // If no such index, error
            if (strategyIndex < 0) {
                throw new RuntimeException("Output from verifytga not understood: " + String.join("\n", lines) + "\n" +
                        "Mutation index: " + mutationIndex);
            }

            List<String> strategy = lines.subList(strategyIndex + 2, lines.size());

            testCases.add(new MutationTestCase(testModel, mutant, strategy));

            // JavaFX elements cannot be updated in another thread, so make it run in a JavaFX thread at some point
            Platform.runLater(this::onGenerationJobDone);
        }).start();
    }

    /**
     * Starts varifytga to fetch a strategy.
     * @param mutationIndex the inputs of the mutant to pass to verifytga
     * @param project the backend XML project containing the test model and the mutant
     * @return the started process, or null if an error occurs
     */
    private Process startVerifytgaProcess(final int mutationIndex, final Project project) {
        final Process process;

        try {
            // Store the project and the refinement query as backend XML
            final String modelPath = UPPAALDriver.storeBackendModel(project, "model" + mutationIndex);

            // Run verifytga to check refinement and to fetch strategy if non-refinement
            final ProcessBuilder builder = new ProcessBuilder(UPPAALDriver.findVerifytgaAbsolutePath(), "-t0", modelPath, queryFilePath);
            process = builder.start();

        } catch (BackendException | IOException | URISyntaxException e) {
            e.printStackTrace();
            Ecdar.showToast("Error: " + e.getMessage());
            return null;
        }

        return process;
    }

    /**
     * Gets the lines from the input stream of verifytga.
     * If an error occurs, this method tells the user and signals this controller to stop.
     * @param process the process running verifytga
     * @return the input lines, or null if an error occurs. Each line is without the newline character.
     */
    private List<String> getVerifytgaInputLines(final Process process) {
        List<String> lines;

        try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            lines = inputReader.lines().collect(Collectors.toList());
            if (!handlePotentialErrorsFromVerifytga(process)) lines = null;
        } catch (final IOException e) {
            e.printStackTrace();

            // Only show error if the process is not already being stopped
            if (getPlan().getStatus().equals(MutationTestPlan.Status.WORKING)) {
                getPlan().setStatus(MutationTestPlan.Status.STOPPING);
                Platform.runLater(() -> {
                    final String message = "I/O exception while reading from verifytga: " + e.getMessage();
                    final Text text = new Text(message);
                    text.setFill(Color.RED);
                    writeProgress(text);
                    Ecdar.showToast(message);
                });
            }
            lines = null;
        }

        return lines;
    }

    /**
     * Checks for errors in a process.
     * The input stream must be completely read before calling this.
     * Otherwise, we rick getting stuck while reading the error stream.
     * If an error occurs, this method tells the user and signals this controller to stop.
     * @param process process to check for
     * @return true iff process was successful (e.g. no errors was found)
     * @throws IOException if an I/O error occurs
     */
    private boolean handlePotentialErrorsFromVerifytga(final Process process) throws IOException {
        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            //final String errorLine = errorReader.readLine();
            final List<String> errorLines = errorReader.lines().collect(Collectors.toList());

            if (!errorLines.isEmpty()) {
                // Only show error if the process is not already being stopped
                if (getPlan().getStatus().equals(MutationTestPlan.Status.WORKING)) {
                    getPlan().setStatus(MutationTestPlan.Status.STOPPING);
                    Platform.runLater(() -> {
                        final String message = "Error from the error stream of verifytga: " + String.join("\n", errorLines);
                        final Text text = new Text(message);
                        text.setFill(Color.RED);
                        writeProgress(text);
                        Ecdar.showToast(message);
                    });
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Is triggered when a test-case generation attempt is done.
     * It updates UI labels to tell user about the progress.
     * Once all test-case generation attempts are done,
     * this method executes the test-cases (not done)
     *
     * This method should be called in a JavaFX thread, since it updates JavaFX elements.
     */
    private synchronized void onGenerationJobDone() {
        generationJobsEnded++;

        plan.setTestCasesText("Test-cases: " + testCases.size() + " - Generation time: " + readableFormat(Duration.between(generationStart, Instant.now())));

        updateGenerationJobs();
    }

    public void onSelectSutButtonPressed() {
        sutPathLabel.setText("The\\Path\\To\\My\\System\\Under\\Test");
        // TODO implement
    }

    public void onExportButtonPressed() {

    }
}
