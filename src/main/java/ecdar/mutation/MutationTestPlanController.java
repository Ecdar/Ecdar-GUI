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

    }
}
