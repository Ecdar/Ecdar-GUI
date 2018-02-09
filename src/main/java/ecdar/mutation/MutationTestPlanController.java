package ecdar.mutation;

import com.jfoenix.controls.JFXTextField;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Project;
import ecdar.backend.BackendException;
import ecdar.backend.UPPAALDriver;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
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
    public JFXTextField generationThreadsTextFields;

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
        progressText.setTextFill(Color.BLACK);

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

    /**
     * Triggered when pressed the stop button.
     * Signals that this test plan should stop doing jobs.
     */
    public void onStopButtonPressed() {
        signalToStop();
    }

    /**
     * Signals that this test plan should stop doing jobs.
     * Jobs are run in other threads, so it might take some time.
     */
    private synchronized void signalToStop() {
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
        while (generationJobsRunning() < getMaxGenerationThreads() &&
                generationJobsStarted < mutants.size()) {
            generateTestCase(testModel, mutants.get(generationJobsStarted), generationJobsStarted);
            generationJobsStarted++;
        }
    }

    private int getMaxGenerationThreads() {
        if (generationThreadsTextFields.getText().isEmpty() || generationThreadsTextFields.getText().equals("0"))
            return 1;
        else
            return Integer.parseInt(generationThreadsTextFields.getText());
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
                ProcessBuilder builder = new ProcessBuilder(UPPAALDriver.findVerifytgaAbsolutePath(), "-t0", modelPath, queryFilePath);
                //ProcessBuilder builder = new ProcessBuilder("C:\\Users\\Tobias\\Documents\\ecdar-0.10\\bin-Win32\\verifytga.exe", "-t0", modelPath, queryFilePath);
                process = builder.start();//Runtime.getRuntime().exec("C:\\Users\\Tobias\\Documents\\ecdar-0.10\\bin-Win32\\verifytga.exe" + " -t0 " + modelPath + " " + queryFilePath);

            } catch (BackendException | IOException | URISyntaxException e) {
                e.printStackTrace();
                Ecdar.showToast("Error: " + e.getMessage());
                return;
            }

            List<String> lines;
            try (BufferedReader inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                lines = inputReader.lines().collect(Collectors.toList());

                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    //final String errorLine = errorReader.readLine();
                    final List<String> errorLines = errorReader.lines().collect(Collectors.toList());

                    if (!errorLines.isEmpty()) {
                        // Only show error if the process is not already being stopped
                        if (!shouldStop) {
                            Platform.runLater(() -> {
                                final String message = "Error from the error stream of verifytga: " + String.join("\n", errorLines);
                                progressText.setText(message);
                                progressText.setTextFill(Color.RED);
                                Ecdar.showToast(message);
                                signalToStop();
                            });
                        }
                        return;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();

                // Only show error if the process is not already being stopped
                if (!shouldStop) {
                    Platform.runLater(() -> {
                        final String message = "I/O exception while reading from verifytga: " + e.getMessage();
                        progressText.setText(message);
                        progressText.setTextFill(Color.RED);
                        Ecdar.showToast(message);
                        signalToStop();
                    });
                }
                return;
            }

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
