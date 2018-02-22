package ecdar.mutation;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXTextField;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.mutation.models.MutationTestPlan;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

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
    public VBox operatorsArea;
    public JFXComboBox<Label> actionPicker;

    public VBox testDependentArea;
    public HBox selectSutArea;
    public JFXButton selectSutButton;
    public Label sutPathLabel;
    public VBox sutDependentArea;
    public VBox demonicArea;
    public JFXCheckBox demonicCheckBox;
    public JFXTextField generationThreadsField;
    public JFXTextField suvInstancesField;
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

        new TestCaseGenerationHandler(getPlan(), testModel, this::writeProgress).start();
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
        writeProgress("Stopping");
        getPlan().setStatus(MutationTestPlan.Status.STOPPING);
    }

    /**
     * Writes progress to the user.
     * @param message the message to display
     */
    public void writeProgress(final String message) {
        final Text text = new Text(message);
        text.setFill(Color.web("#333333"));
        writeProgress(text);
    }

    /**
     * Writes progress to the user.
     * @param text the message to display
     */
    private void writeProgress(final Text text) {
        progressTextFlow.getChildren().clear();
        progressTextFlow.getChildren().add(text);
    }

    /**
     * Opens dialog to select a SUT.
     */
    public void onSelectSutButtonPressed() {
        sutPathLabel.setText("The\\Path\\To\\My\\System\\Under\\Test");
        // TODO implement
    }

    /**
     * Starts exporting.
     */
    public void onExportButtonPressed() {
        getPlan().clearResults();
        new ExportHandler(getPlan(), Ecdar.getProject().findComponent(modelPicker.getValue().getText()), this::writeProgress).start();
    }
}
