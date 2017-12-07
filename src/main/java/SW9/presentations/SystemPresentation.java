package SW9.presentations;

import SW9.abstractions.SystemModel;
import SW9.controllers.ModelController;
import SW9.controllers.SystemController;
import SW9.utility.colors.Color;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.io.IOException;
import java.net.URL;
import java.util.function.BiConsumer;

/**
 * Presentation for a system.
 */
public class SystemPresentation extends ModelPresentation {
    private final SystemController controller;

    public SystemPresentation(final SystemModel system) {
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

        // Re run initialisation on update of width and height property
        system.getBox().widthProperty().addListener(observable -> onUpdateSize.run());
        system.getBox().heightProperty().addListener(observable -> onUpdateSize.run());
    }

    private void initializeToolbar() {
        final SystemModel system = controller.getSystem();

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Set the background of the toolbar
            controller.toolbar.setBackground(new Background(new BackgroundFill(
                    newColor.getColor(newIntensity),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));

            controller.toolbar.setPrefHeight(TOOL_BAR_HEIGHT);
        };

        system.colorProperty().addListener(observable -> updateColor.accept(system.getColor(), system.getColorIntensity()));

        updateColor.accept(system.getColor(), system.getColorIntensity());
    }

    /**
     * Initializes the frame and handling of it.
     * The frame is a rectangle minus two cutouts.
     */
    private void initializeFrame() {
        final SystemModel system = controller.getSystem();

        final Shape[] mask = new Shape[1];
        final Rectangle rectangle = new Rectangle(system.getBox().getWidth(), system.getBox().getHeight());

        // Generate top right corner (to subtract)
        final Polygon topRightCorner = new Polygon(
                system.getBox().getWidth(), 0,
                system.getBox().getWidth() - CORNER_SIZE - 2, 0,
                system.getBox().getWidth(), CORNER_SIZE + 2
        );

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Mask the parent of the frame (will also mask the background)
            mask[0] = Path.subtract(rectangle, TOP_LEFT_CORNER);
            mask[0] = Path.subtract(mask[0], topRightCorner);
            controller.frame.setClip(mask[0]);
            controller.background.setClip(Path.union(mask[0], mask[0]));
            controller.background.setOpacity(0.5);

            // Bind the missing lines that we cropped away
            controller.topLeftLine.setStartX(CORNER_SIZE);
            controller.topLeftLine.setStartY(0);
            controller.topLeftLine.setEndX(0);
            controller.topLeftLine.setEndY(CORNER_SIZE);
            controller.topLeftLine.setStroke(newColor.getColor(newIntensity.next(2)));
            controller.topLeftLine.setStrokeWidth(1.25);
            StackPane.setAlignment(controller.topLeftLine, Pos.TOP_LEFT);

            controller.topRightLine.setStartX(0);
            controller.topRightLine.setStartY(0);
            controller.topRightLine.setEndX(CORNER_SIZE);
            controller.topRightLine.setEndY(CORNER_SIZE);
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

        system.colorProperty().addListener(observable -> updateColor.accept(system.getColor(), system.getColorIntensity()));
        updateColor.accept(system.getColor(), system.getColorIntensity());
    }

    /**
     * Initializes the background
     */
    private void initializeBackground() {
        final SystemModel system = controller.getSystem();

        // Bind the background width and height to the values in the model
        controller.background.widthProperty().bindBidirectional(system.getBox().widthProperty());
        controller.background.heightProperty().bindBidirectional(system.getBox().heightProperty());

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

    // TODO
    @Override
    double getDragAnchorMinWidth() {
        return 0;
    }

    // TODO
    @Override
    double getDragAnchorMinHeight() {
        return 0;
    }
}
