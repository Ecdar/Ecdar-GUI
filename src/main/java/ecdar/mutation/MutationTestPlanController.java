package ecdar.mutation;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.mutation.models.MutationTestCase;
import ecdar.mutation.models.MutationTestPlan;
import ecdar.mutation.models.TestResult;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for a test plan with model-based mutation testing.
 */
public class MutationTestPlanController {
    public final static String SPEC_NAME = "S";
    public final static String MUTANT_NAME = "M";


    /* UI elements */

    public ScrollPane scrollPane;
    public JFXComboBox<Label> modelPicker;
    public VBox modelDependentArea;
    public VBox operatorsInnerRegion;
    public JFXComboBox<Label> actionPicker;

    public VBox testDependentArea;
    public JFXButton selectSutButton;
    public Label sutPathLabel;
    public VBox sutDependentArea;
    public VBox demonicArea;
    public JFXCheckBox demonicCheckBox;
    public JFXTextField generationThreadsField;
    public JFXTextField suvInstancesField;
    public JFXTextField outputWaitTimeField;
    public HBox outputWaitTimeBox;
    public JFXButton testButton;
    public JFXButton stopButton;

    public VBox exportDependantArea;
    public JFXComboBox<Label> formatPicker;
    public JFXCheckBox angelicBox;
    public JFXButton storeMutantsButton;

    public VBox progressAres;
    public TextFlow progressTextFlow;

    public VBox resultsArea;
    public Label mutantsText;
    public Label testCasesText;
    public Label passedText;
    public Label inconclusiveText;
    public Label failedText;
    public StackPane root;
    public JFXTextField verifytgaTriesField;
    public VBox inconclusiveResults;
    public VBox failedResults;
    public JFXTextField timeUnitField;
    public HBox timeUnitBox;
    public Label opsLabel;
    public Label advancedOptionsLabel;
    public Pane advancedOptions;
    public HBox failedRegion;
    public HBox inconclusiveRegion;
    public Label testTimeText;
    public HBox operatorsOuterRegion;
    public JFXTextField stepBoundsField;
    public JFXButton failedTestButton;
    public JFXButton inconclusiveTestButton;


    /* Mutation fields */

    private MutationTestPlan plan;
    private TestingHandler testingHandler;


    /* Properties */

    public MutationTestPlan getPlan() {
        return plan;
    }

    public void setPlan(final MutationTestPlan plan) {
        this.plan = plan;
        testingHandler = new TestingHandler(plan);
    }


    /* Other methods */

    /**
     * Triggered when pressed the test button.
     * Conducts the test.
     */
    public void onTestButtonPressed() {
        getPlan().clearResults();

        // Find test model from test model picker
        // Clone it, because we want to change its name
        final Component testModel = Ecdar.getProject().findComponent(modelPicker.getValue().getText()).cloneForVerification();

        new MutationHandler(testModel, getPlan(), cases -> startGeneration(testModel, cases)).start();
    }

    /**
     * Starts the test-case generation.
     * If the status is not working, ignore instead
     * @param testModel test model
     * @param cases potential test-cases containing the mutants
     */
    private synchronized void startGeneration(final Component testModel, final List<MutationTestCase> cases) {
        if (getPlan().shouldStop()) {
            getPlan().setStatus(MutationTestPlan.Status.IDLE);
            return;
        }

        new TestCaseGenerationHandler(getPlan(), testModel, cases, this::startTestDriver).start();
    }

    /**
     * Starts the test driver.
     * @param cases the mutation test cases to test with
     */
    private void startTestDriver(final List<MutationTestCase> cases) {
        new TestingHandler(plan).testFromScratch(cases);
    }

    /**
     * Triggered when pressed the stop button.
     * Signals that this test plan should stop doing jobs.
     */
    public void onStopButtonPressed() {
        getPlan().writeProgress("Stopping");
        getPlan().setStatus(MutationTestPlan.Status.STOPPING);
    }

    /**
     * Triggered when pressed the inconclusive test button.
     * Retests the inconclusive test-cases.
     */
    public void onInconclusiveTestButtonPressed() {
        final List<MutationTestCase> cases = getPlan().getInconclusiveResults().stream().map(TestResult::getTestCase).collect(Collectors.toList());
        getPlan().getInconclusiveResults().clear();
        testingHandler.retest(cases);
    }

    /**
     * Triggered when pressed the failed test button.
     * Retests the failed test-cases.
     */
    public void onFailedTestButtonPressed() {
        final List<MutationTestCase> cases = getPlan().getFailedResults().stream().map(TestResult::getTestCase).collect(Collectors.toList());
        getPlan().getFailedResults().clear();
        testingHandler.retest(cases);
    }

    /**
     * Opens dialog to select a SUT.
     */
    public void onSelectSutButtonPressed() {
        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose a SUT jar file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Jar files", "*.jar"));

        // The initial location for the file choosing dialog
        final File jarDir;
        if (Ecdar.projectDirectory.get() == null) {
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        } else {
            jarDir = new File(Ecdar.projectDirectory.get());


            // If the file does not exist, we must be running it from a development environment, use an default location
            if(jarDir.exists()) {
                fileChooser.setInitialDirectory(jarDir);
            }
        }

        final File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if (file != null) {
            final String path = new File(Ecdar.projectDirectory.get()).toPath().relativize(file.toPath()).toFile().getPath().replace(File.separator, "/");
            sutPathLabel.setText(path);
            plan.setSutPath(path);
        } else {
            Ecdar.showToast("Did not recognize selected file as a jar file");
        }
    }

    /**
     * Starts exporting.
     */
    public void onExportButtonPressed() {
        getPlan().clearResults();
        new ExportHandler(getPlan(), Ecdar.getProject().findComponent(modelPicker.getValue().getText())).start();
    }
}