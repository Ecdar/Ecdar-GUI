package SW9.presentations;

import SW9.abstractions.Component;
import SW9.abstractions.Edge;
import SW9.abstractions.Location;
import SW9.abstractions.Nail;
import SW9.controllers.ComponentController;
import SW9.controllers.ModelController;
import SW9.utility.colors.Color;
import SW9.utility.helpers.MouseTrackable;
import SW9.utility.helpers.SelectHelper;
import SW9.utility.mouse.MouseTracker;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import org.fxmisc.richtext.StyleSpans;
import org.fxmisc.richtext.StyleSpansBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static SW9.presentations.Grid.GRID_SIZE;

public class ComponentPresentation extends ModelPresentation implements MouseTrackable, SelectHelper.Selectable {
    private static final String uppaalKeywords = "clock|chan|urgent|broadcast";
    private static final String cKeywords = "auto|bool|break|case|char|const|continue|default|do|double|else|enum|extern|float|for|goto|if|int|long|register|return|short|signed|sizeof|static|struct|switch|typedef|union|unsigned|void|volatile|while";
    private static final Pattern UPPAAL = Pattern.compile(""
            + "(" + uppaalKeywords + ")"
            + "|(" + cKeywords + ")"
            + "|(//.*|(\"(?:\\\\[^\"]|\\\\\"|.)*?\")|(?s)/\\*.*?\\*/)");
    private final ComponentController controller;
    private final List<BiConsumer<Color, Color.Intensity>> updateColorDelegates = new ArrayList<>();

    public ComponentPresentation(final Component component) {
        controller = new EcdarFXMLLoader().loadAndGetController("ComponentPresentation.fxml", this);
        controller.setComponent(component);

        super.initialize(component.getBox());

        // Initialize methods that is sensitive to width and height
        final Runnable onUpdateSize = () -> {
            initializeToolbar();
            initializeFrame();
            initializeBackground();
        };

        onUpdateSize.run();

        // Re run initialisation on update of width and height property
        component.getBox().getWidthProperty().addListener(observable -> onUpdateSize.run());
        component.getBox().getHeightProperty().addListener(observable -> onUpdateSize.run());

        controller.declarationTextArea.textProperty().addListener((obs, oldText, newText) ->
                controller.declarationTextArea.setStyleSpans(0, computeHighlighting(newText)));
    }

    public static StyleSpans<Collection<String>> computeHighlighting(final String text) {
        final Matcher matcher = UPPAAL.matcher(text);
        int lastKwEnd = 0;
        final StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {

            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);

            if (matcher.group(1) != null) {
                spansBuilder.add(Collections.singleton("uppaal-keyword"), matcher.end(1) - matcher.start(1));
            } else if (matcher.group(2) != null) {
                spansBuilder.add(Collections.singleton("c-keyword"), matcher.end(2) - matcher.start(2));
            } else if (matcher.group(3) != null) {
                spansBuilder.add(Collections.singleton("comment"), matcher.end(3) - matcher.start(3));
            }

            lastKwEnd = matcher.end();
        }

        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

    private void initializeToolbar() {
        final Component component = controller.getComponent();

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Set the background of the toolbar
            controller.toolbar.setBackground(new Background(new BackgroundFill(
                    newColor.getColor(newIntensity),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));

            // Set the icon color and rippler color of the toggleDeclarationButton
            controller.toggleDeclarationButton.setRipplerFill(newColor.getTextColor(newIntensity));

            controller.toolbar.setPrefHeight(TOOL_BAR_HEIGHT);
            controller.toggleDeclarationButton.setBackground(Background.EMPTY);
        };

        updateColorDelegates.add(updateColor); // TODO maybe move to ModelPresentation

        controller.getComponent().colorProperty().addListener(observable -> updateColor.accept(component.getColor(), component.getColorIntensity()));

        updateColor.accept(component.getColor(), component.getColorIntensity());

        // Set a hover effect for the controller.toggleDeclarationButton
        controller.toggleDeclarationButton.setOnMouseEntered(event -> controller.toggleDeclarationButton.setCursor(Cursor.HAND));
        controller.toggleDeclarationButton.setOnMouseExited(event -> controller.toggleDeclarationButton.setCursor(Cursor.DEFAULT));

    }

