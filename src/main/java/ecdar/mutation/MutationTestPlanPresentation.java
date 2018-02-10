package ecdar.mutation;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXTextField;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.presentations.EcdarFXMLLoader;
import ecdar.presentations.HighLevelModelPresentation;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TooltipBuilder;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * Presentation for a test plan with model-based mutation testing.
 */
public class MutationTestPlanPresentation extends HighLevelModelPresentation {
    private final MutationTestPlanController controller;

    /**
     * Constructs the presentation and initializes it.
     * @param testPlan the test plan to present
     */
    public MutationTestPlanPresentation(final MutationTestPlan testPlan) {
        controller = new EcdarFXMLLoader().loadAndGetController("MutationTestPlanPresentation.fxml", this);
        controller.setPlan(testPlan);

        initialize();
    }

    /**
     * Initializes this.
     */
    private void initialize() {
        initializeModelPicker();
        initializeOperators();
        initializeActionPicker();
        initializeSutPath();

        initializeFormatPicker();

        controller.progressTextFlow.getChildren().addListener((ListChangeListener<Node>) change -> show(controller.progressAres));
        controller.mutantsText.textProperty().addListener(((observable, oldValue, newValue) -> show(controller.resultsArea)));

        // Bind UI text to models
        controller.mutantsText.textProperty().bind(controller.getPlan().mutantsTextProperty());
        controller.testCasesText.textProperty().bind(controller.getPlan().testCasesTextProperty());

        initializePositiveIntegerTextField(controller.generationThreadsField);
        initializePositiveIntegerTextField(controller.suvInstancesField);

        controller.demonicCheckBox.selectedProperty().bindBidirectional(controller.getPlan().demonicProperty());
        controller.angelicBox.selectedProperty().bindBidirectional(controller.getPlan().angelicWhenExportProperty());

        InitializeStatusHandling();

        installTooltip(controller.demonicCheckBox, "Use this, if the test model is not input-enabled, " +
                "and you want to ignore mutants leading to these missing inputs. " +
                "We apply angelic completion on the mutants.");
    }

    private void InitializeStatusHandling() {
        handleStatusUpdate(null, controller.getPlan().getStatus());
        controller.getPlan().statusProperty().addListener(((observable, oldValue, newValue) -> handleStatusUpdate(oldValue, newValue)));
    }

    private void handleStatusUpdate(final MutationTestPlan.Status oldValue, final MutationTestPlan.Status newValue) {
        if (oldValue != null) {
            switch (oldValue) {
                case STOPPING:
                    controller.writeProgress("Stopped");
                    break;
            }
        }

        switch (newValue) {
            case IDLE:
                show(controller.testButton);
                hide(controller.stopButton);
                for (final Region region : getRegionsToDisableWhenWorking()) region.setDisable(false);
                break;
            case WORKING:
                hide(controller.testButton);
                show(controller.stopButton);
                controller.stopButton.setDisable(false);
                for (final Region region : getRegionsToDisableWhenWorking()) region.setDisable(true);
                break;
            case STOPPING:
                controller.stopButton.setDisable(true);
                break;
        }
    }

    private List<Region> getRegionsToDisableWhenWorking() {
        final List<Region> regions = new ArrayList<>();

        regions.add(controller.modelPicker);
        regions.add(controller.operatorsArea);
        regions.add(controller.actionPicker);
        regions.add(controller.demonicArea);
        regions.add(controller.selectSutArea);
        regions.add(controller.exportDependantArea);

        return regions;
    }



