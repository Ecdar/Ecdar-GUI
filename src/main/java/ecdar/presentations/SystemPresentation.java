package ecdar.presentations;

import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.controllers.ModelController;
import ecdar.controllers.SystemController;
import ecdar.utility.colors.Color;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.util.function.BiConsumer;

/**
 * Presentation for a system.
 */
public class SystemPresentation extends ModelPresentation {
    private final SystemController controller;

    public SystemPresentation(final EcdarSystem system) {
        controller = new EcdarFXMLLoader().loadAndGetController("SystemPresentation.fxml", this);
        controller.setSystem(system);

        super.initialize(system.getBox());

        initializeDimensions(system.getBox());

        // Initialize methods that is sensitive to width and height
        final Runnable onUpdateSize = () -> {
            initializeToolbar();
            initializeFrame();
            initializeBackground();
        };

        onUpdateSize.run();

        // Re-run initialisation on update of width and height property
        system.getBox().getWidthProperty().addListener(observable -> onUpdateSize.run());
        system.getBox().getHeightProperty().addListener(observable -> onUpdateSize.run());
    }

    /**
     * Initializes the toolbar.
     */
    private void initializeToolbar() {
        final EcdarSystem system = controller.getSystem();

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Set the background of the toolbar
            controller.toolbar.setBackground(new Background(new BackgroundFill(
                    newColor.getColor(newIntensity),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));

            controller.toolbar.setPrefHeight(Ecdar.CANVAS_PADDING * 2);
        };

        system.colorProperty().addListener(observable -> updateColor.accept(system.getColor(), system.getColorIntensity()));

        updateColor.accept(system.getColor(), system.getColorIntensity());
    }

    /**
     * Initializes the frame and handling of it.
     * The frame is a rectangle minus two cutouts.
     */
    private void initializeFrame() {
        final EcdarSystem system = controller.getSystem();

        final Shape[] mask = new Shape[1];
        final Rectangle rectangle = new Rectangle(system.getBox().getWidth(), system.getBox().getHeight());

        // Generate top right corner (to subtract)
        final Polygon topRightCorner = new Polygon(
                system.getBox().getWidth(), 0,
                system.getBox().getWidth() - Ecdar.CANVAS_PADDING * 3 + 8, 0,
                system.getBox().getWidth(), Ecdar.CANVAS_PADDING * 4 + 2
        );

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Mask the parent of the frame (will also mask the background)
            mask[0] = Path.subtract(rectangle, TOP_LEFT_CORNER);
            mask[0] = Path.subtract(mask[0], topRightCorner);
            controller.frame.setClip(mask[0]);
            controller.background.setClip(Path.union(mask[0], mask[0]));
            controller.background.setOpacity(0.5);

            // Bind the missing lines that we cropped away
            controller.topLeftLine.setStartX(40);
            controller.topLeftLine.setStartY(0);
            controller.topLeftLine.setEndX(0);
            controller.topLeftLine.setEndY(40);
            controller.topLeftLine.setStroke(newColor.getColor(newIntensity.next(2)));
            controller.topLeftLine.setStrokeWidth(1.25);
            StackPane.setAlignment(controller.topLeftLine, Pos.TOP_LEFT);

            controller.topRightLine.setStartX(0);
            controller.topRightLine.setStartY(0);
            controller.topRightLine.setEndX(40);
            controller.topRightLine.setEndY(40);
            controller.topRightLine.setStroke(newColor.getColor(newIntensity.next(2)));
            controller.topRightLine.setStrokeWidth(1.25);
            StackPane.setAlignment(controller.topRightLine, Pos.TOP_RIGHT);

            // Set the stroke color to two shades darker
            controller.frame.setBorder(new Border(new BorderStroke(
                    newColor.getColor(newIntensity.next(2)),
                    BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY,
                    new BorderWidths(1),
                    Insets.EMPTY
            )));
        };

        // Update now, and update on color change
        updateColor.accept(system.getColor(), system.getColorIntensity());
        system.colorProperty().addListener(observable -> updateColor.accept(system.getColor(), system.getColorIntensity()));
    }

    /**
     * Initializes the background
     */
    private void initializeBackground() {
        final EcdarSystem system = controller.getSystem();

        // Bind the background width and height to the values in the model
        controller.background.widthProperty().bindBidirectional(system.getBox().getWidthProperty());
        controller.background.heightProperty().bindBidirectional(system.getBox().getHeightProperty());

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Set the background color to the lightest possible version of the color
            controller.background.setFill(newColor.getColor(newIntensity.next(-10).next(2)));
        };

        system.colorProperty().addListener(observable -> updateColor.accept(system.getColor(), system.getColorIntensity()));
        updateColor.accept(system.getColor(), system.getColorIntensity());
    }

    @Override
    ModelController getModelController() {
        return controller;
    }

    /**
     * Gets the minimum allowed width when dragging the anchor.
     * It is determined by the position and size of the system nodes.
     * @return the minimum allowed width
     */
    @Override
    double getDragAnchorMinWidth() {
        final EcdarSystem system = controller.getSystem();
        double minWidth = system.getSystemRoot().getX() + SystemRoot.WIDTH + Ecdar.CANVAS_PADDING * 2;

        for (final ComponentInstance instance : system.getComponentInstances()) {
            minWidth = Math.max(minWidth, instance.getBox().getX() + instance.getBox().getWidth() + Ecdar.CANVAS_PADDING);
        }

        for (final ComponentOperator operator : system.getComponentOperators()) {
            minWidth = Math.max(minWidth, operator.getBox().getX() + operator.getBox().getWidth() + Ecdar.CANVAS_PADDING);
        }

        return minWidth;
    }

    /**
     * Gets the minimum allowed height when dragging the anchor.
     * It is determined by the position and size of the system nodes.
     * @return the minimum allowed height
     */
    @Override
    double getDragAnchorMinHeight() {
        final EcdarSystem system = controller.getSystem();
        double minHeight = Ecdar.CANVAS_PADDING * 2;

        for (final ComponentInstance instance : system.getComponentInstances()) {
            minHeight = Math.max(minHeight, instance.getBox().getY() + instance.getBox().getHeight() + Ecdar.CANVAS_PADDING);
        }

        for (final ComponentOperator operator : system.getComponentOperators()) {
            minHeight = Math.max(minHeight, operator.getBox().getY() + operator.getBox().getHeight() + Ecdar.CANVAS_PADDING);
        }

        return minHeight;
    }
}
