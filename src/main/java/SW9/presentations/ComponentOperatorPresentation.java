package SW9.presentations;
import SW9.abstractions.ComponentOperator;
import SW9.abstractions.EcdarSystem;
import SW9.controllers.ComponentOperatorController;
import SW9.utility.colors.Color;
import SW9.utility.helpers.ItemDragHelper;
import SW9.utility.helpers.SelectHelper;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import static SW9.presentations.Grid.GRID_SIZE;

/**
 * Presentation of a component operator
 */
public class ComponentOperatorPresentation extends StackPane implements SelectHelper.ItemSelectable {
    private final ComponentOperatorController controller;
    private final List<BiConsumer<Color, Color.Intensity>> updateColorDelegates = new ArrayList<>();

    /**
     * Constructor for ComponentOperatorPresentation, sets the controller and initializes label, dimensions, frame and mouse controls
     * @param operator the ComponentOperator that we want to present
     * @param system the system we want to present the operator within
     */
    public ComponentOperatorPresentation(final ComponentOperator operator, final EcdarSystem system){
        controller = new EcdarFXMLLoader().loadAndGetController("ComponentOperatorPresentation.fxml", this);

        controller.setOperator(operator);
        controller.setSystem(system);

        initializeIdLabel();
        initializeDimensions();
        initializeFrame();
        initializeMouseControls();
    }

    /**
     * Initializes the label
     */
    private void initializeIdLabel() {
        final EcdarSystem system = controller.getSystem();
        final ComponentOperator operator = controller.getOperator();
        final Label idLabel = controller.label;

        idLabel.textProperty().set(operator.getLabel());
        idLabel.setTranslateX(0);
        idLabel.setTranslateY(-2);

        // Delegate to style the label based on the color of the location
        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> idLabel.setTextFill(newColor.getTextColor(newIntensity));

        // Update color now and on color change
        updateColor.accept(system.getColor(), system.getColorIntensity());
        system.colorProperty().addListener(observable -> updateColor.accept(system.getColor(), system.getColorIntensity()));
        updateColorDelegates.add(updateColor);
    }


    /**
     * Initializes the dimensions.
     */
    private void initializeDimensions() {
        final ComponentOperator instance = controller.getOperator();

        setMinWidth(ComponentOperator.WIDTH);
        setMaxWidth(ComponentOperator.WIDTH);
        setMinHeight(ComponentOperator.HEIGHT);
        setMaxHeight(ComponentOperator.HEIGHT);

        instance.getBox().getWidthProperty().bind(widthProperty());
        instance.getBox().getHeightProperty().bind(heightProperty());

        // Bind x and y
        setLayoutX(Grid.snap(instance.getBox().getX()));
        setLayoutY(Grid.snap(instance.getBox().getY()));
        instance.getBox().getXProperty().bind(layoutXProperty());
        instance.getBox().getYProperty().bind(layoutYProperty());
    }

    /**
     * Initializes the frame.
     */
    private void initializeFrame() {
        final EcdarSystem system = controller.getSystem();

        controller.frame.getPoints().addAll(1d * Grid.GRID_SIZE, - 1d * Grid.GRID_SIZE);
        controller.frame.getPoints().addAll(- 1d * Grid.GRID_SIZE, - 1d * Grid.GRID_SIZE);
        controller.frame.getPoints().addAll(- 2d * Grid.GRID_SIZE, 0d);
        controller.frame.getPoints().addAll(- 1d * Grid.GRID_SIZE, 1d * Grid.GRID_SIZE);
        controller.frame.getPoints().addAll(1d * Grid.GRID_SIZE, 1d * Grid.GRID_SIZE);
        controller.frame.getPoints().addAll(2d * Grid.GRID_SIZE, 0d);

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> controller.frame.setFill(newColor.getColor(newIntensity));

        updateColor.accept(system.getColor(), system.getColorIntensity());
        system.colorProperty().addListener(observable -> updateColor.accept(system.getColor(), system.getColorIntensity()));
        updateColorDelegates.add(updateColor);
    }

    /**
     * Initialize the mouse controls
     */
    private void initializeMouseControls() {
        addEventHandler(MouseEvent.MOUSE_PRESSED, (event) -> {
            event.consume();

            if (event.isShortcutDown()) {
                SelectHelper.addToSelection(this);
            } else {
                SelectHelper.select(this);
            }
        });

        ItemDragHelper.makeDraggable(this, this::getDragBounds);
    }

    /**
     * Is meant to set the color, but this feature is not available for operators so this method does nothing
     */
    @Override
    public void color(final Color color, final Color.Intensity intensity) {
    }

    @Override
    public Color getColor() {
        return controller.getSystem().getColor();
    }

    @Override
    public Color.Intensity getColorIntensity() {
        return controller.getSystem().getColorIntensity();
    }

    /**
     * Gets the bound that it is valid to drag the operator within.
     * @return the bounds
     */
    @Override
    public ItemDragHelper.DragBounds getDragBounds() {
        final ObservableDoubleValue minX = new SimpleDoubleProperty(Grid.GRID_SIZE);
        final ObservableDoubleValue maxX = controller.getSystem().getBox().getWidthProperty()
                .subtract(Grid.GRID_SIZE)
                .subtract(controller.getOperator().getBox().getWidth());
        final ObservableDoubleValue minY = new SimpleDoubleProperty(Grid.TOOL_BAR_HEIGHT + Grid.GRID_SIZE * 3);
        final ObservableDoubleValue maxY = controller.getSystem().getBox().getHeightProperty()
                .subtract(Grid.GRID_SIZE)
                .subtract(controller.getOperator().getBox().getHeight());
        return new ItemDragHelper.DragBounds(minX, maxX, minY, maxY);
    }

    @Override
    public DoubleProperty xProperty() {
        return controller.getOperator().getBox().getXProperty();
    }

    @Override
    public DoubleProperty yProperty() {
        return controller.getOperator().getBox().getYProperty();
    }

    @Override
    public double getX() {
        return controller.getOperator().getBox().getX();
    }

    @Override
    public double getY() {
        return controller.getOperator().getBox().getY();
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
            final EcdarSystem system = controller.getSystem();

            colorConsumer.accept(system.getColor(), system.getColorIntensity());
        });

    }
}
