package ecdar.mutation;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXTextField;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.controllers.CanvasController;
import ecdar.mutation.models.ExpandableContent;
import ecdar.mutation.models.MutationTestPlan;
import ecdar.mutation.models.TestResult;
import ecdar.mutation.operators.MutationOperator;
import ecdar.presentations.EcdarFXMLLoader;
import ecdar.presentations.HighLevelModelPresentation;
import javafx.beans.property.IntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Text;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

/**
 * Presentation for a test plan with model-based mutation testing.
 */
public class MutationTestPlanPresentation extends HighLevelModelPresentation {
    private final MutationTestPlanController controller;

    private final static int ARROW_HEIGHT = 19; // Height of view containing the arrow for expanding and collapsing views. For labels, this is 19

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

    /* Properties */

    private MutationTestPlan getPlan() {
        return controller.getPlan();
    }


    /* Other */

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
        initializeTestResults();

        initializePositiveIntegerTextField(controller.generationThreadsField, getPlan().getConcurrentGenerationsThreadsProperty());
        initializePositiveIntegerTextField(controller.suvInstancesField, getPlan().getConcurrentSutInstancesProperty());
        initializePositiveIntegerTextField(controller.outputWaitTimeField, getPlan().getOutputWaitTimeProperty());
        initializePositiveIntegerTextField(controller.verifytgaTriesField, getPlan().getVerifytgaTriesProperty());
        initializePositiveIntegerTextField(controller.timeUnitField, getPlan().getTimeUnitProperty());
        initializePositiveIntegerTextField(controller.stepBoundsField, getPlan().getStepBoundsProperty());

        controller.demonicCheckBox.selectedProperty().bindBidirectional(getPlan().getDemonicProperty());
        controller.angelicBox.selectedProperty().bindBidirectional(getPlan().getAngelicWhenExportProperty());

        InitializeStatusHandling();

        installTooltip(controller.demonicCheckBox, "Use this, if the test model is not input-enabled, " +
                "and you want to ignore mutants leading to these missing inputs. " +
                "We apply angelic completion on the mutants.");
        initializeWidthAndHeight();


