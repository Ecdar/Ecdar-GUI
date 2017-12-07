package SW9.presentations;

import SW9.controllers.CanvasController;
import SW9.utility.UndoRedoStack;
import SW9.utility.helpers.CanvasDragHelper;
import SW9.utility.helpers.MouseTrackable;
import SW9.utility.keyboard.Keybind;
import SW9.utility.keyboard.KeyboardTracker;
import SW9.utility.mouse.MouseTracker;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;

import java.io.IOException;
import java.net.URL;

public class CanvasPresentation extends Pane implements MouseTrackable {
    public static MouseTracker mouseTracker;

    private final DoubleProperty x = new SimpleDoubleProperty(0);
    private final DoubleProperty y = new SimpleDoubleProperty(0);
    private final BooleanProperty gridOn = new SimpleBooleanProperty(false);
    private final Grid grid = new Grid(Grid.GRID_SIZE);
    private final CanvasController controller;

    public CanvasPresentation() {
        mouseTracker = new MouseTracker(this);

        KeyboardTracker.registerKeybind(KeyboardTracker.UNDO, new Keybind(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN), UndoRedoStack::undo));
        KeyboardTracker.registerKeybind(KeyboardTracker.REDO, new Keybind(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), UndoRedoStack::redo));

        controller = new EcdarFXMLLoader().loadAndGetController("CanvasPresentation.fxml", this);

        initializeGrid();

        CanvasDragHelper.makeDraggable(this, mouseEvent -> mouseEvent.getButton().equals(MouseButton.SECONDARY));
    }

    private void initializeGrid() {
        getChildren().add(grid);
        grid.toBack();
        gridOn.setValue(true);
    }

    /**
     * Toggles the grid on if the grid is off and vice versa
     * @return a Boolean property that is true if the grid has been turned on and false if the grid has been turned off
     */
    public BooleanProperty toggleGrid() {
        if (gridOn.get()) {
            grid.setOpacity(0);
            gridOn.setValue(false);
        } else {
            grid.setOpacity(1);
            gridOn.setValue(true);
        }
        return gridOn;
    }


    /**
     * Updates if views should show an inset behind the error view.
     * @param shouldShow true iff views should show an inset
     */
    public static void showBottomInset(final Boolean shouldShow) {
        CanvasController.updateOffset(shouldShow);
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

    public boolean isGridOn() {
        return gridOn.get();
    }

    public CanvasController getController() {
        return controller;
    }
}
