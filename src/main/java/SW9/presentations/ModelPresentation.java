package SW9.presentations;

import SW9.abstractions.Box;
import SW9.abstractions.Component;
import SW9.abstractions.HighLevelModelObject;
import SW9.controllers.CanvasController;
import SW9.controllers.ModelController;
import SW9.utility.colors.Color;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.shape.Polygon;

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

    void initialize() {
        initializeName();
    }

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
}
