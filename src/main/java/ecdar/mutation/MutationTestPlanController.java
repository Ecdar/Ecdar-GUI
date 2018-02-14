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
    public ScrollPane scrollPane;

    // Mutation objects
    private MutationTestPlan plan;

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
        getPlan().clearResults();

        // Find test model from test model picker
        // Clone it, because we want to change its name
        final Component testModel = Ecdar.getProject().findComponent(modelPicker.getValue().getText()).cloneForVerification();

        new TestCaseGenerationHandler(getPlan(), testModel, this::writeProgress, this::getMaxConcurrentGenerationJobs).start();
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
