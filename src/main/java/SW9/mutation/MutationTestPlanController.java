package SW9.mutation;

import SW9.Ecdar;
import SW9.abstractions.Component;
import SW9.abstractions.Project;
import SW9.backend.BackendException;
import SW9.backend.UPPAALDriver;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;

import java.io.BufferedReader;
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

    public JFXComboBox<Label> testModelPicker;
    public JFXButton testButton;
    public Label mutantsText;
    public Label testCasesText;
    public Label progressText;

    private MutationTestPlan plan;
    private ObservableList<MutationTestCase> testCases;

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
        testButton.setDisable(true);

        // Find test model from test model picker
        // Clone it, because we want to change its name
        final Component testModel = Ecdar.getProject().findComponent(testModelPicker.getValue().getText()).cloneForVerification();
        testModel.setName(TEST_MODEL_NAME);
        testModel.updateIOList();

        // Mutate
        final Instant start = Instant.now();
        final List<Component> mutants = new ChangeSourceOperator(testModel).computeMutants();
        mutants.addAll(new ChangeTargetOperator(testModel).computeMutants());
        plan.setMutantsText("Mutants: " + mutants.size() + " - Execution time: " + humanReadableFormat(Duration.between(start, Instant.now())));

        // Create test cases
        // This takes a lot of time, so do it in another thread
        progressText.setText("Generating test-cases (0/" + mutants.size() + ")");
        new Thread(() -> {
            final Instant generationStart = Instant.now();
            testCases = FXCollections.observableArrayList();
            for (int i = 0; i < mutants.size(); i++) {
                generateTestCase(testModel, mutants.get(i));

                // FXJava cannot be updated in another thread, so make it run at some point
                int finalI = i;
                Platform.runLater(() -> {
                    progressText.setText("Generating test-cases (" + finalI + "/" + mutants.size() + ")");
                    plan.setTestCasesText("Test-cases: " + testCases.size() + " - Generation time: " + humanReadableFormat(Duration.between(generationStart, Instant.now())));
                });
            }

            Platform.runLater(() -> progressText.setText("Done"));
            testButton.setDisable(false);
        }).start();
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

    /**
     * Generates a test-case.
     * @param testModel test model
     * @param mutant mutant of the test model
     */
    private void generateTestCase(final Component testModel, final Component mutant) {
        // make a project with the test model and the mutant
        final Project project = new Project();
        mutant.setName(MUTANT_NAME);
        project.getComponents().addAll(testModel, mutant);
        project.setGlobalDeclarations(Ecdar.getProject().getGlobalDeclarations());
        mutant.updateIOList(); // Update io in order to get the right system declarations for the mutant
        project.setSystemDeclarations(new TwoComponentSystemDeclarations(testModel, mutant));

        try {
            // Store the project and the refinement query as backend XML
            final String modelPath = UPPAALDriver.storeBackendModel(project);
            UPPAALDriver.storeQuery("refinement: " + MUTANT_NAME + "<=" + TEST_MODEL_NAME);

            // Run verifytga to check refinement and to fetch strategy if non-refinement
            final Process p = Runtime.getRuntime().exec(UPPAALDriver.findVerifytgaAbsolutePath() + " -t0 " + modelPath);

            new Thread(() -> {
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));

                List<String > lines = input.lines().collect(Collectors.toList());

                // If refinement, no test-case to generate.
                // I use endsWith rather than contains,
                // since verifytga sometimes output some weird symbols at the start of this line.
                if (lines.stream().anyMatch(line -> line.endsWith(" -- Property is satisfied."))) return;

                // Verifytga should output that the property is not satisfied
                // If it does not, then this is an error
                if (lines.stream().noneMatch(line -> line.endsWith(" -- Property is NOT satisfied."))) {
                    throw new RuntimeException("Output from verifytga not understood:\n" + String.join("\n", lines));
                }

                int strategyIndex = lines.indexOf("Strategy for the attacker:");

                // If no such index, error
                if (strategyIndex < 0) {
                    throw new RuntimeException("Output from verifytga not understood:\n" + String.join("\n", lines));
                }

                List<String> strategy = lines.subList(strategyIndex + 2, lines.size());

                /*for (String s : strategy) System.out.println(s);
                System.out.println();
                System.out.println();*/

                testCases.add(new MutationTestCase(testModel, mutant, strategy));
            }).start();

            // We need to wait for each process
            // Otherwise, the output of verifytga is sometimes cut off
            p.waitFor();
        } catch (BackendException | IOException | URISyntaxException | InterruptedException  e) {
            e.printStackTrace();
            Ecdar.showToast("Error: " + e.getMessage());
        }
    }
}
