package ecdar.mutation;

import com.jfoenix.controls.JFXButton;
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
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.value.ChangeListener;
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
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

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
        InitializeStatusHandling();

        initializeModelPicker();
        initializeOperators();
        initializeActionPicker();
        initializeSutPath();
        initializeEnablingTestButton();

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


        installTooltip(controller.demonicCheckBox, "Use this, if the test model is not input-enabled, " +
                "and you want to ignore mutants leading to these missing inputs. " +
                "We apply angelic completion on the mutants.");
        initializeWidthAndHeight();


        VisibilityHelper.initializeExpand(controller.opsLabel, controller.operatorsOuterRegion);
        VisibilityHelper.initializeExpand(controller.advancedOptionsLabel, controller.advancedOptions);


    }

    /**
     * Initializes enabling and disabling of the test button.
     * The button is disabled when no mutation operators are selected.
     */
    private void initializeEnablingTestButton() {
        final Runnable runnable = () -> {
            if (getPlan().getSelectedMutationOperators().isEmpty()) controller.testButton.setDisable(true);
            else controller.testButton.setDisable(false);
        };

        runnable.run();
        getPlan().getOperators().forEach(op -> op.getSelectedProperty().addListener(
                (observable, oldValue, newValue) -> runnable.run()
        ));
    }

    /**
     * Initializes UI elements for displaying progress and results.
     */
    private void initializeProgressAndResultsTexts() {
        // Show info when added
        controller.progressTextFlow.getChildren().addListener((ListChangeListener<Node>) change -> VisibilityHelper.show(controller.progressAres));
        controller.mutantsText.textProperty().addListener(((observable, oldValue, newValue) -> VisibilityHelper.show(controller.resultsArea)));

        // Add progress initially
        getPlan().getProgressTexts().forEach(text -> controller.progressTextFlow.getChildren().add(text));

        // Add and remove when changed
        getPlan().getProgressTexts().addListener((ListChangeListener<Text>) change -> {
            while (change.next()) {
                change.getAddedSubList().forEach(text -> Platform.runLater(() -> controller.progressTextFlow.getChildren().add(text)));
                change.getRemoved().forEach(text -> Platform.runLater(() -> controller.progressTextFlow.getChildren().remove(text)));
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
        VisibilityHelper.initializeExpand(getPlan().getShowInconclusiveProperty(), controller.inconclusiveText, controller.inconclusiveRegion);

        final Consumer<List> consumer = list -> {
            final int size = list.size();
            controller.inconclusiveText.setText("Inconclusive: " + size);
            if (size == 0) VisibilityHelper.hide(controller.inconclusiveTestButton);
            else VisibilityHelper.show(controller.inconclusiveTestButton);
        };

        consumer.accept(getPlan().getInconclusiveResults());
        getPlan().getInconclusiveResults().addListener((ListChangeListener<TestResult>) c -> consumer.accept(c.getList()));

        initializeExpandableList(getPlan().getInconclusiveResults(), controller.inconclusiveResults.getChildren());
    }

    /**
     * Initializes handling of failed results.
     * Expands/collapses results, when pressed on the text.
     * Shows reset button only when there is something to retest.
     * Makes each result expandable.
     */
    private void initializeFailedResults() {
        VisibilityHelper.initializeExpand(getPlan().getShowFailedProperty(), controller.failedText, controller.failedRegion);

        final Consumer<List> consumer = list -> {
            final int size = list.size();
            controller.failedText.setText("Failed: " + size);
            if (size == 0) VisibilityHelper.hide(controller.failedTestButton);
            else VisibilityHelper.show(controller.failedTestButton);
        };

        consumer.accept(getPlan().getFailedResults());
        getPlan().getFailedResults().addListener((ListChangeListener<TestResult>) c -> consumer.accept(c.getList()));

        initializeExpandableList(getPlan().getFailedResults(), controller.failedResults.getChildren());
    }

    /**
     * Initializes handling of a list of test results.
     * This method makes sure that the view list is adjusted when the model list changes.
     * @param modelList the list of test results
     * @param viewList the list of nodes to add and remove nodes from
     */
    private void initializeExpandableList(final ObservableList<TestResult> modelList, final List<Node> viewList) {
        final Map<ExpandableContent, VBox> contentModelMap = new HashMap<>();

        modelList.forEach(testCase -> addExpandableResultsToView(modelList, viewList, contentModelMap, testCase));

        modelList.addListener((ListChangeListener<TestResult>) change -> {
            while (change.next()) {
                change.getAddedSubList().forEach(testResult ->
                        addExpandableResultsToView(modelList, viewList, contentModelMap, testResult));

                change.getRemoved().forEach(item -> viewList.remove(contentModelMap.get(item)));
            }
        });
    }

    /**
     * Adds a test result to af view list.
     * @param modelList the model list containing the test results
     * @param viewList the view list visible to the user
     * @param contentModelMap the map from models to views
     * @param testResult the test result to add
     */
    private void addExpandableResultsToView(final ObservableList<TestResult> modelList, final List<Node> viewList,
                                            final Map<ExpandableContent, VBox> contentModelMap, final TestResult testResult) {

        final Label titleLabel = new Label();

        final HBox header = new HBox(16, titleLabel, testResult.getTitle());

        final Node content = makeResultContent(testResult, modelList);

        final VBox vBox = new VBox(header, content);
        contentModelMap.put(testResult, vBox);

        VisibilityHelper.updateExpand(!testResult.isHidden(), titleLabel, content);

        header.setOnMouseClicked(e -> {
            testResult.setHidden(!testResult.isHidden());
            VisibilityHelper.updateExpand(!testResult.isHidden(), titleLabel, content);
        });

        viewList.add(vBox);
    }

    /**
     * Constructs content for a test result.
     * This includes a label containing info, and a retest button.
     * @param testResult the test result
     * @param resultList list of results to remove the test result from, when retesting
     * @return the content
     */
    private Node makeResultContent(final TestResult testResult, final List<? extends TestResult> resultList) {
        final Label content = new Label(testResult.getContent());
        content.setWrapText(true);

        // Add a retest button
        final JFXButton button = new JFXButton("Retest");
        button.setPrefWidth(65);
        button.setStyle("-fx-text-fill:WHITE;-fx-background-color:#4CAF50;-fx-font-size:14px;");
        button.setOnMousePressed(event -> {
            resultList.remove(testResult);
            controller.getTestingHandler().retest(testResult.getTestCase());
        });

        return new HBox(8, new Separator(Orientation.VERTICAL), new VBox(8, content, button));
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
            if (getPlan().getTestModel() == component)
                controller.modelPicker.setValue(label);
        });

        // Bind test plan to test model picker
        controller.modelPicker.valueProperty().addListener(((observable, oldValue, newValue) ->
                getPlan().setTestModel(Ecdar.getProject().findComponent(newValue.getText()))));

        // If test model is selected, show elements
        if (controller.modelPicker.getValue() != null) {
            VisibilityHelper.show(controller.modelDependentArea);
        } else {
            // Show when selected
            controller.modelPicker.valueProperty().addListener(((observable, oldValue, newValue) ->
                    VisibilityHelper.show(controller.modelDependentArea)));
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
        getPlan().getOperators().forEach(operator -> {
            final JFXCheckBox checkBox = new JFXCheckBox(operator.getText());
            checkBox.selectedProperty().bindBidirectional(operator.getSelectedProperty());

            final Text mutantsText = new Text();
            mutantsText.setFill(Color.GRAY);

            final HBox hBox = new HBox(checkBox, mutantsText);
            controller.operatorsInnerRegion.getChildren().add(hBox);

            installTooltip(checkBox, operator.getDescription());

            getMutantsTextUpdater(operator, mutantsText).changed(null, null, getPlan().getTestModel());
            getPlan().getTestModelProperty().addListener(getMutantsTextUpdater(operator, mutantsText));
        });
    }

    /**
     * Gets a change listener of the test model that updates the mutants text for an operator.
     * @param operator the operator
     * @param mutantsText the mutants text
     * @return the change listener
     */
    private static ChangeListener<Component> getMutantsTextUpdater(final MutationOperator operator, final Text mutantsText) {
        return (observable, oldValue, newValue) -> {
            if (newValue != null) mutantsText.setText(" - " +
                    (operator.isUpperLimitExact() ? "" : "up to ") + operator.getUpperLimit(newValue) + " mutants");
        };
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
                VisibilityHelper.show(controller.testButton);
                VisibilityHelper.hide(controller.stopButton);
                for (final Region region : getRegionsToDisableWhileWorking()) region.setDisable(false);

                if (oldValue != null && oldValue.equals(MutationTestPlan.Status.STOPPING))
                    getPlan().writeProgress("Stopped");

                break;
            case WORKING:
                VisibilityHelper.hide(controller.testButton);
                VisibilityHelper.show(controller.stopButton);
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
        VisibilityHelper.show(controller.sutDependentArea);
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
                VisibilityHelper.show(controller.testDependentArea);
                VisibilityHelper.hide(controller.exportDependantArea);
            } else {
                VisibilityHelper.hide(controller.testDependentArea);
                VisibilityHelper.show(controller.exportDependantArea);
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

}
