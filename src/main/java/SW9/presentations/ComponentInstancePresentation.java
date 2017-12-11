package SW9.presentations;

import SW9.abstractions.Component;
import SW9.abstractions.ComponentInstance;
import SW9.abstractions.SystemModel;
import SW9.controllers.CanvasController;
import SW9.controllers.ComponentInstanceController;
import SW9.utility.colors.Color;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.util.function.BiConsumer;

/**
 * Presentation for a component instance.
 */
public class ComponentInstancePresentation extends StackPane {
    private final ComponentInstanceController controller;

    public ComponentInstancePresentation(final ComponentInstance instance, final SystemModel system) {
        controller = new EcdarFXMLLoader().loadAndGetController("ComponentInstancePresentation.fxml", this);

        controller.setInstance(instance);
        controller.setSystem(system);

        initializeName();
        initializeDimensions();
        initializeToolbar();
        initializeFrame();
        initializeBackground();
    }

    /**
     * Initializes handling of the name.
     */
    private void initializeName() {
        final ComponentInstance instance = controller.getInstance();
        final BooleanProperty initialized = new SimpleBooleanProperty(false);

        controller.identifier.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && !initialized.get()) {
                controller.root.requestFocus();
                initialized.setValue(true);
            }
        });

        // Bind the model to the text field
        controller.identifier.textProperty().bindBidirectional(instance.getIdProperty());

        final Runnable updateColor = () -> {
            final Color color = instance.getComponent().getColor();
            final Color.Intensity colorIntensity = instance.getComponent().getColorIntensity();

            // Set the text color for the label
            controller.identifier.setStyle("-fx-text-fill: " + color.getTextColorRgbaString(colorIntensity) + ";");
            controller.identifier.setFocusColor(color.getTextColor(colorIntensity));
            controller.identifier.setUnFocusColor(javafx.scene.paint.Color.TRANSPARENT);

            controller.originalComponentLabel.setStyle("-fx-text-fill: " + color.getTextColorRgbaString(colorIntensity) + ";");
        };

        // Update color and whenever color of the component changes
        updateColor.run();
        instance.getComponent().colorProperty().addListener(observable -> updateColor.run());

        // Center the text vertically and aff a left padding of CORNER_SIZE
        controller.identifier.setPadding(new Insets(2, 0, 0, ModelPresentation.CORNER_SIZE)); // TODO maybe move constant
        controller.identifier.setOnKeyPressed(CanvasController.getLeaveTextAreaKeyHandler());

        controller.originalComponentLabel.setPadding(new Insets(0, 5, 0, 15));
        controller.originalComponentLabel.textProperty().bind(instance.getComponent().nameProperty());
    }

    /**
     * Initializes the dimensions.
     */
    private void initializeDimensions() {
        final ComponentInstance instance = controller.getInstance();

        setMinWidth(Grid.GRID_SIZE * 24);
        setMaxWidth(Grid.GRID_SIZE * 24);
        setMinHeight(Grid.GRID_SIZE * 12);
        setMaxHeight(Grid.GRID_SIZE * 12);

        instance.getBox().getWidthProperty().bind(widthProperty());
        instance.getBox().getHeightProperty().bind(heightProperty());

        // Bind x and y
        setLayoutX(instance.getBox().getX());
        setLayoutY(instance.getBox().getY());
        instance.getBox().getXProperty().bind(layoutXProperty());
        instance.getBox().getYProperty().bind(layoutYProperty());
    }


    /**
     * Initializes handling of the toolbar.
     */
    private void initializeToolbar() {
        final Component component = controller.getInstance().getComponent();

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Set the background of the toolbar
            controller.toolbar.setBackground(new Background(new BackgroundFill(
                    newColor.getColor(newIntensity),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));

            controller.toolbar.setPrefHeight(ModelPresentation.TOOL_BAR_HEIGHT); // TODO maybe move constant
        };

        // Update color now and whenever color of component changes
        updateColor.accept(component.getColor(), component.getColorIntensity());
        component.colorProperty().addListener(observable -> updateColor.accept(component.getColor(), component.getColorIntensity()));
    }

    /**
     * Initializes the frame.
     */
    private void initializeFrame() {
        final Component component = controller.getInstance().getComponent();

        final Shape[] mask = new Shape[1];
        final Rectangle rectangle = new Rectangle(getMinWidth(), getMinHeight());

        // Generate first corner (to subtract)
        final Polygon corner1 = new Polygon(
                0, 0,
                ModelPresentation.CORNER_SIZE + 2, 0,
                0, ModelPresentation.CORNER_SIZE + 2
        );

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Mask the parent of the frame (will also mask the background)
            mask[0] = Path.subtract(rectangle, corner1);
            controller.frame.setClip(mask[0]);
            controller.background.setClip(Path.union(mask[0], mask[0]));

            // Bind the missing lines that we cropped away
            controller.line1.setStartX(ModelPresentation.CORNER_SIZE);
            controller.line1.setStartY(0);
            controller.line1.setEndX(0);
            controller.line1.setEndY(ModelPresentation.CORNER_SIZE);
            controller.line1.setStroke(newColor.getColor(newIntensity.next(2)));
            controller.line1.setStrokeWidth(1.25);
            StackPane.setAlignment(controller.line1, Pos.TOP_LEFT);

            // Set the stroke color to two shades darker
            controller.frame.setBorder(new Border(new BorderStroke(
                    newColor.getColor(newIntensity.next(2)),
                    BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY,
                    new BorderWidths(1),
                    Insets.EMPTY
            )));
        };

        // Update color now and on color change
        updateColor.accept(component.getColor(), component.getColorIntensity());
        component.colorProperty().addListener(observable -> updateColor.accept(component.getColor(), component.getColorIntensity()));

    }

    /**
     * Initializes the background.
     */
    private void initializeBackground() {
        final Component component = controller.getInstance().getComponent();

        // Bind the background width and height to the values in the model
        controller.background.widthProperty().bind(minWidthProperty());
        controller.background.heightProperty().bind(minHeightProperty());

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Set the background color to the lightest possible version of the color
            controller.background.setFill(newColor.getColor(newIntensity.next(-20)));
        };

        // Update color now and on color change
        updateColor.accept(component.getColor(), component.getColorIntensity());
        component.colorProperty().addListener(observable -> updateColor.accept(component.getColor(), component.getColorIntensity()));

    }

}
