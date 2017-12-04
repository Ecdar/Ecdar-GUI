package SW9.presentations;

import SW9.abstractions.Component;
import SW9.controllers.CanvasController;
import SW9.controllers.ComponentController;
import SW9.utility.UndoRedoStack;
import SW9.utility.colors.Color;
import SW9.utility.helpers.MouseTrackable;
import SW9.utility.helpers.SelectHelper;
import SW9.utility.mouse.MouseTracker;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.layout.*;
import javafx.scene.shape.*;
import org.fxmisc.richtext.StyleSpans;
import org.fxmisc.richtext.StyleSpansBuilder;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
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
        final URL location = this.getClass().getResource("ComponentPresentation.fxml");

        final FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(location);
        fxmlLoader.setBuilderFactory(new JavaFXBuilderFactory());

        try {
            fxmlLoader.setRoot(this);
            fxmlLoader.load(location.openStream());

            // Set the width and the height of the view to the values in the abstraction
            setMinWidth(component.getBox().getWidth());
            setMaxWidth(component.getBox().getWidth());
            setMinHeight(component.getBox().getHeight());
            setMaxHeight(component.getBox().getHeight());
            minHeightProperty().bindBidirectional(component.getBox().heightProperty());
            maxHeightProperty().bindBidirectional(component.getBox().heightProperty());
            minWidthProperty().bindBidirectional(component.getBox().widthProperty());
            maxWidthProperty().bindBidirectional(component.getBox().widthProperty());

            controller = fxmlLoader.getController();
            controller.setComponent(component);

            // Initializer methods that is sensitive to width and height
            final Runnable onUpdateSize = () -> {
                initializeToolbar();
                initializeFrame();
                initializeBackground();
            };

            initializeName();
            initializeDragAnchors();
            onUpdateSize.run();

            // Re run initialisation on update of width and height property
            component.getBox().widthProperty().addListener(observable -> {
                onUpdateSize.run();
            });
            component.getBox().heightProperty().addListener(observable -> {
                onUpdateSize.run();
            });

            controller.declarationTextArea.textProperty().addListener((obs, oldText, newText) ->
                    controller.declarationTextArea.setStyleSpans(0, computeHighlighting(newText)));

        } catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
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

    private void initializeDragAnchors() {
        final Component component = controller.getComponent();
        final BooleanProperty wasResized = new SimpleBooleanProperty(false);

        // Bottom anchor
        final Rectangle bottomAnchor = controller.bottomAnchor;

        bottomAnchor.setCursor(Cursor.S_RESIZE);

        // Bind the place and size of bottom anchor
        bottomAnchor.widthProperty().bind(component.getBox().widthProperty().subtract(CORNER_SIZE));
        bottomAnchor.setHeight(5);

        final DoubleProperty prevY = new SimpleDoubleProperty();
        final DoubleProperty prevHeight = new SimpleDoubleProperty();

        final Supplier<Double> componentMinHeight = () -> {

            final DoubleProperty minHeight = new SimpleDoubleProperty(10 * GRID_SIZE);

            component.getLocations().forEach(location -> {
                minHeight.set(Math.max(minHeight.doubleValue(), location.getY() + GRID_SIZE * 2));
            });

            component.getEdges().forEach(edge -> {
                edge.getNails().forEach(nail -> {
                    minHeight.set(Math.max(minHeight.doubleValue(), nail.getY() + GRID_SIZE));
                });
            });

            return minHeight.get();
        };

        bottomAnchor.setOnMousePressed(event -> {
            prevY.set(event.getScreenY());
            prevHeight.set(component.getBox().getHeight());
        });

        bottomAnchor.setOnMouseDragged(event -> {
            double diff = event.getScreenY() - prevY.get();
            diff -= diff % GRID_SIZE;

            final double newHeight = prevHeight.get() + diff;
            final double minHeight = componentMinHeight.get();

            component.getBox().setHeight(Math.max(newHeight, minHeight));
            wasResized.set(true);
        });

        bottomAnchor.setOnMouseReleased(event -> {
            if (!wasResized.get()) return;
            final double previousHeight = prevHeight.doubleValue();
            final double currentHeight = component.getBox().getHeight();

            // If no difference do not save change
            if (previousHeight == currentHeight) return;

            UndoRedoStack.pushAndPerform(() -> { // Perform
                        component.getBox().setHeight(currentHeight);
                    }, () -> { // Undo
                        component.getBox().setHeight(previousHeight);
                    },
                    "Component height resized", "settings-overscan");

            wasResized.set(false);
        });

        // Right anchor
        final Rectangle rightAnchor = controller.rightAnchor;

        rightAnchor.setCursor(Cursor.E_RESIZE);

        // Bind the place and size of bottom anchor
        rightAnchor.setWidth(5);
        rightAnchor.heightProperty().bind(component.getBox().heightProperty().subtract(CORNER_SIZE));

        final DoubleProperty prevX = new SimpleDoubleProperty();
        final DoubleProperty prevWidth = new SimpleDoubleProperty();

        final Supplier<Double> componentMinWidth = () -> {
            final DoubleProperty minWidth = new SimpleDoubleProperty(10 * GRID_SIZE);

            component.getLocations().forEach(location -> {
                minWidth.set(Math.max(minWidth.doubleValue(), location.getX() + GRID_SIZE * 2));
            });

            component.getEdges().forEach(edge -> {
                edge.getNails().forEach(nail -> {
                    minWidth.set(Math.max(minWidth.doubleValue(), nail.getX() + GRID_SIZE));
                });
            });

            return minWidth.get();
        };

        rightAnchor.setOnMousePressed(event -> {
            prevX.set(event.getScreenX());
            prevWidth.set(component.getBox().getWidth());
        });

        rightAnchor.setOnMouseDragged(event -> {
            double diff = event.getScreenX() - prevX.get();
            diff -= diff % GRID_SIZE;

            final double newWidth = prevWidth.get() + diff;
            final double minWidth = componentMinWidth.get();
            component.getBox().setWidth(Math.max(newWidth, minWidth));
            wasResized.set(true);
        });

        rightAnchor.setOnMouseReleased(event -> {
            if (!wasResized.get()) return;
            final double previousWidth = prevWidth.doubleValue();
            final double currentWidth = component.getBox().getWidth();

            // If no difference do not save change
            if (previousWidth == currentWidth) return;

            UndoRedoStack.pushAndPerform(() -> { // Perform
                        component.getBox().setWidth(currentWidth);
                    }, () -> { // Undo
                        component.getBox().setWidth(previousWidth);
                    },
                    "Component width resized", "settings-overscan");

            wasResized.set(false);
        });


    }

    private void initializeName() {
        final Component component = controller.getComponent();
        final BooleanProperty initialized = new SimpleBooleanProperty(false);

        controller.name.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && !initialized.get()) {
                controller.root.requestFocus();
                initialized.setValue(true);
            }
        });

        // Set the text field to the name in the model, and bind the model to the text field
        controller.name.setText(component.getName());
        controller.name.textProperty().addListener((obs, oldName, newName) -> {
            component.nameProperty().unbind();
            component.setName(newName);
        });

        final Runnable updateColor = () -> {
            final Color color = component.getColor();
            final Color.Intensity colorIntensity = component.getColorIntensity();

            // Set the text color for the label
            controller.name.setStyle("-fx-text-fill: " + color.getTextColorRgbaString(colorIntensity) + ";");
            controller.name.setFocusColor(color.getTextColor(colorIntensity));
            controller.name.setUnFocusColor(javafx.scene.paint.Color.TRANSPARENT);
        };

        controller.getComponent().colorProperty().addListener(observable -> updateColor.run());
        updateColor.run();

        // Center the text vertically and aff a left padding of CORNER_SIZE
        controller.name.setPadding(new Insets(2, 0, 0, CORNER_SIZE));
        controller.name.setOnKeyPressed(CanvasController.getLeaveTextAreaKeyHandler());
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
        controller.background.widthProperty().bindBidirectional(component.getBox().widthProperty());
        controller.background.heightProperty().bindBidirectional(component.getBox().heightProperty());

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
}
