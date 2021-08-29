package ecdar.presentations;

import ecdar.abstractions.*;
import ecdar.controllers.ComponentInstanceController;
import ecdar.controllers.EcdarController;
import ecdar.controllers.MainController;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.ItemDragHelper;
import ecdar.utility.helpers.SelectHelper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static ecdar.presentations.Grid.GRID_SIZE;

/**
 * Presentation for a component instance.
 */
public class ComponentInstancePresentation extends StackPane implements SelectHelper.ItemSelectable {
    private final ComponentInstanceController controller;
    private final List<BiConsumer<Color, Color.Intensity>> updateColorDelegates = new ArrayList<>();

    public ComponentInstancePresentation(final ComponentInstance instance, final EcdarSystem system) {
        controller = new EcdarFXMLLoader().loadAndGetController("ComponentInstancePresentation.fxml", this);

        controller.setInstance(instance);
        controller.setSystem(system);

        initializeName();
        initializeDimensions();
        initializeToolbar();
        initializeFrame();
        initializeSeparator();
        initializeBackground();
        initializeMouseControls();
        initializeNails();

        this.widthProperty().addListener((obs, oldValue, newValue) -> {
            double halfWidth = newValue.intValue()/2;
            controller.outputContainer.setPrefWidth(halfWidth - 1); // Magic 1 (without it it becomes too large due to the separator line)
            controller.inputContainer.setPrefWidth(halfWidth - 1);

            double nailX = newValue.intValue()/4; // Place nails at a quarter of the component width
            controller.inputNailGroup.setTranslateX(-nailX);
            controller.outputNailGroup.setTranslateX(nailX);
        });
    }

    /***
     * Initializes the nails representing the input and output halves of the component,
     * so they change color whenever the component instance does e.g. on drag.
     */
    private void initializeNails() {
        final Component component = controller.getInstance().getComponent();
        final BiConsumer<Color, Color.Intensity> updateNailColor = (newColor, newIntensity) ->
        {
            final Color color = newColor;
            final Color.Intensity colorIntensity = newIntensity;

            controller.inputNailCircle.setFill(color.getColor(colorIntensity));
            controller.inputNailCircle.setStroke(color.getColor(colorIntensity.next(2)));

            controller.outputNailCircle.setFill(color.getColor(colorIntensity));
            controller.outputNailCircle.setStroke(color.getColor(colorIntensity.next(2)));
        };

        // When the color of the component updates, update the nail indicator as well
        controller.getInstance().getComponent().colorProperty().addListener(
                (observable) -> updateNailColor.accept(component.getColor(), component.getColorIntensity()));

        // When the color intensity of the component updates, update the nail indicator
        controller.getInstance().getComponent().colorIntensityProperty().addListener(
                (observable) -> updateNailColor.accept(component.getColor(), component.getColorIntensity()));

        // Initialize the color of the nail with the current color
        updateNailColor.accept(component.getColor(), component.getColorIntensity());
        updateColorDelegates.add(updateNailColor);
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
        controller.identifier.textProperty().bindBidirectional(instance.getInstanceIdProperty());

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
        controller.identifier.setPadding(new Insets(2, 0, 0, Grid.CORNER_SIZE));
        controller.identifier.setOnKeyPressed(MainController.getActiveCanvasPresentation().getController().getLeaveTextAreaKeyHandler());

        controller.originalComponentLabel.setPadding(new Insets(0, 5, 0, 15));
        controller.originalComponentLabel.textProperty().bind(instance.getComponent().nameProperty());
    }

    /**
     * Initializes the dimensions.
     */
    private void initializeDimensions() {
        final ComponentInstance instance = controller.getInstance();

        setMinWidth(ComponentInstance.WIDTH);
        setMaxWidth(ComponentInstance.WIDTH);
        setMinHeight(ComponentInstance.HEIGHT);
        setMaxHeight(ComponentInstance.HEIGHT);

        instance.getBox().getWidthProperty().bind(widthProperty());
        instance.getBox().getHeightProperty().bind(heightProperty());

        // Bind x and y
        setLayoutX(Grid.snap(instance.getBox().getX()));
        setLayoutY(Grid.snap(instance.getBox().getY()));
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

            controller.toolbar.setPrefHeight(Grid.TOOL_BAR_HEIGHT);
        };

