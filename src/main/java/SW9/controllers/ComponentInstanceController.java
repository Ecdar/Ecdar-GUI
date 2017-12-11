package SW9.controllers;

import SW9.abstractions.ComponentInstance;
import SW9.abstractions.SystemModel;
import SW9.presentations.ComponentPresentation;
import SW9.utility.helpers.ItemDragHelper;
import com.jfoenix.controls.JFXTextField;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ResourceBundle;

import static SW9.presentations.Grid.GRID_SIZE;

/**
 * Controller for a component instance.
 */
public class ComponentInstanceController implements Initializable {
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

    /**
     * Gets the bound that it is valid to drag the instance within.
     * @return the bounds
     */
    private ItemDragHelper.DragBounds getDragBounds() {
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
}
