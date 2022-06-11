package ecdar.presentations;

import ecdar.abstractions.Box;
import ecdar.abstractions.HighLevelModelObject;
import ecdar.controllers.EcdarController;
import ecdar.controllers.ModelController;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import static ecdar.presentations.Grid.GRID_SIZE;

/**
 * Presentation for high level graphical models such as systems and components
 */
public abstract class ModelPresentation extends HighLevelModelPresentation {
    static final Polygon TOP_LEFT_CORNER = new Polygon(
            0, 0,
            Grid.CORNER_SIZE + 2, 0,
            0, Grid.CORNER_SIZE + 2
    );

    abstract ModelController getModelController();
    abstract double getDragAnchorMinWidth();
    abstract double getDragAnchorMinHeight();

    /**
     * Initializes this.
     * @param box the box of the model
     */
    void initialize(final Box box) {
        initializeName();
        initializeDimensions(box);
        initializesBottomDragAnchor(box);
        initializesRightDragAnchor(box);
        initializesCornerDragAnchor(box);
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
        controller.name.setPadding(new Insets(2, 0, 0, Grid.CORNER_SIZE));
        controller.name.setOnKeyPressed(EcdarController.getActiveCanvasPresentation().getController().getLeaveTextAreaKeyHandler());
    }

    /**
     * Sets the width and the height of the view to the values in the abstraction.
     * @param box The dimensions to set
     */
    void initializeDimensions(final Box box) {
        // Ensure that the component snaps to the grid
        if (box.getX() == 0 && box.getY() == 0) {
            box.setX(GRID_SIZE * 0.5);
            box.setY(GRID_SIZE * 0.5);
        }

        // Bind the position of the abstraction to the values in the view
        layoutXProperty().set(box.getX());
        layoutYProperty().set(box.getY());
        box.getXProperty().bindBidirectional(layoutXProperty());
        box.getYProperty().bindBidirectional(layoutYProperty());

        setMinWidth(box.getWidth());
        setMaxWidth(box.getWidth());
        setMinHeight(box.getHeight());
        setMaxHeight(box.getHeight());
        minHeightProperty().bindBidirectional(box.getHeightProperty());
        maxHeightProperty().bindBidirectional(box.getHeightProperty());
        minWidthProperty().bindBidirectional(box.getWidthProperty());
        maxWidthProperty().bindBidirectional(box.getWidthProperty());
    }

    /**
     * Initializes the right drag anchor.
     * @param box the box of the model
     */
    private void initializesRightDragAnchor(final Box box) {
        final BooleanProperty wasResized = new SimpleBooleanProperty(false);

        // Right anchor
        final Rectangle rightAnchor = getModelController().rightAnchor;

        rightAnchor.setCursor(Cursor.E_RESIZE);

        // Bind the place and size of bottom anchor
        rightAnchor.setWidth(5);
        rightAnchor.heightProperty().bind(box.getHeightProperty());

        final DoubleProperty prevX = new SimpleDoubleProperty();
        final DoubleProperty prevWidth = new SimpleDoubleProperty();

        rightAnchor.setOnMousePressed(event -> {
            event.consume();

            prevX.set(event.getScreenX());
            prevWidth.set(box.getWidth());
        });

        rightAnchor.setOnMouseDragged(event -> {
            double diff = event.getScreenX() - prevX.get();
            diff -= diff % Grid.GRID_SIZE;

            final double newWidth = prevWidth.get() + diff;
            final double minWidth = getDragAnchorMinWidth();

            // Move the model left or right to account for new height (needed because model is centered in parent)
            setTranslateX(getTranslateX() + (Math.max(newWidth, minWidth) - box.getWidth()) / 2);
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

    /**
     * Initializes the bottom drag anchor.
     * @param box the box of the model
     */
    private void initializesBottomDragAnchor(final Box box) {
        final BooleanProperty wasResized = new SimpleBooleanProperty(false);
        // Bottom anchor
        final Rectangle bottomAnchor = getModelController().bottomAnchor;

        bottomAnchor.setCursor(Cursor.S_RESIZE);

        // Bind the place and size of bottom anchor
        bottomAnchor.widthProperty().bind(box.getWidthProperty());
        bottomAnchor.setHeight(5);

        final DoubleProperty prevY = new SimpleDoubleProperty();
        final DoubleProperty prevHeight = new SimpleDoubleProperty();

        bottomAnchor.setOnMousePressed(event -> {
            event.consume();
            
            prevY.set(event.getScreenY());
            prevHeight.set(box.getHeight());
        });

        bottomAnchor.setOnMouseDragged(event -> {
            double diff = event.getScreenY() - prevY.get();
            diff -= diff % Grid.GRID_SIZE;

            final double newHeight = prevHeight.get() + diff;
            final double minHeight = getDragAnchorMinHeight();

            // Move the model up or down to account for new height (needed because model is centered in parent)
            setTranslateY(getTranslateY() + (Math.max(newHeight, minHeight) - box.getHeight()) / 2);
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
    }

    private void initializesCornerDragAnchor(final Box box) {
        final BooleanProperty wasResized = new SimpleBooleanProperty(false);

        final Rectangle cornerAnchor = getModelController().cornerAnchor;
        cornerAnchor.setCursor(Cursor.SE_RESIZE);

        // Bind the place and size of bottom anchor
        cornerAnchor.setWidth(10);
        cornerAnchor.setHeight(10);

        final DoubleProperty prevX = new SimpleDoubleProperty();
        final DoubleProperty prevY = new SimpleDoubleProperty();
        final DoubleProperty prevWidth = new SimpleDoubleProperty();
        final DoubleProperty prevHeight = new SimpleDoubleProperty();

        cornerAnchor.setOnMousePressed(event -> {
            event.consume();

            prevX.set(event.getScreenX());
            prevWidth.set(box.getWidth());
            prevY.set(event.getScreenY());
            prevHeight.set(box.getHeight());
        });

        cornerAnchor.setOnMouseDragged(event -> {
            double xDiff = event.getScreenX() - prevX.get();
            xDiff -= xDiff % Grid.GRID_SIZE;

            final double newWidth = prevWidth.get() + xDiff;
            final double minWidth = getDragAnchorMinWidth();
            box.setWidth(Math.max(newWidth, minWidth));

            double yDiff = event.getScreenY() - prevY.get();
            yDiff -= yDiff % Grid.GRID_SIZE;

            final double newHeight = prevHeight.get() + yDiff;
            final double minHeight = getDragAnchorMinHeight();
            box.setHeight(Math.max(newHeight, minHeight));

            wasResized.set(true);
        });

        cornerAnchor.setOnMouseReleased(event -> {
            if (!wasResized.get()) return;
            final double previousWidth = prevWidth.doubleValue();
            final double currentWidth = box.getWidth();
            final double previousHeight = prevHeight.doubleValue();
            final double currentHeight = box.getHeight();

            // If no difference do not save change
            if (previousWidth == currentWidth && previousHeight == currentHeight) return;

            UndoRedoStack.pushAndPerform(() -> { // Perform
                        box.setWidth(currentWidth);
                        box.setHeight(currentHeight);
                    }, () -> { // Undo
                        box.setWidth(previousWidth);
                        box.setHeight(previousHeight);
                    },
                    "Component resized",
                    "settings-overscan"
            );

            wasResized.set(false);
        });
    }
}
