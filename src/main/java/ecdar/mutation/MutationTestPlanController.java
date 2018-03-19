package ecdar.mutation;

import com.jfoenix.controls.*;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.mutation.models.ExpandableContent;
import ecdar.mutation.models.MutationTestCase;
import ecdar.mutation.models.MutationTestPlan;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import jdk.nashorn.internal.ir.ExpressionStatement;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Controller for a test plan with model-based mutation testing.
 */
public class MutationTestPlanController {
    public final static String SPEC_NAME = "S";
    public final static String MUTANT_NAME = "M";

    private static final int TEST_STEP_BOUND = 100;


    /* UI elements */

    public ScrollPane scrollPane;
    public JFXComboBox<Label> modelPicker;
    public VBox modelDependentArea;
    public VBox operatorsArea;
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
    public VBox inconclusiveMessageList;
    public VBox failedMessageList;
    public JFXTextField timeUnitField;
    public HBox timeUnitBox;
    public Label opsLabel;
    public Label advancedOptionsLabel;
    public Pane advancedOptions;
    public HBox failedRegion;
    public HBox inconclusiveRegion;


    /* Mutation fields */

    private MutationTestPlan plan;


    /* Properties */

    public MutationTestPlan getPlan() {
        return plan;
    }

    public void setPlan(final MutationTestPlan plan) {
        this.plan = plan;
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

        new TestCaseGenerationHandler(getPlan(), testModel, this::startTestDriver).start();
    }

    /**
     * Starts the test driver.
     * @param cases the mutation test cases to test with
     */
    private void startTestDriver(final List<MutationTestCase> cases) {
        new TestDriver(cases, plan, getPlan().getTimeUnit(), TEST_STEP_BOUND).start();
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
     * Triggered when pressed the stop button.
     * Signals that this test plan should stop doing jobs.
     */
    public void onStopButtonPressed() {
        getPlan().writeProgress("Stopping");
        getPlan().setStatus(MutationTestPlan.Status.STOPPING);
    }

    /**
     * Opens dialog to select a SUT.
     */
    public void onSelectSutButtonPressed() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose a SUT jar file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Jar files", "*.jar"));

        // The initial location for the file choosing dialog
        final File jarDir;
        if(Ecdar.projectDirectory.get() == null) {
            fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        } else {
            jarDir = new File(Ecdar.projectDirectory.get());


            // If the file does not exist, we must be running it from a development environment, use an default location
            if(jarDir.exists()) {
                fileChooser.setInitialDirectory(jarDir);
            }
        }

        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if(file != null) {
            String path = new File(Ecdar.projectDirectory.get()).toPath().relativize(file.toPath()).toFile().getPath().replace(File.separator, "/");
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