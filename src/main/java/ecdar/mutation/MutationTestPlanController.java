package ecdar.mutation;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Project;
import ecdar.backend.BackendException;
import ecdar.backend.UPPAALDriver;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for a test plan with model-based mutation testing.
 */
public class MutationTestPlanController {
    private final static String TEST_MODEL_NAME = "S";
    private final static String MUTANT_NAME = "M";

    // UI elements
    public JFXComboBox<Label> testModelPicker;
    public JFXButton testButton;
    public JFXButton stopButton;
    public Label mutantsText;
    public Label testCasesText;
    public Label progressText;

    // Mutation objects
    private MutationTestPlan plan;
    private Component testModel;
    private List<Component> mutants;
    private ObservableList<MutationTestCase> testCases;

    // Progress fields
    private Instant generationStart;
    private String queryFilePath;
    private int generationJobsStarted;
    private int generationJobsEnded;
    private boolean shouldStop;

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
        showStopButton();
        shouldStop = false;

        // Find test model from test model picker
        // Clone it, because we want to change its name
        testModel = Ecdar.getProject().findComponent(testModelPicker.getValue().getText()).cloneForVerification();
        testModel.setName(TEST_MODEL_NAME);
        testModel.updateIOList();

        // Mutate and make input-enabled with angelic completion
        final Instant start = Instant.now();
        mutants = new ChangeSourceOperator(testModel).computeMutants();
        mutants.addAll(new ChangeTargetOperator(testModel).computeMutants());
        mutants.forEach(Component::applyAngelicCompletion);
        plan.setMutantsText("Mutants: " + mutants.size() + " - Execution time: " + humanReadableFormat(Duration.between(start, Instant.now())));

        // Create test cases
        generationJobsStarted = 0;
        generationJobsEnded = 0;
        generationStart = Instant.now();
        testCases = FXCollections.observableArrayList();

        try {
            queryFilePath = UPPAALDriver.storeQuery("refinement: " + MUTANT_NAME + "<=" + TEST_MODEL_NAME, "query");
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
            Ecdar.showToast("Error: " + e.getMessage());
            return;
        }

        updateGenerationJobs();
    }

    public synchronized void onStopButtonPressed() {
        shouldStop = true;
        stopButton.setDisable(true);
    }

    /**
     * Converts a duration to a human readable format, e.g. 0.293s.
     * @param duration the duration
     * @return a string in a human readable format
     */
    private static String humanReadableFormat(final Duration duration) {
        return duration.toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase();
    }

    private synchronized void updateGenerationJobs() {
        if (shouldStop) {
            if (generationJobsRunning() == 0) {
                progressText.setText("Stopped");
                stopButton.setDisable(false);

                showTestButton();
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

            progressText.setText("Done");
            testButton.setDisable(false);

            showTestButton();

            return;
        }

        progressText.setText("Generating test-cases... (" + generationJobsEnded + "/" + mutants.size() + " mutants processed)");

        // while we have not reach the maximum allowed threads and there are still jobs to start
        final int MAX_GENERATION_THREADS = 10;
        while (generationJobsRunning() < MAX_GENERATION_THREADS &&
                generationJobsStarted < mutants.size()) {
            generateTestCase(testModel, mutants.get(generationJobsStarted), generationJobsStarted);
            generationJobsStarted++;
        }
    }

    private void showStopButton() {
        testButton.setManaged(false); testButton.setVisible(false);
        stopButton.setManaged(true); stopButton.setVisible(true);
    }

    private void showTestButton() {
        testButton.setManaged(true); testButton.setVisible(true);
        stopButton.setManaged(false); stopButton.setVisible(false);
    }

    private synchronized int generationJobsRunning() {
        return generationJobsStarted - generationJobsEnded;
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
            final Process process;

            try {
                // Store the project and the refinement query as backend XML
                final String modelPath = UPPAALDriver.storeBackendModel(project, "model" + mutationIndex);

                // Run verifytga to check refinement and to fetch strategy if non-refinement
                process = Runtime.getRuntime().exec(UPPAALDriver.findVerifytgaAbsolutePath() + " -t0 " + modelPath + " " + queryFilePath);

            } catch (BackendException | IOException | URISyntaxException e) {
                e.printStackTrace();
                Ecdar.showToast("Error: " + e.getMessage());
                return;
            }

            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));

            List<String > lines = input.lines().collect(Collectors.toList());

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
                throw new RuntimeException("Output from verifytga not understood:\n" + String.join("\n", lines) + "\n" +
                        "Mutation index: " + mutationIndex);
            }

            int strategyIndex = lines.indexOf("Strategy for the attacker:");

            // If no such index, error
            if (strategyIndex < 0) {
                throw new RuntimeException("Output from verifytga not understood:\n" + String.join("\n", lines) + "\n" +
                        "Mutation index: " + mutationIndex);
            }

            List<String> strategy = lines.subList(strategyIndex + 2, lines.size());

            testCases.add(new MutationTestCase(testModel, mutant, strategy));

            // JavaFX elements cannot be updated in another thread, so make it run in a JavaFX thread at some point
            Platform.runLater(this::onGenerationJobDone);
        }).start();
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

        plan.setTestCasesText("Test-cases: " + testCases.size() + " - Generation time: " + humanReadableFormat(Duration.between(generationStart, Instant.now())));

        updateGenerationJobs();
    }
}
