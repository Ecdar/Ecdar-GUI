package SW9.presentations;

import SW9.abstractions.Box;
import SW9.abstractions.Component;
import SW9.abstractions.HighLevelModelObject;
import SW9.controllers.CanvasController;
import SW9.controllers.ModelController;
import SW9.utility.UndoRedoStack;
import SW9.utility.colors.Color;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import java.util.function.Supplier;

import static SW9.presentations.Grid.GRID_SIZE;

/**
 *
 */
public abstract class ModelPresentation extends HighLevelModelPresentation {
    static final double CORNER_SIZE = 4 * GRID_SIZE;
    public static final double TOOL_BAR_HEIGHT = CORNER_SIZE / 2;

    static final Polygon TOP_LEFT_CORNER = new Polygon(
            0, 0,
            CORNER_SIZE + 2, 0,
            0, CORNER_SIZE + 2
    );

    abstract ModelController getModelController();

    /**
     * Initializes this.
     */
    void initialize() {
        initializeName();
    }

    /**
     * Initializes handling of name.
     */
    private void initializeName() {
        final ModelController controller = getModelController();
        final HighLevelModelObject model = controller.getModel();

        final BooleanProperty initialized = new SimpleBooleanProperty(false);

        controller.name.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && !initialized.get()) {
                controller.root.requestFocus();
                initialized.setValue(true);
            }
        });

        // Set the text field to the name in the model, and bind the model to the text field
        controller.name.setText(model.getName());
        controller.name.textProperty().addListener((obs, oldName, newName) -> {
            model.nameProperty().unbind();
            model.setName(newName);
        });

        final Runnable updateColor = () -> {
            final Color color = model.getColor();
            final Color.Intensity colorIntensity = model.getColorIntensity();

            // Set the text color for the label
            controller.name.setStyle("-fx-text-fill: " + color.getTextColorRgbaString(colorIntensity) + ";");
            controller.name.setFocusColor(color.getTextColor(colorIntensity));
            controller.name.setUnFocusColor(javafx.scene.paint.Color.TRANSPARENT);
        };

        model.colorProperty().addListener(observable -> updateColor.run());
        updateColor.run();

        // Center the text vertically and aff a left padding of CORNER_SIZE
        controller.name.setPadding(new Insets(2, 0, 0, CORNER_SIZE));
        controller.name.setOnKeyPressed(CanvasController.getLeaveTextAreaKeyHandler());
    }

    /**
     * Sets the width and the height of the view to the values in the abstraction.
     * @param box The dimensions to set
     */
    void initializeDimensions(final Box box) {
        setMinWidth(box.getWidth());
        setMaxWidth(box.getWidth());
        setMinHeight(box.getHeight());
        setMaxHeight(box.getHeight());
        minHeightProperty().bindBidirectional(box.heightProperty());
        maxHeightProperty().bindBidirectional(box.heightProperty());
        minWidthProperty().bindBidirectional(box.widthProperty());
        maxWidthProperty().bindBidirectional(box.widthProperty());
    }

    private void initializeDragAnchors(final Box box, final Supplier<Double> minHeightSupplier, final Supplier<Double> minWidthSupplier) {
        final BooleanProperty wasResized = new SimpleBooleanProperty(false);

        // Bottom anchor
        final Rectangle bottomAnchor = getModelController().bottomAnchor;

        bottomAnchor.setCursor(Cursor.S_RESIZE);

        // Bind the place and size of bottom anchor
        bottomAnchor.widthProperty().bind(box.widthProperty().subtract(CORNER_SIZE));
        bottomAnchor.setHeight(5);

        final DoubleProperty prevY = new SimpleDoubleProperty();
        final DoubleProperty prevHeight = new SimpleDoubleProperty();

        bottomAnchor.setOnMousePressed(event -> {
            prevY.set(event.getScreenY());
            prevHeight.set(box.getHeight());
        });

        bottomAnchor.setOnMouseDragged(event -> {
            double diff = event.getScreenY() - prevY.get();
            diff -= diff % GRID_SIZE;

            final double newHeight = prevHeight.get() + diff;
            final double minHeight = minHeightSupplier.get();

            box.setHeight(Math.max(newHeight, minHeight));
            wasResized.set(true);
        });

        bottomAnchor.setOnMouseReleased(event -> {
            if (!wasResized.get()) return;
            final double previousHeight = prevHeight.doubleValue();
            final double currentHeight = box.getHeight();

            // If no difference do not save change
            if (previousHeight == currentHeight) return;

            UndoRedoStack.pushAndPerform(() -> { // Perform
                        box.setHeight(currentHeight);
                    }, () -> { // Undo
                        box.setHeight(previousHeight);
                    },
                    "Component height resized",
                    "settings-overscan"
            );

            wasResized.set(false);
        });

        // Right anchor
        final Rectangle rightAnchor = getModelController().rightAnchor;

        rightAnchor.setCursor(Cursor.E_RESIZE);

        // Bind the place and size of bottom anchor
        rightAnchor.setWidth(5);
        rightAnchor.heightProperty().bind(box.heightProperty().subtract(CORNER_SIZE));

        final DoubleProperty prevX = new SimpleDoubleProperty();
        final DoubleProperty prevWidth = new SimpleDoubleProperty();

        rightAnchor.setOnMousePressed(event -> {
            prevX.set(event.getScreenX());
            prevWidth.set(box.getWidth());
        });

        rightAnchor.setOnMouseDragged(event -> {
            double diff = event.getScreenX() - prevX.get();
            diff -= diff % GRID_SIZE;

            final double newWidth = prevWidth.get() + diff;
            final double minWidth = minWidthSupplier.get();
            box.setWidth(Math.max(newWidth, minWidth));
            wasResized.set(true);
        });

        rightAnchor.setOnMouseReleased(event -> {
            if (!wasResized.get()) return;
            final double previousWidth = prevWidth.doubleValue();
            final double currentWidth = box.getWidth();

            // If no difference do not save change
            if (previousWidth == currentWidth) return;

            UndoRedoStack.pushAndPerform(() -> { // Perform
                        box.setWidth(currentWidth);
                    }, () -> { // Undo
                        box.setWidth(previousWidth);
                    },
                    "Component width resized",
                    "settings-overscan"
            );

            wasResized.set(false);
        });


    }
}
