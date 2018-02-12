package ecdar.mutation;

import com.jfoenix.controls.*;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.controllers.CanvasController;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.util.ResourceBundle;

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

    public void writeProgress(final String message) {
        final Text text = new Text(message);
        text.setFill(Color.web("#333333"));
        writeProgress(text);
    }

    private void writeProgress(final Text text) {
        progressTextFlow.getChildren().clear();
        progressTextFlow.getChildren().add(text);
    }

    public void onSelectSutButtonPressed() {
        sutPathLabel.setText("The\\Path\\To\\My\\System\\Under\\Test");
        // TODO implement
    }

    public void onExportButtonPressed() {
        getPlan().clearResults();
        new ExportHandler(getPlan(), Ecdar.getProject().findComponent(modelPicker.getValue().getText()), this::writeProgress).start();
    }
}