        initializeExpand(controller.opsLabel, controller.operatorsOuterRegion);
        initializeExpand(controller.advancedOptionsLabel, controller.advancedOptions);


    }

    /**
     * Makes a region show/hide when pressing a label.
     * @param label the label
     * @param region the region
     */
    private static void initializeExpand(final Label label, final Region region) {
        final boolean[] isHidden = {true};

        label.setGraphic(createArrowPath(false));
        label.setGraphicTextGap(10);

        label.setOnMousePressed(event -> {
            isHidden[0] = !isHidden[0];
            if (isHidden[0]) {
                label.setGraphic(createArrowPath(false));
                hide(region);
            }
            else {
                label.setGraphic(createArrowPath(true));
                show(region);
            }
        });

        // Hide initially
        hide(region);
    }

    /**
     * Creates an arrow head path to draw expand and collapse graphics.
     * @param up true iff the arrow should point up
     * @return the arrow head graphics
     */
    private static SVGPath createArrowPath(final boolean up) {
        return createArrowPath(ARROW_HEIGHT, up);
    }

    /**
     * Creates an arrow head path to draw expand and collapse graphics.
     * From: http://tech.chitgoks.com/2013/05/19/how-to-create-a-listview-with-title-header-that-expands-collapses-in-java-fx/
     * @param height height of the view it should match
     * @param up true iff the arrow should point up
     * @return the arrow head graphics
     */
    private static SVGPath createArrowPath(final int height, final boolean up) {
        final SVGPath svg = new SVGPath();
        final int width = height / 4;

        if (up)
            svg.setContent("M" + width + " 0 L" + (width * 2) + " " + width + " L0 " + width + " Z");
        else
            svg.setContent("M0 0 L" + (width * 2) + " 0 L" + width + " " + width + " Z");

        return svg;
    }

    /**
     * Initializes UI elements for displaying progress and results.
     */
    private void initializeProgressAndResultsTexts() {
        // Show info when added
        controller.progressTextFlow.getChildren().addListener((ListChangeListener<Node>) change -> show(controller.progressAres));
        controller.mutantsText.textProperty().addListener(((observable, oldValue, newValue) -> show(controller.resultsArea)));

        // Add progress initially
        getPlan().getProgressTexts().forEach(text -> controller.progressTextFlow.getChildren().add(text));

        // Add and remove when changed
        getPlan().getProgressTexts().addListener((ListChangeListener<Text>) change -> {
            while (change.next()) {
                change.getAddedSubList().forEach(text -> controller.progressTextFlow.getChildren().add(text));
                change.getRemoved().forEach(text -> controller.progressTextFlow.getChildren().remove(text));
            }
        });

        controller.mutantsText.textProperty().bind(getPlan().getMutantsTextProperty());
        controller.testCasesText.textProperty().bind(getPlan().getTestCasesTextProperty());
        controller.testTimeText.textProperty().bindBidirectional(getPlan().getTestTimeTextProperty());
    }

    /**
     * Initializes test results.
     */
    private void initializeTestResults() {
        controller.passedText.setText("Passed: " + getPlan().getPassedResults().size());
        getPlan().getPassedResults().addListener((ListChangeListener<TestResult>) c -> {
            while (c.next()) controller.passedText.setText("passed: " + c.getList().size());
        });

        initializeInconclusiveResults();

        initializeFailedResults();
    }

    /**
     * Initializes handling of inconclusive results.
     * Expands/collapses results, when pressed on the text.
     * Shows reset button only when there is something to retest.
     * Makes each result expandable.
     */
    private void initializeInconclusiveResults() {
        initializeExpand(controller.inconclusiveText, controller.inconclusiveRegion);

        final Consumer<List> consumer = list -> {
            final int size = list.size();
            controller.inconclusiveText.setText("Inconclusive: " + size);
            if (size == 0) hide(controller.inconclusiveTestButton);
            else show(controller.inconclusiveTestButton);
        };

        consumer.accept(getPlan().getInconclusiveResults());
        getPlan().getInconclusiveResults().addListener((ListChangeListener<TestResult>) c -> consumer.accept(c.getList()));

        getPlan().getInconclusiveResults().addListener(getExpandableListListener(controller.inconclusiveResults.getChildren()));
    }

    private void initializeFailedResults() {
        initializeExpand(controller.failedText, controller.failedRegion);

        final Consumer<List> consumer = list -> {
            final int size = list.size();
            controller.failedText.setText("Failed: " + size);
            if (size == 0) hide(controller.failedTestButton);
            else show(controller.failedTestButton);
        };

        consumer.accept(getPlan().getFailedResults());
        getPlan().getFailedResults().addListener((ListChangeListener<TestResult>) c -> consumer.accept(c.getList()));

        getPlan().getFailedResults().addListener(getExpandableListListener(controller.failedResults.getChildren()));
    }

    /**
     * Gets a listener for adding expandable content.
     * @param list the list to add the content to
     * @return the listener
     */
    private static ListChangeListener<ExpandableContent> getExpandableListListener(final ObservableList<Node> list) {
        final Map<ExpandableContent, VBox> contentModelMap = new HashMap<>();

        return change -> {
            while (change.next()) {
                change.getAddedSubList().forEach(item -> {
                    final VBox vBox = new VBox();
                    contentModelMap.put(item, vBox);
                    list.add(vBox);

                    final Label labelHeader = new Label(item.getTitle());

                    labelHeader.setGraphic(createArrowPath(false));
                    labelHeader.setGraphicTextGap(10);

                    labelHeader.setOnMouseClicked(e -> {
                        item.setHidden(!item.isHidden());
                        if (item.isHidden()) {
                            labelHeader.setGraphic(createArrowPath(false));
                            vBox.getChildren().remove(vBox.getChildren().size() - 1);
                        } else {
                            labelHeader.setGraphic(createArrowPath(true));

                            final HBox hBox = new HBox();
                            hBox.setSpacing(8);
                            hBox.getChildren().add(new Separator(Orientation.VERTICAL));
                            final Label content = new Label(item.getContent());
                            content.setWrapText(true);
                            hBox.getChildren().add(content);

                            vBox.getChildren().add(hBox);
                        }
                    });

                    vBox.getChildren().add(labelHeader);
                });

                change.getRemoved().forEach(item -> list.remove(contentModelMap.get(item)));
            }
        };
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
            final String testModelId = getPlan().getTestModelId();
            if (testModelId != null && testModelId.equals(component.getName()))
                controller.modelPicker.setValue(label);
        });

        // Bind test plan to test model picker
        controller.modelPicker.valueProperty().addListener(((observable, oldValue, newValue) ->
                getPlan().setTestModelId(newValue.getText())));

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
     * Initializes width and height of the text editor field, such that it fills up the whole canvas.
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
        for (final MutationOperator operator : getPlan().getOperators()) {
            final JFXCheckBox checkBox = new JFXCheckBox(operator.getText());
            checkBox.selectedProperty().bindBidirectional(operator.getSelectedProperty());
            controller.operatorsInnerRegion.getChildren().add(checkBox);

            installTooltip(checkBox, operator.getDescription());
        }
    }

    /**
     * Initializes handling of the status of the test plan.
     */
    private void InitializeStatusHandling() {
        handleStatusUpdate(null, getPlan().getStatus());
        getPlan().getStatusProperty().addListener(((observable, oldValue, newValue) -> handleStatusUpdate(oldValue, newValue)));
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
                    getPlan().writeProgress("Stopped");

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
        regions.add(controller.operatorsInnerRegion);
        regions.add(controller.actionPicker);
        regions.add(controller.demonicArea);
        regions.add(controller.selectSutButton);
        regions.add(controller.sutPathLabel);
        regions.add(controller.exportDependantArea);
        regions.add(controller.outputWaitTimeBox);
        regions.add(controller.timeUnitBox);

        return regions;
    }

    /**
     * Initializes a text field to enforce its values to be positive integers.
     * While in focus, the text field can still have an empty value.
     * Makes the field update when the property updates.
     * @param field the text field
     * @param property the property
     */
    private static void initializePositiveIntegerTextField(final JFXTextField field, final IntegerProperty property) {
        // Set value initially
        field.setText(String.valueOf(property.get()));

        // Force the field to be empty positive integer
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            String updateValue = newValue;

            if (!updateValue.matches("\\d*")) updateValue = newValue.replaceAll("[^\\d]", "");
            if (updateValue.equals("0")) updateValue = "1";

            if (!updateValue.equals(newValue)) {
                field.setText(updateValue);
            }

            // Update property when text field changes, if not empty
            if (!updateValue.isEmpty() && !updateValue.equals(String.valueOf(property.get()))) property.set(Integer.parseInt(updateValue));
        });

        // If empty when leaving focus, set to "1"
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
        controller.sutPathLabel.textProperty().bindBidirectional(getPlan().getSutPathProperty());
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
                getPlan().setAction(newValue.getText())));

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
        if (getPlan().getAction().equals("Export mutants")) controller.actionPicker.setValue(exportLabel);
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
        if (getPlan().getAction().equals("XML")) controller.formatPicker.setValue(xmlLabel);
        else controller.formatPicker.setValue(jsonLabel);

        controller.formatPicker.valueProperty().addListener(((observable, oldValue, newValue) ->
                getPlan().setFormat(newValue.getText())));
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
