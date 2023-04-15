package ecdar.controllers;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXRippler;
import com.jfoenix.controls.JFXToggleButton;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.DisplayableEdge;
import ecdar.abstractions.EdgeStatus;
import ecdar.presentations.*;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.helpers.SelectHelper;
import ecdar.utility.keyboard.Keybind;
import ecdar.utility.keyboard.KeyboardTracker;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Pair;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

public class EditorController implements ModeController, Initializable {
    public VBox root;
    public HBox toolbar;
    public JFXRippler colorSelected;
    public JFXRippler deleteSelected;
    public JFXRippler undo;
    public JFXRippler redo;
    public JFXButton switchToInputButton;
    public JFXButton switchToOutputButton;
    public JFXToggleButton switchEdgeStatusButton;
    public StackPane canvasPane;

    public final ProjectPanePresentation projectPane = new ProjectPanePresentation();
    public final QueryPanePresentation queryPane = new QueryPanePresentation();

    private final ObjectProperty<EdgeStatus> globalEdgeStatus = new SimpleObjectProperty<>(EdgeStatus.INPUT);
    private final ObjectProperty<CanvasPresentation> activeCanvasPresentation = new SimpleObjectProperty<>(new CanvasPresentation());

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        initializeCanvasPane();
        initializeEdgeStatusHandling();
        initializeKeybindings();
        Platform.runLater(this::initializeSidePanes);