    private void initializePositiveIntegerTextField(final JFXTextField field) {
        // Force the field to be empty positive integer
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) field.setText(newValue.replaceAll("[^\\d]", ""));
            if (newValue.equals("0")) field.setText("1");
        });

        field.focusedProperty().addListener(((observable, oldValue, newValue) -> {
            if (!newValue && field.getText().isEmpty()) field.setText("1");
        }));
    }

    /**
     * Initializes the UI element for the path to the system under test.
     * If it is non-empty and visible, shows UI elements.
     * Otherwise, show them when it becomes non-empty.
     */
    private void initializeSutPath() {
        controller.sutPathLabel.textProperty().bindBidirectional(controller.getPlan().sutPathProperty());
        if (!controller.sutPathLabel.getText().isEmpty() && controller.sutPathLabel.isVisible()) showSutArea();
        else controller.sutPathLabel.textProperty().addListener(((observable, oldValue, newValue) -> showSutArea()));
    }

    /**
     * Makes the select system under test button grey.
     * Shows some UI elements.
     */
    private void showSutArea() {
        controller.selectSutButton.setStyle("-fx-text-fill:WHITE;-fx-background-color:#9E9E9E;-fx-font-size:14px;");
        show(controller.sutDependentArea);
    }

    private void initializeModelPicker() {
        // Fill test model picker with components
        for (final Component component : Ecdar.getProject().getComponents()) {
            final Label label = new Label(component.getName());
            controller.modelPicker.getItems().add(label);

            // If component is the test model of the test plan, select it
            final String testModelId = controller.getPlan().getTestModelId();
            if (testModelId != null && testModelId.equals(component.getName()))
                controller.modelPicker.setValue(label);
        }

        // Bind test plan to test model picker
        controller.modelPicker.valueProperty().addListener(((observable, oldValue, newValue) ->
                controller.getPlan().setTestModelId(newValue.getText())));

        // If test model is selected, show elements
        if (controller.modelPicker.getValue() != null) {
            show(controller.modelDependentArea);
        } else {
            // Show when selected
            controller.modelPicker.valueProperty().addListener(((observable, oldValue, newValue) ->
                    show(controller.modelDependentArea)));
        }
    }

    private void initializeOperators() {
        for (final JFXCheckBox checkBox : controller.getOperatorMap().keySet()) {
            controller.operatorsArea.getChildren().add(checkBox);
        }
    }

    /**
     * Initializes the action picker.
     */
    private void initializeActionPicker() {
        final Label testLabel = new Label("Test");
        installTooltip(testLabel, "Mutates the test model, " +
                "generates test-cases based on non-refinements between the test model and the mutants, " +
                "and executes the test-cases on a system under test.");

        final Label exportLabel = new Label("Export mutants");
        controller.actionPicker.getItems().addAll(testLabel, exportLabel);

        controller.actionPicker.valueProperty().addListener(((observable, oldValue, newValue) ->
                controller.getPlan().setAction(newValue.getText())));

        // Change visibility of areas when action changes
        controller.actionPicker.valueProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue == testLabel) {
                show(controller.testDependentArea);
                hide(controller.exportDependantArea);
            } else {
                hide(controller.testDependentArea);
                show(controller.exportDependantArea);
            }
        }));

        // Set action from model, or Test if not selected
        if (controller.getPlan().getAction().equals("Export mutants")) controller.actionPicker.setValue(exportLabel);
        else controller.actionPicker.setValue(testLabel);
    }

    private void installTooltip(final Control testLabel, final String text) {
        final Tooltip tooltip = new Tooltip(text);
        tooltip.setPrefWidth(400);
        tooltip.setWrapText(true);
        testLabel.setTooltip(tooltip);
    }

    private void initializeFormatPicker() {
        final Label jsonLabel = new Label("JSON");
        final Label xmlLabel = new Label("XML");
        controller.formatPicker.getItems().addAll(jsonLabel, xmlLabel);

        // Set action from model, or JSON if not selected
        if (controller.getPlan().getAction().equals("XML")) controller.formatPicker.setValue(xmlLabel);
        else controller.formatPicker.setValue(jsonLabel);

        controller.formatPicker.valueProperty().addListener(((observable, oldValue, newValue) ->
                controller.getPlan().setFormat(newValue.getText())));
    }

    private void show(final Node ... regions) {
        for (final Node region : regions) {
            region.setManaged(true);
            region.setVisible(true);
        }
    }

    private void hide(final Node ... regions) {
        for (final Node region : regions) {
            region.setManaged(false);
            region.setVisible(false);
        }
    }
}
