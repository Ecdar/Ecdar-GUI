package SW9.presentations;

import SW9.abstractions.SystemModel;
import SW9.abstractions.SystemRoot;
import SW9.controllers.SystemRootController;
import SW9.utility.Highlightable;
import SW9.utility.colors.Color;
import SW9.utility.helpers.ItemDragHelper;
import SW9.utility.helpers.SelectHelper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Polygon;

/**
 * Presentation for a system root
 */
public class SystemRootPresentation extends Polygon implements Highlightable {
    private final SystemRootController controller;

    public SystemRootPresentation(final SystemModel system) {
        controller = new EcdarFXMLLoader().loadAndGetController("SystemRootPresentation.fxml", this);
        controller.setSystemRoot(system.getSystemRoot());
        controller.setSystem(system);

        initializeDimensions();
        initializeMouseControls();
        initializeColor();
    }

    /**
     * Initializes the dimensions.
     */
    private void initializeDimensions() {
        final SystemRoot root = controller.getSystemRoot();

        // Bind x and y
        setLayoutX(root.getX());
        root.getXProperty().bind(layoutXProperty());

        setLayoutY(ComponentPresentation.TOOL_BAR_HEIGHT); // TODO move const

        controller.root.getPoints().addAll(- 3d * Grid.GRID_SIZE, 0d);
        controller.root.getPoints().addAll(- 1d * Grid.GRID_SIZE, 2d * Grid.GRID_SIZE);
        controller.root.getPoints().addAll(1d * Grid.GRID_SIZE, 2d * Grid.GRID_SIZE);
        controller.root.getPoints().addAll(3d * Grid.GRID_SIZE, 0d);
    }

    /**
     * Initializes the mouse controls.
     * This includes handling of selection and making this draggable.
     */
    private void initializeMouseControls() {
        addEventHandler(MouseEvent.MOUSE_ENTERED, (event) -> {
            event.consume();
            highlight();
        });

        addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            // Button is down when dragging, and we want it to stay highlighted even if we exit on drag
            if(!event.isPrimaryButtonDown()) {
                event.consume();
                unhighlight();
            }
        });

        addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            if(!event.isStillSincePress()) { // Should not unhighlight if it's a simple press with no drag
                event.consume();
                unhighlight();
            }
        });

        ItemDragHelper.makeDraggable(this, this::getDragBounds);
    }

    /**
     * Initializes color.
     */
    private void initializeColor() {
        // Update color now and whenever color of system changes
        dyeFromSystemColor();
        controller.getSystem().colorProperty().addListener(observable -> dyeFromSystemColor());
    }

    /**
     * Dyes this based on the color of the system.
     * The color will be a bit darker than the color of the system.
     */
    private void dyeFromSystemColor() {
        dye(controller.getSystem().getColor(), controller.getSystem().getColorIntensity().next(2));
    }

    /**
     * Dyes the polygon.
     * @param color the color to dye with
     * @param intensity the intensity of the color to use
     */
    private void dye(final Color color, final Color.Intensity intensity) {
        setFill(color.getColor(intensity));
    }

    /**
     * Gets the drag bounds.
     * The drags bounds expand horizontally to enable such dragging.
     * The drag bounds does not expand vertically to disable such dragging.
     * @return the drag bounds
     */
    private ItemDragHelper.DragBounds getDragBounds() {
        final ObservableDoubleValue minX = new SimpleDoubleProperty(5d * Grid.GRID_SIZE);
        final ObservableDoubleValue maxX = controller.getSystem().getBox().getWidthProperty()
                .subtract(5d * Grid.GRID_SIZE);
        final ObservableDoubleValue y = new SimpleDoubleProperty(ComponentPresentation.TOOL_BAR_HEIGHT);

        return new ItemDragHelper.DragBounds(minX, maxX, y, y);
    }

    @Override
    public void highlight() {
        dye(SelectHelper.SELECT_COLOR, SelectHelper.SELECT_COLOR_INTENSITY_NORMAL);
    }

    @Override
    public void unhighlight() {
        dyeFromSystemColor();
    }
}
