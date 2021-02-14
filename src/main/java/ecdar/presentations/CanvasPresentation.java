package ecdar.presentations;

import ecdar.controllers.CanvasController;
import ecdar.controllers.EcdarController;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.helpers.CanvasDragHelper;
import ecdar.utility.helpers.MouseTrackable;
import ecdar.utility.keyboard.Keybind;
import ecdar.utility.keyboard.KeyboardTracker;
import ecdar.utility.mouse.MouseTracker;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

public class CanvasPresentation extends Pane implements MouseTrackable {
    public static MouseTracker mouseTracker;

    private final DoubleProperty x = new SimpleDoubleProperty(0);
    private final DoubleProperty y = new SimpleDoubleProperty(0);

    // This is the value of the user option for grid on/off.
    // This is not whether the grid is actual visible or not.
    // E.g. the grid is hidden while taking snapshots.
    // But that does not affect the gridUiOn field.
    private final BooleanProperty gridUiOn = new SimpleBooleanProperty(false);

    private final Grid grid = new Grid();
    private final CanvasController controller;

    public CanvasPresentation() {
        mouseTracker = new MouseTracker(this);
        controller = new EcdarFXMLLoader().loadAndGetController("CanvasPresentation.fxml", this);

        initializeGrid();

        CanvasDragHelper.makeDraggable(this, mouseEvent -> mouseEvent.getButton().equals(MouseButton.SECONDARY));

        controller.zoomHelper.setGrid(this.grid);
        controller.zoomHelper.setCanvas(this);
    }

    private void initializeGrid() {
        getChildren().add(grid);
        grid.toBack();
        gridUiOn.setValue(true);

        controller.allowGridProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && gridUiOn.get()) showGrid();
            else hideGrid();
        });

        //When the translation coordinates are changed, make sure that it is handled for the grid as well, to ensure that the grid is still centered on screen
        this.translateXProperty().addListener(((observable, oldValue, newValue) -> grid.handleTranslateX(oldValue.doubleValue(), newValue.doubleValue(), this.scaleXProperty().doubleValue())));
        this.translateYProperty().addListener(((observable, oldValue, newValue) -> grid.handleTranslateY(oldValue.doubleValue(), newValue.doubleValue(), this.scaleYProperty().doubleValue())));
    }

    /**
     * Toggles the user option for whether or not to show the grid on components and system views.
     * @return a Boolean property that is true if the grid has been turned on and false if the grid has been turned off
     */
    public BooleanProperty toggleGridUi() {
        if (gridUiOn.get()) {
            if (controller.isGridAllowed()) hideGrid();

            gridUiOn.setValue(false);
        } else {
            showGrid();
            gridUiOn.setValue(true);
        }
        return gridUiOn;
    }

    /**
     * Shows the grid.
     */
    private void showGrid() {
        grid.setOpacity(1);
    }

    /**
     * Hides the grid.
     */
    private void hideGrid() {
        grid.setOpacity(0);
    }

    /**
     * Updates if views should show an inset behind the error view.
     * @param shouldShow true iff views should show an inset
     */
    public static void showBottomInset(final Boolean shouldShow) {
        EcdarController.activeCanvasPresentation.getController().updateOffset(shouldShow);
    }

    @Override
    public DoubleProperty xProperty() {
        return x;
    }

    @Override
    public DoubleProperty yProperty() {
        return y;
    }

    @Override
    public double getX() {
        return xProperty().get();
    }

    @Override
    public double getY() {
        return yProperty().get();
    }

    @Override
    public MouseTracker getMouseTracker() {
        return mouseTracker;
    }

    public CanvasController getController() {
        return controller;
    }
}
