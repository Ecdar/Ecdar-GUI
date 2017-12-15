package SW9.presentations;

import SW9.abstractions.ComponentOperator;
import SW9.abstractions.SystemModel;
import SW9.controllers.ComponentOperatorController;
import SW9.utility.colors.Color;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.util.function.BiConsumer;

/**
 * Presentation of a component operator
 */
public class ComponentOperatorPresentation extends StackPane {
    private final ComponentOperatorController controller;

    public ComponentOperatorPresentation(final ComponentOperator operator, final SystemModel system){
        controller = new EcdarFXMLLoader().loadAndGetController("ComponentOperatorPresentation.fxml", this);

        controller.setOperator(operator);
        controller.setSystem(system);

        initializeIdLabel();
        initializeDimensions();
        initializeFrame();
        initializeBackground();
    }

    private void initializeIdLabel() {
        final SystemModel system = controller.getSystem();
        final ComponentOperator operator = controller.getOperator();
        final Label idLabel = controller.label;

        final DropShadow ds = new DropShadow();
        ds.setRadius(2);
        ds.setSpread(1);

        idLabel.setEffect(ds);

        idLabel.textProperty().set(operator.getLabel());

        // Center align the label
        idLabel.widthProperty().addListener((obsWidth, oldWidth, newWidth) -> idLabel.translateXProperty().set(newWidth.doubleValue() / -2));
        idLabel.heightProperty().addListener((obsHeight, oldHeight, newHeight) -> idLabel.translateYProperty().set(newHeight.doubleValue() / -2));

        // Delegate to style the label based on the color of the location
        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            idLabel.setTextFill(newColor.getTextColor(newIntensity));
            ds.setColor(newColor.getColor(newIntensity));
        };

        // Update color now and on color change
        updateColor.accept(system.getColor(), system.getColorIntensity());
        system.colorProperty().addListener(observable -> updateColor.accept(system.getColor(), system.getColorIntensity()));
    }


    /**
     * Initializes the dimensions.
     */
    private void initializeDimensions() {
        final ComponentOperator instance = controller.getOperator();

        setMinWidth(30);
        setMaxWidth(30);
        setMinHeight(30);
        setMaxHeight(30);

        instance.getBox().getWidthProperty().bind(widthProperty());
        instance.getBox().getHeightProperty().bind(heightProperty());

        // Bind x and y
        setLayoutX(instance.getBox().getX());
        setLayoutY(instance.getBox().getY());
        instance.getBox().getXProperty().bind(layoutXProperty());
        instance.getBox().getYProperty().bind(layoutYProperty());
    }

    /**
     * Initializes the frame.
     */
    private void initializeFrame() {
        final SystemModel system = controller.getSystem();

        final Shape[] mask = new Shape[1];
        final Rectangle rectangle = new Rectangle(getMinWidth(), getMinHeight());

        // Generate four corners (to subtract)
        // Upper left
        final Polygon corner1 = new Polygon(
                0, 0,
                ModelPresentation.COMPONENT_CORNER_SIZE + 2, 0,
                0, ModelPresentation.COMPONENT_CORNER_SIZE + 2
        );

        // Upper right
        final Polygon corner2 = new Polygon(
                getMinWidth() - ModelPresentation.COMPONENT_CORNER_SIZE - 2, 0,
                getMinWidth(), 0,
                getMinWidth(), ModelPresentation.COMPONENT_CORNER_SIZE + 2
        );

        // Lower left
        final Polygon corner3 = new Polygon(
                0, getMinHeight() - ModelPresentation.COMPONENT_CORNER_SIZE - 2,
                0, getMinHeight(),
                ModelPresentation.COMPONENT_CORNER_SIZE + 2, getMinHeight()
        );

        //Lower right
        final Polygon corner4 = new Polygon(
                getMinWidth(), getMinHeight() - ModelPresentation.COMPONENT_CORNER_SIZE - 2,
                getMinWidth() - ModelPresentation.COMPONENT_CORNER_SIZE - 2, getMinHeight(),
                getMinWidth(), getMinHeight()
        );

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Mask the parent of the frame (will also mask the background)
            mask[0] = Path.subtract(rectangle, corner1);
            mask[0] = Path.subtract(mask[0], corner2);
            mask[0] = Path.subtract(mask[0], corner3);
            mask[0] = Path.subtract(mask[0], corner4);

            controller.frame.setClip(mask[0]);
            controller.background.setClip(Path.union(mask[0], mask[0]));
        };

        // Update color now and on color change
        updateColor.accept(system.getColor(), system.getColorIntensity());
        system.colorProperty().addListener(observable -> updateColor.accept(system.getColor(), system.getColorIntensity()));

    }

    /**
     * Initializes the background.
     */
    private void initializeBackground() {
        final SystemModel system = controller.getSystem();

        // Bind the background width and height to the values in the model
        controller.background.widthProperty().bind(minWidthProperty());
        controller.background.heightProperty().bind(minHeightProperty());

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Set the background color to the lightest possible version of the color
            controller.background.setFill(newColor.getColor(newIntensity));
        };

        // Update color now and on color change
        updateColor.accept(system.getColor(), system.getColorIntensity());
        system.colorProperty().addListener(observable -> updateColor.accept(system.getColor(), system.getColorIntensity()));

    }
}
