package ecdar.mutation;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXTextField;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.controllers.CanvasController;
import ecdar.mutation.models.MutationOperator;
import ecdar.mutation.models.MutationTestPlan;
import ecdar.presentations.EcdarFXMLLoader;
import ecdar.presentations.HighLevelModelPresentation;
import javafx.beans.property.IntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Presentation for a test plan with model-based mutation testing.
 */
public class MutationTestPlanPresentation extends HighLevelModelPresentation {
    private final MutationTestPlanController controller;

    private double offSet, canvasHeight;

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
     * Converts a duration to a human readable format, e.g. 0.293s.
     * @param duration the duration
     * @return a string in a human readable format
     */
    static String readableFormat(final Duration duration) {
        return duration.toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase();
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

        initializeProgressAndResultsTexts();

        initializePositiveIntegerTextField(controller.generationThreadsField, controller.getPlan().maxGenerationThreadsProperty());
        initializePositiveIntegerTextField(controller.suvInstancesField, controller.getPlan().maxSutInstancesProperty());
        initializePositiveIntegerTextField(controller.outputWaitTimeField, controller.getPlan().outputWaitTimeProperty());

        controller.demonicCheckBox.selectedProperty().bindBidirectional(controller.getPlan().demonicProperty());
        controller.angelicBox.selectedProperty().bindBidirectional(controller.getPlan().angelicWhenExportProperty());

        InitializeStatusHandling();

        installTooltip(controller.demonicCheckBox, "Use this, if the test model is not input-enabled, " +
                "and you want to ignore mutants leading to these missing inputs. " +
                "We apply angelic completion on the mutants.");
        initializeWidthAndHeight();
    }

    /**
     * Initializes UI elements for displaying progress and results.
     */
    private void initializeProgressAndResultsTexts() {
        controller.progressTextFlow.getChildren().addListener((ListChangeListener<Node>) change -> show(controller.progressAres));
        controller.mutantsText.textProperty().addListener(((observable, oldValue, newValue) -> show(controller.resultsArea)));

        controller.mutantsText.textProperty().bind(controller.getPlan().mutantsTextProperty());
        controller.testCasesText.textProperty().bind(controller.getPlan().testCasesTextProperty());
        controller.passedText.textProperty().bind(controller.getPlan().passedTextProperty());
        controller.inconclusiveText.textProperty().bind(controller.getPlan().inconclusiveTextProperty());
        controller.failedText.textProperty().bind(controller.getPlan().failedTextProperty());
    }

