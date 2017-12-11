package SW9.controllers;

import SW9.abstractions.*;
import SW9.code_analysis.Nearable;
import SW9.presentations.ComponentPresentation;
import SW9.utility.UndoRedoStack;
import SW9.utility.colors.Color;
import SW9.utility.helpers.ItemDragHelper;
import SW9.utility.helpers.SelectHelper;
import SW9.utility.keyboard.Keybind;
import SW9.utility.keyboard.KeyboardTracker;
import com.jfoenix.controls.JFXPopup;
import com.jfoenix.controls.JFXTextField;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import static SW9.presentations.Grid.GRID_SIZE;

/**
 * Controller for a component instance.
 */
public class ComponentInstanceController implements Initializable, SelectHelper.ItemSelectable {
    public BorderPane frame;
    public Line line1;
    public Rectangle background;
    public Label originalComponentLabel;
    public JFXTextField identifier;
    public StackPane root;
    public HBox toolbar;

    private ComponentInstance instance;
    private SystemModel system;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        initializeSelectListener();
        initializeMouseControls();
    }

    private void initializeSelectListener() {
        SelectHelper.elementsToBeSelected.addListener((ListChangeListener<Nearable>) change -> {
            while (change.next()) {
                if (change.getAddedSize() == 0) return;

                for (final Nearable nearable : SelectHelper.elementsToBeSelected) {
                    if (nearable instanceof ComponentInstance && nearable == getInstance()) {
                        SelectHelper.addToSelection(ComponentInstanceController.this);
                        break;
                    }
                }
            }
        });
    }

    private void initializeMouseControls() {
        root.addEventHandler(MouseEvent.MOUSE_PRESSED, (event) -> {
            event.consume();

            if (event.isShortcutDown()) {
                SelectHelper.addToSelection(this);
            } else {
                SelectHelper.select(this);
            }
        });

        ItemDragHelper.makeDraggable(root, this::getDragBounds);
    }

    public ComponentInstance getInstance() {
        return instance;
    }

    public void setInstance(final ComponentInstance instance) {
        this.instance = instance;
    }

    public void setSystem(final SystemModel system) {
        this.system = system;
    }

    public SystemModel getSystem() {
        return system;
    }

    @Override
    public void color(Color color, Color.Intensity intensity) {

    }

    @Override
    public Color getColor() {
        return null;
    }

    @Override
    public Color.Intensity getColorIntensity() {
        return null;
    }

    /**
     * Gets the bound that it is valid to drag the instance within.
     * @return the bounds
     */
    @Override
    public ItemDragHelper.DragBounds getDragBounds() {
        final ObservableDoubleValue minX = new SimpleDoubleProperty(GRID_SIZE * 2);
        final ObservableDoubleValue maxX = getSystem().getBox().getWidthProperty()
                .subtract(GRID_SIZE * 2)
                .subtract(getInstance().getBox().getWidth());
        final ObservableDoubleValue minY = new SimpleDoubleProperty(ComponentPresentation.TOOL_BAR_HEIGHT + GRID_SIZE * 2);
        final ObservableDoubleValue maxY = getSystem().getBox().getHeightProperty()
                .subtract(GRID_SIZE * 2)
                .subtract(getInstance().getBox().getHeight());
        return new ItemDragHelper.DragBounds(minX, maxX, minY, maxY);
    }

    @Override
    public DoubleProperty xProperty() {
        return root.layoutXProperty();
    }

    @Override
    public DoubleProperty yProperty() {
        return root.layoutYProperty();
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
    public void select() {
        root.sele
    }

    @Override
    public void deselect() {

    }
}