        // Update color now, whenever color of component changes, and when someone uses the color delegates
        updateColor.accept(component.getColor(), component.getColorIntensity());
        component.colorProperty().addListener(observable -> updateColor.accept(component.getColor(), component.getColorIntensity()));
        updateColorDelegates.add(updateColor);
    }

    /***
     * Initializes the line that separates the input and output signatures
     * Updates the color of the line when the component color changes
     * Updates(redraws) the line when the component size changes
     */
    private void initializeSeparator() {
        final Component component = controller.getInstance().getComponent();

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            controller.separatorLine.setStroke(newColor.getColor(newIntensity.next(2)));
        };

        final Runnable drawLine = () -> {
            controller.separatorLine.setStartX(0);
            controller.separatorLine.setStartY(0);
            controller.separatorLine.setEndX(0);

            double lineHeight = heightProperty().get() - controller.toolbar.getHeight()-1;
            controller.separatorLine.setEndY(-lineHeight);
            controller.separatorLine.setStrokeWidth(1);
            StackPane.setAlignment(controller.separatorLine, Pos.BOTTOM_CENTER);
        };

        heightProperty().addListener(observable -> drawLine.run());
        controller.toolbar.heightProperty().addListener(obs -> drawLine.run());

        updateColor.accept(component.getColor(), component.getColorIntensity());
        component.colorProperty().addListener(observable -> updateColor.accept(component.getColor(), component.getColorIntensity()));
        updateColorDelegates.add(updateColor);
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
                Grid.CORNER_SIZE + 2, 0,
                0, Grid.CORNER_SIZE + 2
        );

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Mask the parent of the frame (will also mask the background)
            mask[0] = Path.subtract(rectangle, corner1);
            controller.frame.setClip(mask[0]);
            controller.background.setClip(Path.union(mask[0], mask[0]));

            // Bind the missing lines that we cropped away
            controller.line1.setStartX(Grid.CORNER_SIZE);
            controller.line1.setStartY(0);
            controller.line1.setEndX(0);
            controller.line1.setEndY(Grid.CORNER_SIZE);
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

        // Update color now, whenever color of component changes, and when someone uses the color delegates
        updateColor.accept(component.getColor(), component.getColorIntensity());
        component.colorProperty().addListener(observable -> updateColor.accept(component.getColor(), component.getColorIntensity()));
        updateColorDelegates.add(updateColor);
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

        // Update color now, whenever color of component changes, and when someone uses the color delegates
        updateColor.accept(component.getColor(), component.getColorIntensity());
        component.colorProperty().addListener(observable -> updateColor.accept(component.getColor(), component.getColorIntensity()));
        updateColorDelegates.add(updateColor);
    }

    /**
     * Initializes the mouse controls.
     * This includes handling of selection and making this draggable.
     */
    private void initializeMouseControls() {
        addEventHandler(MouseEvent.MOUSE_PRESSED, (event) -> {
            event.consume();

            if (event.isShortcutDown()) {
                if (SelectHelper.getSelectedElements().contains(this)) {
                    SelectHelper.deselect(this);
                } else {
                    SelectHelper.addToSelection(this);
                }
            } else {
                SelectHelper.select(this);
            }
        });

        ItemDragHelper.makeDraggable(this, this::getDragBounds);
    }

    /**
     * Dyes the delegates with the select color.
     */
    @Override
    public void select() {
        updateColorDelegates.forEach(colorConsumer -> colorConsumer.accept(SelectHelper.SELECT_COLOR, SelectHelper.SELECT_COLOR_INTENSITY_NORMAL));
    }

    /**
     * Dyes the delegates with the component color.
     */
    @Override
    public void deselect() {
        updateColorDelegates.forEach(colorConsumer -> {
            final Component component = controller.getInstance().getComponent();

            colorConsumer.accept(component.getColor(), component.getColorIntensity());
        });
    }

    /**
     * This method is meant for dyeing with a custom color.
     * But since, component instances use the color of the corresponding component,
     * this method does nothing.
     * @param color not used
     * @param intensity not used
     */
    @Override
    @Deprecated
    public void color(final Color color, final Color.Intensity intensity) { }

    /**
     * Gets the color of the corresponding component.
     * @return the color of the component.
     */
    @Override
    public Color getColor() {
        return controller.getInstance().getComponent().getColor();
    }

    /**
     * Gets the color intensity of the corresponding component.
     * @return the color of the component
     */
    @Override
    public Color.Intensity getColorIntensity() {
        return controller.getInstance().getComponent().getColorIntensity();
    }

    /**
     * Gets the bound that it is valid to drag the instance within.
     * @return the bounds
     */
    @Override
    public ItemDragHelper.DragBounds getDragBounds() {
        final ObservableDoubleValue minX = new SimpleDoubleProperty(GRID_SIZE);
        final ObservableDoubleValue maxX = controller.getSystem().getBox().getWidthProperty()
                .subtract(GRID_SIZE)
                .subtract(controller.getInstance().getBox().getWidth());
        final ObservableDoubleValue minY = new SimpleDoubleProperty(Grid.TOOL_BAR_HEIGHT + Grid.GRID_SIZE * 3);
        final ObservableDoubleValue maxY = controller.getSystem().getBox().getHeightProperty()
                .subtract(GRID_SIZE)
                .subtract(controller.getInstance().getBox().getHeight());
        return new ItemDragHelper.DragBounds(minX, maxX, minY, maxY);
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

    @Override
    public double getSelectableWidth() {
        return ComponentInstance.WIDTH;
    }

    @Override
    public double getSelectableHeight() {
        return ComponentInstance.HEIGHT;
    }
}