    /**
     * Initializes the model picker.
     */
    private void initializeModelPicker() {
        // Fill test model picker with sorted components
        Ecdar.getProject().getComponents().stream().sorted(Comparator.comparing(Component::getName)).forEach(component -> {
            final Label label = new Label(component.getName());
            controller.modelPicker.getItems().add(label);

            // If component is the test model of the test plan, select it
            final String testModelId = controller.getPlan().getTestModelId();
            if (testModelId != null && testModelId.equals(component.getName()))
                controller.modelPicker.setValue(label);
        });

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

    /**
     * Initializes width and height of the text editor field, such that it fills up the whole canvas
     */
    private void initializeWidthAndHeight() {
        controller.scrollPane.setPrefWidth(CanvasController.getWidthProperty().doubleValue());
        CanvasController.getWidthProperty().addListener((observable, oldValue, newValue) ->
                controller.scrollPane.setPrefWidth(newValue.doubleValue()));

        updateOffset(CanvasController.getInsetShouldShow().get());
        CanvasController.getInsetShouldShow().addListener((observable, oldValue, newValue) -> {
            updateOffset(newValue);
            updateHeight();
        });

        canvasHeight = CanvasController.getHeightProperty().doubleValue();
        updateHeight();
        CanvasController.getHeightProperty().addListener((observable, oldValue, newValue) -> {
            canvasHeight = newValue.doubleValue();
            updateHeight();
        });
    }

    /**
     * Updates if height of the view should have an offset at the bottom.
     * Whether the view should have an offset is based on the configuration of the error view.
     * @param shouldHave true iff views should have an offset
     */
    private void updateOffset(final boolean shouldHave) {
        if (shouldHave) {
            offSet = 20;
        } else {
            offSet = 0;
        }
    }

    /**
     * Updates the height of the view.
     */
    private void updateHeight() {
        controller.scrollPane.setPrefHeight(canvasHeight - CanvasController.DECLARATION_Y_MARGIN - offSet);
    }

    /**
     * Initializes the UI for selecting mutation operators.
     */
    private void initializeOperators() {
        for (final MutationOperator operator : controller.getPlan().getOperators()) {
            final JFXCheckBox checkBox = new JFXCheckBox(operator.getText());
            checkBox.selectedProperty().bindBidirectional(operator.selectedProperty());
            controller.operatorsArea.getChildren().add(checkBox);

            installTooltip(checkBox, operator.getDescription());
        }
    }

    /**
     * Initializes handling of the status of the test plan.
     */
    private void InitializeStatusHandling() {
        handleStatusUpdate(null, controller.getPlan().getStatus());
        controller.getPlan().statusProperty().addListener(((observable, oldValue, newValue) -> handleStatusUpdate(oldValue, newValue)));
    }

    /**
     * Handles an update of the status of the test plan.
     * @param oldValue the old status
     * @param newValue the new status
     */
    private void handleStatusUpdate(final MutationTestPlan.Status oldValue, final MutationTestPlan.Status newValue) {
        switch (newValue) {
            case IDLE:
                show(controller.testButton);
                hide(controller.stopButton);
                for (final Region region : getRegionsToDisableWhileWorking()) region.setDisable(false);

                if (oldValue != null && oldValue.equals(MutationTestPlan.Status.STOPPING))
                    controller.writeProgress("Stopped");

                break;
            case WORKING:
                hide(controller.testButton);
                show(controller.stopButton);
                controller.stopButton.setDisable(false);
                for (final Region region : getRegionsToDisableWhileWorking()) region.setDisable(true);
                break;
            case STOPPING:
            case ERROR:
                controller.stopButton.setDisable(true);
                break;
        }
    }

    /**
     * Gets which regions should be disabled while working.
     * @return the regions
     */
    private List<Region> getRegionsToDisableWhileWorking() {
        final List<Region> regions = new ArrayList<>();

        regions.add(controller.modelPicker);
        regions.add(controller.operatorsArea);
        regions.add(controller.actionPicker);
        regions.add(controller.demonicArea);
        regions.add(controller.selectSutButton);
        regions.add(controller.sutPathLabel);
        regions.add(controller.exportDependantArea);

        return regions;
    }

    /**
     * Initializes a text field to enforce its values to be positive integers.
     * While in focus, the text field can still have an empty value.
     * @param field the text field
     */
    private static void initializePositiveIntegerTextField(final JFXTextField field, final IntegerProperty property) {
        // Set value initially
        field.setText(String.valueOf(property.get()));

        // Update property when text field changes
        field.textProperty().addListener(((observable, oldValue, newValue) -> {
            if (!newValue.isEmpty()) property.setValue(Integer.parseInt(newValue));
        }));

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

    /**
     * Initializes the action picker.
     */
    private void initializeActionPicker() {
        final Label testLabel = new Label("Test");
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

    /**
     * Installs a tooltip on a control.
     * The tooltip is shown without delay on mouse entered.
     * The tooltip does not disappear after some time.
     * @param control the control
     * @param text the text of the tooltip
     */
    private static void installTooltip(final Control control, final String text) {
        final Tooltip tooltip = new Tooltip(text);
        tooltip.setPrefWidth(250);
        tooltip.setWrapText(true);

        // Show the tooltip at the bottom right of the control as to not trigger on mouse existed
        control.setOnMouseEntered(event -> {
            final Point2D point = control.localToScreen(control.getLayoutBounds().getMaxX(), control.getLayoutBounds().getMaxY());
            tooltip.show(control, point.getX(), point.getY());
        });
        control.setOnMouseExited(event -> tooltip.hide());
    }

    /**
     * Initializes handling of the format picker.
     */
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

    /**
     * Shows some regions and set them to be managed.
     * @param regions the regions
     */
    private static void show(final Node... regions) {
        for (final Node region : regions) {
            region.setManaged(true);
            region.setVisible(true);
        }
    }

    /**
     * Hides some regions and set them to not be managed.
     * @param regions the regions
     */
    private static void hide(final Node... regions) {
        for (final Node region : regions) {
            region.setManaged(false);
            region.setVisible(false);
        }
    }
}