        // Update file coloring when active model changes
        getActiveCanvasPresentation().getController().activeModelProperty().addListener((observable, oldModel, newModel) -> projectPane.getController().swapHighlightBetweenTwoModelFiles(oldModel, newModel));
    }

    private void initializeSidePanes() {
        final DoubleProperty prevX = new SimpleDoubleProperty();
        final DoubleProperty prevWidth = new SimpleDoubleProperty();

        queryPane.getController().resizeAnchor.setOnMousePressed(event -> {
            event.consume();

            prevX.set(event.getScreenX());
            prevWidth.set(queryPane.getWidth());
        });

        queryPane.getController().resizeAnchor.setOnMouseDragged(event -> {
            double diff = prevX.get() - event.getScreenX();

            // Set bounds for resizing to be between 280px and half the screen width
            final double newWidth = Math.min(Math.max(prevWidth.get() + diff, 280), Ecdar.getPresentation().getWidth() / 2);

            queryPane.setMinWidth(newWidth);
            queryPane.setMaxWidth(newWidth);
        });

        projectPane.getStyleClass().add("responsive-pane-sizing");
        queryPane.getStyleClass().add("responsive-pane-sizing");
    }

    public EdgeStatus getGlobalEdgeStatus() {
        return globalEdgeStatus.get();
    }

    ObjectProperty<CanvasPresentation> activeCanvasPresentationProperty() {
        return activeCanvasPresentation;
    }

    public CanvasPresentation getActiveCanvasPresentation() {
        return activeCanvasPresentation.get();
    }

    public void setActiveCanvasPresentation(CanvasPresentation newActiveCanvasPresentation) {
        activeCanvasPresentation.get().setOpacity(0.75);
        newActiveCanvasPresentation.setOpacity(1);
        activeCanvasPresentation.set(newActiveCanvasPresentation);
    }

    public void setActiveModelPresentationForActiveCanvas(HighLevelModelPresentation newActiveModelPresentation) {
        getActiveCanvasPresentation().getController().setActiveModelPresentation(newActiveModelPresentation);

        // Change zoom level to fit new active model
        Platform.runLater(() -> getActiveCanvasPresentation().getController().zoomHelper.zoomToFit());
    }

    public DoubleProperty getActiveCanvasZoomFactor() {
        return getActiveCanvasPresentation().getController().zoomHelper.currentZoomFactor;
    }

    private void initializeCanvasPane() {
        Platform.runLater(this::setCanvasModeToSingular);
    }

    private void initializeKeybindings() {
        // Keybinds for coloring the selected elements
        EnabledColor.enabledColors.forEach(enabledColor -> {
            KeyboardTracker.registerKeybind(KeyboardTracker.COLOR_SELECTED + "_" + enabledColor.keyCode.getName(), new Keybind(new KeyCodeCombination(enabledColor.keyCode), () -> {

                final List<Pair<SelectHelper.ItemSelectable, EnabledColor>> previousColor = new ArrayList<>();

                SelectHelper.getSelectedElements().forEach(selectable -> {
                    previousColor.add(new Pair<>(selectable, selectable.getColor()));
                });
                changeColorOnSelectedElements(enabledColor, previousColor);
                SelectHelper.clearSelectedElements();
            }));
        });

        // Keybind for deleting the selected elements
        KeyboardTracker.registerKeybind(KeyboardTracker.DELETE_SELECTED, new Keybind(new KeyCodeCombination(KeyCode.DELETE), this::deleteSelectedClicked));
    }

    /**
     * Initializes edge status.
     * Input is the default status.
     * This method sets buttons for edge status whenever the status changes.
     */
    private void initializeEdgeStatusHandling() {
        globalEdgeStatus.set(EdgeStatus.INPUT);

        Tooltip.install(switchToInputButton, new Tooltip("Switch to input mode"));
        Tooltip.install(switchToOutputButton, new Tooltip("Switch to output mode"));
        switchToInputButton.setDisableVisualFocus(true); // Hiding input button rippler on start-up

        globalEdgeStatus.addListener(((observable, oldValue, newValue) -> {
            if (newValue.equals(EdgeStatus.INPUT)) {
                switchToInputButton.setTextFill(javafx.scene.paint.Color.WHITE);
                switchToOutputButton.setTextFill(javafx.scene.paint.Color.GREY);
                switchEdgeStatusButton.setSelected(false);
            } else {
                switchToInputButton.setTextFill(javafx.scene.paint.Color.GREY);
                switchToOutputButton.setTextFill(javafx.scene.paint.Color.WHITE);
                switchEdgeStatusButton.setSelected(true);
            }
        }));

        // Ensure that the rippler is centered when scale is changed
        Platform.runLater(() -> {
            var rippler = ((JFXRippler) switchEdgeStatusButton.lookup(".jfx-rippler"));
            if (rippler == null) return;
            rippler.setRipplerRecenter(true);
        });
    }

    /**
     * Sets the global edge status.
     *
     * @param status the status
     */
    private void setGlobalEdgeStatus(EdgeStatus status) {
        globalEdgeStatus.set(status);
    }

    void scaleEdgeStatusToggle(double size) {
        switchEdgeStatusButton.setScaleX(size / 13.0);
        switchEdgeStatusButton.setScaleY(size / 13.0);
    }

    /**
     * Handles the change of color on selected objects
     *
     * @param enabledColor  The new color for the selected objects
     * @param previousColor The old color of the selected objects
     */
    public void changeColorOnSelectedElements(final EnabledColor enabledColor,
                                              final List<Pair<SelectHelper.ItemSelectable, EnabledColor>> previousColor) {
        UndoRedoStack.pushAndPerform(() -> { // Perform
            SelectHelper.getSelectedElements()
                    .forEach(selectable -> selectable.color(enabledColor));
        }, () -> { // Undo
            previousColor.forEach(selectableEnabledColorPair -> selectableEnabledColorPair.getKey().color(selectableEnabledColorPair.getValue()));
        }, String.format("Changed the color of %d elements to %s", previousColor.size(), enabledColor.color.name()), "color-lens");
    }

    /**
     * Removes the canvases and adds a new one, with the active component of the active canvasPresentation.
     */
    void setCanvasModeToSingular() {
        canvasPane.getChildren().clear();
        CanvasPresentation canvasPresentation = new CanvasPresentation();
        if (activeCanvasPresentation.get().getController().getActiveModelPresentation() != null) {
            canvasPresentation.getController().setActiveModelPresentation(activeCanvasPresentation.get().getController().getActiveModelPresentation());
        }

        canvasPane.getChildren().add(canvasPresentation);
        setActiveCanvasPresentation(canvasPresentation);

        Rectangle clip = new Rectangle();
        clip.setArcWidth(1);
        clip.setArcHeight(1);
        clip.widthProperty().bind(canvasPane.widthProperty());
        clip.heightProperty().bind(canvasPane.heightProperty());
        canvasPresentation.getController().zoomablePane.setClip(clip);

        canvasPresentation.getController().zoomablePane.minWidthProperty().bind(canvasPane.widthProperty());
        canvasPresentation.getController().zoomablePane.maxWidthProperty().bind(canvasPane.widthProperty());
        canvasPresentation.getController().zoomablePane.minHeightProperty().bind(canvasPane.heightProperty());
        canvasPresentation.getController().zoomablePane.maxHeightProperty().bind(canvasPane.heightProperty());

        Platform.runLater(() -> projectPane.getController().setHighlightedForModelFiles(Collections.singletonList(getActiveCanvasPresentation().getController().getActiveModelPresentation())));
    }

    /**
     * Removes the canvas and adds a GridPane with four new canvases, with different active components,
     * the first being the one previously displayed on the single canvas.
     */
    void setCanvasModeToSplit() {
        canvasPane.getChildren().clear();

        GridPane canvasGrid = new GridPane();

        canvasGrid.addColumn(0);
        canvasGrid.addColumn(1);
        canvasGrid.addRow(0);
        canvasGrid.addRow(1);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);

        RowConstraints row1 = new RowConstraints();
        row1.setPercentHeight(50);

        canvasGrid.getColumnConstraints().add(col1);
        canvasGrid.getColumnConstraints().add(col1);
        canvasGrid.getRowConstraints().add(row1);
        canvasGrid.getRowConstraints().add(row1);

        ObservableList<ComponentPresentation> components = projectPane.getController().getComponentPresentations();
        int currentCompNum = 0, numComponents = components.size();

        // Add the canvasPresentation at the top-left
        CanvasPresentation canvasPresentation = initializeNewCanvasPresentation();
        canvasPresentation.getController().setActiveModelPresentation(getActiveCanvasPresentation().getController().getActiveModelPresentation());
        canvasGrid.add(canvasPresentation, 0, 0);
        setActiveCanvasPresentation(canvasPresentation);

        // Add the canvasPresentation at the top-right
        canvasPresentation = initializeNewCanvasPresentationWithActiveComponent(components, currentCompNum);
        canvasPresentation.setOpacity(0.75);
        canvasGrid.add(canvasPresentation, 1, 0);

        // Update the startIndex for the next canvasPresentation
        for (int i = 0; i < numComponents; i++) {
            if (canvasPresentation.getController().getActiveModelPresentation() != null && canvasPresentation.getController().getActiveModelPresentation().getController().getModel().equals(components.get(i))) {
                currentCompNum = i + 1;
            }
        }

        // Add the canvasPresentation at the bottom-left
        canvasPresentation = initializeNewCanvasPresentationWithActiveComponent(components, currentCompNum);
        canvasPresentation.setOpacity(0.75);
        canvasGrid.add(canvasPresentation, 0, 1);

        // Update the startIndex for the next canvasPresentation
        for (int i = 0; i < numComponents; i++)
            if (canvasPresentation.getController().getActiveModelPresentation() != null && canvasPresentation.getController().getActiveModelPresentation().getController().getModel().equals(components.get(i))) {
                currentCompNum = i + 1;
            }

        // Add the canvasPresentation at the bottom-right
        canvasPresentation = initializeNewCanvasPresentationWithActiveComponent(components, currentCompNum);
        canvasPresentation.setOpacity(0.75);
        canvasGrid.add(canvasPresentation, 1, 1);

        canvasPane.getChildren().add(canvasGrid);
    }

    /**
     * Initialize a new CanvasShellPresentation and set its active component to the next component encountered from the startIndex and return it
     *
     * @param componentPresentations the list of componentPresentations for assigning active component of the CanvasPresentation
     * @param startIndex the index to start at when trying to find the component to set as active
     * @return new CanvasShellPresentation
     */
    private CanvasPresentation initializeNewCanvasPresentationWithActiveComponent(ObservableList<ComponentPresentation> componentPresentations, int startIndex) {
        CanvasPresentation canvasPresentation = initializeNewCanvasPresentation();

        int numComponents = componentPresentations.size();
        canvasPresentation.getController().setActiveModelPresentation(null);
        for (int currentCompNum = startIndex; currentCompNum < numComponents; currentCompNum++) {
            if (getActiveCanvasPresentation().getController().getActiveModelPresentation() != componentPresentations.get(currentCompNum)) {
                canvasPresentation.getController().setActiveModelPresentation(componentPresentations.get(currentCompNum));
                break;
            }
        }

        return canvasPresentation;
    }

    /**
     * Initialize a new CanvasPresentation and return it
     *
     * @return new CanvasPresentation
     */
    private CanvasPresentation initializeNewCanvasPresentation() {
        CanvasPresentation canvasPresentation = new CanvasPresentation();
        canvasPresentation.setBorder(new Border(new BorderStroke(Color.GREY.getColor(Color.Intensity.I500), BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderStroke.THIN)));

        // Set th clip of the zoomable pane to be half of the canvasPane,
        // to ensure a 2 by 2 grid without overflowing borders
        Rectangle clip = new Rectangle();
        clip.setArcWidth(1);
        clip.setArcHeight(1);
        clip.widthProperty().bind(canvasPane.widthProperty().divide(2));
        clip.heightProperty().bind(canvasPane.heightProperty().divide(2));
        canvasPresentation.getController().zoomablePane.setClip(clip);

        canvasPresentation.getController().zoomablePane.minWidthProperty().bind(canvasPane.widthProperty().divide(2));
        canvasPresentation.getController().zoomablePane.maxWidthProperty().bind(canvasPane.widthProperty().divide(2));
        canvasPresentation.getController().zoomablePane.minHeightProperty().bind(canvasPane.heightProperty().divide(2));
        canvasPresentation.getController().zoomablePane.maxHeightProperty().bind(canvasPane.heightProperty().divide(2));

        return canvasPresentation;
    }

    @FXML
    private void undoClicked() {
        UndoRedoStack.undo();
    }

    @FXML
    private void redoClicked() {
        UndoRedoStack.redo();
    }

    /**
     * Switch to input edge mode
     */
    @FXML
    private void switchToInputClicked() {
        setGlobalEdgeStatus(EdgeStatus.INPUT);
    }

    /**
     * Switch to output edge mode
     */
    @FXML
    private void switchToOutputClicked() {
        setGlobalEdgeStatus(EdgeStatus.OUTPUT);
    }

    /**
     * Switch edge status.
     */
    @FXML
    private void switchEdgeStatusClicked() {
        if (getGlobalEdgeStatus().equals(EdgeStatus.INPUT)) {
            setGlobalEdgeStatus(EdgeStatus.OUTPUT);
        } else {
            setGlobalEdgeStatus(EdgeStatus.INPUT);
        }
    }

    @FXML
    private void deleteSelectedClicked() {
        if (SelectHelper.getSelectedElements().size() == 0) return;

        // Run through the selected elements and look for something that we can delete
        SelectHelper.getSelectedElements().forEach(selectable -> {
            if (selectable instanceof LocationController) {
                ((LocationController) selectable).tryDelete();
            } else if (selectable instanceof EdgeController) {
                final Component component = ((EdgeController) selectable).getComponent();
                final DisplayableEdge edge = ((EdgeController) selectable).getEdge();

                // Dont delete edge if it is locked
                if (edge.getIsLockedProperty().getValue()) {
                    return;
                }

                UndoRedoStack.pushAndPerform(() -> { // Perform
                    // Remove the edge
                    component.removeEdge(edge);
                }, () -> { // Undo
                    // Re-all the edge
                    component.addEdge(edge);
                }, String.format("Deleted %s", selectable.toString()), "delete");
            } else if (selectable instanceof NailController) {
                ((NailController) selectable).tryDelete();
            }
        });

        SelectHelper.clearSelectedElements();
    }

    @Override
    public StackPane getLeftPane() {
        return projectPane;
    }

    @Override
    public StackPane getRightPane() {
        return queryPane;
    }
}
