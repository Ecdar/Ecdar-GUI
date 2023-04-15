package ecdar.presentations;

import ecdar.abstractions.EcdarSystem;
import ecdar.abstractions.SystemRoot;
import ecdar.controllers.SystemRootController;
import ecdar.utility.Highlightable;
import ecdar.utility.colors.Color;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.helpers.ItemDragHelper;
import ecdar.utility.helpers.SelectHelper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

/**
 * Presentation for a system root
 */
public class SystemRootPresentation extends StackPane implements Highlightable {
    private final SystemRootController controller;
    private final EnabledColor highlightColor = new EnabledColor(SelectHelper.SELECT_COLOR, SelectHelper.SELECT_COLOR_INTENSITY_NORMAL);

    public SystemRootPresentation(final EcdarSystem system) {
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

        setLayoutY(ModelPresentation.TOOLBAR_HEIGHT);

        controller.shape.getPoints().addAll(0d, 0d);
        controller.shape.getPoints().addAll(20d, 15.);
        controller.shape.getPoints().addAll(40d, 15.);
        controller.shape.getPoints().addAll(60d, 0d);
    }

    /**
     * Initializes the mouse controls.
     * This includes handling of highlighting and making this draggable.
     * The polygon is highlighted while mouse is entered or being pressed on.
     */
    private void initializeMouseControls() {
        final BooleanProperty isEntered = new SimpleBooleanProperty(false);
        final BooleanProperty isPressed = new SimpleBooleanProperty(false);

        addEventHandler(MouseEvent.MOUSE_ENTERED, (event) -> {
            isEntered.set(true);
            event.consume();
            highlight();
        });

        addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            isEntered.set(false);
            // If mouse is also released, unhighlight
            if(!isPressed.get()) {
                event.consume();
                unhighlight();
            }
        });

        addEventHandler(MouseEvent.MOUSE_PRESSED, (event) -> {
            isPressed.set(true);
            event.consume();
            highlight();
        });

        addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            isPressed.set(false);
            // If mouse is also exited, unhighlight
            if(!isEntered.get()) {
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
        dye(controller.getSystem().getColor());
    }

    /**
     * Dyes the polygon.
     * @param color the color to dye with
     */
    private void dye(final EnabledColor color) {
        controller.shape.setFill(color.getPaintColor());
    }

    /**
     * Gets the drag bounds.
     * The drags bounds expand horizontally to enable such dragging.
     * The drag bounds does not expand vertically to disable such dragging.
     * @return the drag bounds
     */
    private ItemDragHelper.DragBounds getDragBounds() {
        final ObservableDoubleValue minX = new SimpleDoubleProperty(20d);
        final ObservableDoubleValue maxX = controller.getSystem().getBox().getWidthProperty()
                .subtract(80d);
        final ObservableDoubleValue y = new SimpleDoubleProperty(20);

        return new ItemDragHelper.DragBounds(minX, maxX, y, y);
    }

    @Override
    public void highlight() {
        dye(highlightColor);
    }

    @Override
    public void unhighlight() {
        dyeFromSystemColor();
    }

    @Override
    public void highlightPurple() {
        dye(new EnabledColor(Color.DEEP_PURPLE, SelectHelper.SELECT_COLOR_INTENSITY_NORMAL));
    }
}