    private void initializeFrame() {
        final Component component = controller.getComponent();

        final Shape[] mask = new Shape[1];
        final Rectangle rectangle = new Rectangle(component.getBox().getWidth(), component.getBox().getHeight());

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Mask the parent of the frame (will also mask the background)
            mask[0] = Path.subtract(rectangle, TOP_LEFT_CORNER);
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

            // Set the stroke color to two shades darker
            controller.frame.setBorder(new Border(new BorderStroke(
                    newColor.getColor(newIntensity.next(2)),
                    BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY,
                    new BorderWidths(1),
                    Insets.EMPTY
            )));
        };

        updateColorDelegates.add(updateColor); // TODO maybe move to ModelPresentation

        component.colorProperty().addListener(observable -> {
            updateColor.accept(component.getColor(), component.getColorIntensity());
        });

        updateColor.accept(component.getColor(), component.getColorIntensity());
    }

    private void initializeBackground() {
        final Component component = controller.getComponent();

        // Bind the background width and height to the values in the model
        controller.background.widthProperty().bindBidirectional(component.getBox().getWidthProperty());
        controller.background.heightProperty().bindBidirectional(component.getBox().getHeightProperty());

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Set the background color to the lightest possible version of the color
            controller.background.setFill(newColor.getColor(newIntensity.next(-10).next(2)));
        };

        updateColorDelegates.add(updateColor);  // TODO maybe move to ModelPresentation

        component.colorProperty().addListener(observable -> {
            updateColor.accept(component.getColor(), component.getColorIntensity());
        });

        updateColor.accept(component.getColor(), component.getColorIntensity());
    }

    @Override
    public DoubleProperty xProperty() {
        return layoutXProperty();
    }

    @Override
    public DoubleProperty yProperty() {
        return layoutYProperty();
    }

    @Override
    public double getX() {
        return xProperty().get();
    }

    @Override
    public double getY() {
        return yProperty().get();
    }

    public ComponentController getController() {
        return controller;
    }

    @Override
    public MouseTracker getMouseTracker() {
        return controller.getMouseTracker();
    }

    @Override
    public void select() {
        updateColorDelegates.forEach(colorConsumer -> colorConsumer.accept(SelectHelper.SELECT_COLOR, SelectHelper.SELECT_COLOR_INTENSITY_NORMAL));
    }

    @Override
    public void deselect() {
        updateColorDelegates.forEach(colorConsumer -> {
            final Component component = controller.getComponent();

            colorConsumer.accept(component.getColor(), component.getColorIntensity());
        });
    }

    @Override
    ModelController getModelController() {
        return getController();
    }

    @Override
    double getDragAnchorMinWidth() {
        final Component component = controller.getComponent();
        double minWidth = 10 * GRID_SIZE;

        for (final Location location : component.getLocations()) {
            minWidth = Math.max(minWidth, location.getX() + GRID_SIZE * 2);
        }

        for (final Edge edge : component.getEdges()) {
            for (final Nail nail : edge.getNails()) {
                minWidth = Math.max(minWidth, nail.getX() + GRID_SIZE);
            }
        }

        return minWidth;
    }

    /**
     * Gets the minimum possible height when dragging the anchor.
     * The height is based on the y coordinate of locations, nails and the signature arrows
     * @return the minimum possible height.
     */
    @Override
    double getDragAnchorMinHeight() {
        final Component component = controller.getComponent();
        double minHeight = 10 * GRID_SIZE;

        for (final Location location : component.getLocations()) {
            minHeight = Math.max(minHeight, location.getY() + GRID_SIZE * 2);
        }

        for (final Edge edge : component.getEdges()) {
            for (final Nail nail : edge.getNails()) {
                minHeight = Math.max(minHeight, nail.getY() + GRID_SIZE);
            }
        }

        //Component should not get smaller than the height of the input/output signature containers
        minHeight = Math.max(controller.inputSignatureContainer.getHeight(), minHeight);
        minHeight = Math.max(controller.outputSignatureContainer.getHeight(), minHeight);

        return minHeight;
    }
}
