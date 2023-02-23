package ecdar.controllers;

import ecdar.Ecdar;
import ecdar.abstractions.Box;
import ecdar.abstractions.HighLevelModel;
import com.jfoenix.controls.JFXTextField;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.helpers.StringValidator;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

import static ecdar.presentations.ModelPresentation.CORNER_SIZE;

/**
 * Controller for a high level model, such as a component or a system.
 */
public abstract class ModelController extends HighLevelModelController {
    public StackPane root;
    public Rectangle background;
    public BorderPane frame;
    public Rectangle cornerAnchor;
    public Rectangle rightAnchor;
    public Rectangle bottomAnchor;
    public Line topLeftLine;
    public BorderPane toolbar;
    public JFXTextField name;

    /**
     * Hides the border and background.
     */
    void hideBorderAndBackground() {
        frame.setVisible(false);
        topLeftLine.setVisible(false);
        background.setVisible(false);
    }

    /**
     * Shows the border and background.
     */
    void showBorderAndBackground() {
        frame.setVisible(true);
        topLeftLine.setVisible(true);
        background.setVisible(true);
    }

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
        final HighLevelModel model = getModel();

        final BooleanProperty initialized = new SimpleBooleanProperty(false);

        name.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && !initialized.get()) {
                root.requestFocus();
                initialized.setValue(true);
            }
        });

        // Set the text field to the name in the model, and bind the model to the text field
        name.setText(model.getName());
        name.textProperty().addListener((obs, oldName, newName) -> {
            if (StringValidator.validateComponentName(newName)) {
                model.nameProperty().unbind();
                model.setName(newName);
            } else {
                name.setText(model.getName());
                Ecdar.showToast("Component names cannot contain '.'");
            }
        });

        final Runnable updateColor = () -> {
            final EnabledColor color = model.getColor();

            // Set the text color for the label
            name.setStyle("-fx-text-fill: " + color.color.getTextColorRgbaString(color.intensity) + ";");
            name.setFocusColor(color.getTextColor());
            name.setUnFocusColor(javafx.scene.paint.Color.TRANSPARENT);
        };

        model.colorProperty().addListener(observable -> updateColor.run());
        updateColor.run();

        // Center the text vertically and aff a left padding of CORNER_SIZE
        name.setPadding(new Insets(2, 0, 0, CORNER_SIZE));
        name.setOnKeyPressed(EcdarController.getActiveCanvasPresentation().getController().getLeaveTextAreaKeyHandler());
    }

    /**
     * Sets the width and the height of the view to the values in the abstraction.
     * @param box The dimensions to set
     */
    void initializeDimensions(final Box box) {
        // Ensure that the component snaps to the grid
        if (box.getX() == 0 && box.getY() == 0) {
            box.setX(Ecdar.CANVAS_PADDING * 0.5);
            box.setY(Ecdar.CANVAS_PADDING * 0.5);
        }

        // Bind the position of the abstraction to the values in the view
        root.layoutXProperty().set(box.getX());
        root.layoutYProperty().set(box.getY());
        box.getXProperty().bindBidirectional(root.layoutXProperty());
        box.getYProperty().bindBidirectional(root.layoutYProperty());

        root.setMinWidth(box.getWidth());
        root.setMaxWidth(box.getWidth());
        root.setMinHeight(box.getHeight());
        root.setMaxHeight(box.getHeight());
        root.minHeightProperty().bindBidirectional(box.getHeightProperty());
        root.maxHeightProperty().bindBidirectional(box.getHeightProperty());
        root.minWidthProperty().bindBidirectional(box.getWidthProperty());
        root.maxWidthProperty().bindBidirectional(box.getWidthProperty());
    }

    /**
     * Initializes the right drag anchor.
     * @param box the box of the model
     */
    private void initializesRightDragAnchor(final Box box) {
        final BooleanProperty wasResized = new SimpleBooleanProperty(false);
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
            diff -= diff % Ecdar.CANVAS_PADDING;

            final double newWidth = prevWidth.get() + diff;
            final double minWidth = getDragAnchorMinWidth();

            // Move the model left or right to account for new height (needed because model is centered in parent)
            root.setTranslateX(root.getTranslateX() + (Math.max(newWidth, minWidth) - box.getWidth()) / 2);
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
            final double newHeight = prevHeight.get() + diff;
            final double minHeight = getDragAnchorMinHeight();

            // Move the model up or down to account for new height (needed because model is centered in parent)
            root.setTranslateY(root.getTranslateY() + (Math.max(newHeight, minHeight) - box.getHeight()) / 2);
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
            double xDiff = (event.getScreenX() - prevX.get()) / EcdarController.getActiveCanvasZoomFactor().get(); // ToDo NIELS: Fix dependency
            final double newWidth = Math.max(prevWidth.get() + xDiff, getDragAnchorMinWidth());
            box.setWidth(newWidth);

            double yDiff = (event.getScreenY() - prevY.get()) / EcdarController.getActiveCanvasZoomFactor().get();
            final double newHeight = Math.max(prevHeight.get() + yDiff, getDragAnchorMinHeight());
            box.setHeight(newHeight);

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
