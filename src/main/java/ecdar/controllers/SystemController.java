package ecdar.controllers;

import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.presentations.*;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.SelectHelper;
import com.jfoenix.controls.JFXPopup;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.*;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;

import static ecdar.presentations.Grid.GRID_SIZE;
import static ecdar.presentations.ModelPresentation.TOP_LEFT_CORNER;

/**
 * Controller for a system.
 */
public class SystemController extends ModelController implements Initializable {
    public Line topRightLine;

    public Pane systemRootContainer;
    public Pane componentInstanceContainer;
    public Pane componentOperatorContainer;
    public Pane edgeContainer;
    
    private final Map<ComponentInstance, ComponentInstancePresentation> componentInstancePresentationMap = new HashMap<>();
    private final Map<ComponentOperator, ComponentOperatorPresentation> componentOperatorPresentationMap = new HashMap<>();
    private final Map<EcdarSystemEdge, SystemEdgePresentation> edgePresentationMap = new HashMap<>();

    private final ObjectProperty<EcdarSystem> system = new SimpleObjectProperty<>();

    private Circle dropDownMenuHelperCircle;
    private DropDownMenu contextMenu;

    public EcdarSystem getSystem() {
        return system.get();
    }

    public void setSystem(final EcdarSystem system) {
        this.system.setValue(system);
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        // Initialize when system is added
        system.addListener((observable, oldValue, newValue) -> {
            initializeSystemRoot(newValue);

            initializeContextMenu(newValue);

            initializeComponentInstanceHandling(newValue);
            initializeOperatorHandling(newValue);
            initializeEdgeHandling(newValue);
        });

        super.initialize(getSystem().getBox());
        initializeDimensions(getSystem().getBox());

        // Initialize methods that is sensitive to width and height
        final Runnable onUpdateSize = () -> {
            initializeToolbar();
            initializeFrame();
            initializeBackground();
        };

        onUpdateSize.run();

        // Re-run initialisation on update of width and height property
        getSystem().getBox().getWidthProperty().addListener(observable -> onUpdateSize.run());
        getSystem().getBox().getHeightProperty().addListener(observable -> onUpdateSize.run());
    }

    private void initializeSystemRoot(final EcdarSystem system) {
        systemRootContainer.getChildren().add(new SystemRootPresentation(system));
    }

    /**
     * Initializes the toolbar.
     */
    private void initializeToolbar() {
        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Set the background of the toolbar
            toolbar.setBackground(new Background(new BackgroundFill(
                    newColor.getColor(newIntensity),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));

            toolbar.setPrefHeight(Grid.TOOL_BAR_HEIGHT);
        };

        getSystem().colorProperty().addListener(observable -> updateColor.accept(getSystem().getColor(), getSystem().getColorIntensity()));

        updateColor.accept(getSystem().getColor(), getSystem().getColorIntensity());
    }

    /**
     * Initializes the frame and handling of it.
     * The frame is a rectangle minus two cutouts.
     */
    private void initializeFrame() {
        final Shape[] mask = new Shape[1];
        final Rectangle rectangle = new Rectangle(getSystem().getBox().getWidth(), getSystem().getBox().getHeight());

        // Generate top right corner (to subtract)
        final Polygon topRightCorner = new Polygon(
                getSystem().getBox().getWidth(), 0,
                getSystem().getBox().getWidth() - Grid.CORNER_SIZE - 2, 0,
                getSystem().getBox().getWidth(), Grid.CORNER_SIZE + 2
        );

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Mask the parent of the frame (will also mask the background)
            mask[0] = Path.subtract(rectangle, TOP_LEFT_CORNER);
            mask[0] = Path.subtract(mask[0], topRightCorner);
            frame.setClip(mask[0]);
            background.setClip(Path.union(mask[0], mask[0]));
            background.setOpacity(0.5);

            // Bind the missing lines that we cropped away
            topLeftLine.setStartX(Grid.CORNER_SIZE);
            topLeftLine.setStartY(0);
            topLeftLine.setEndX(0);
            topLeftLine.setEndY(Grid.CORNER_SIZE);
            topLeftLine.setStroke(newColor.getColor(newIntensity.next(2)));
            topLeftLine.setStrokeWidth(1.25);
            StackPane.setAlignment(topLeftLine, Pos.TOP_LEFT);

            topRightLine.setStartX(0);
            topRightLine.setStartY(0);
            topRightLine.setEndX(Grid.CORNER_SIZE);
            topRightLine.setEndY(Grid.CORNER_SIZE);
            topRightLine.setStroke(newColor.getColor(newIntensity.next(2)));
            topRightLine.setStrokeWidth(1.25);
            StackPane.setAlignment(topRightLine, Pos.TOP_RIGHT);

            // Set the stroke color to two shades darker
            frame.setBorder(new Border(new BorderStroke(
                    newColor.getColor(newIntensity.next(2)),
                    BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY,
                    new BorderWidths(1),
                    Insets.EMPTY
            )));
        };

        // Update now, and update on color change
        updateColor.accept(getSystem().getColor(), getSystem().getColorIntensity());
        getSystem().colorProperty().addListener(observable -> updateColor.accept(getSystem().getColor(), getSystem().getColorIntensity()));
    }

    /**
     * Initializes the background
     */
    private void initializeBackground() {
        // Bind the background width and height to the values in the model
        background.widthProperty().bindBidirectional(getSystem().getBox().getWidthProperty());
        background.heightProperty().bindBidirectional(getSystem().getBox().getHeightProperty());

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Set the background color to the lightest possible version of the color
            background.setFill(newColor.getColor(newIntensity.next(-10).next(2)));
        };

        getSystem().colorProperty().addListener(observable -> updateColor.accept(getSystem().getColor(), getSystem().getColorIntensity()));
        updateColor.accept(getSystem().getColor(), getSystem().getColorIntensity());
    }

    /**
     * Handles when tapping on the background of the system view.
     * @param event mouse event
     */
    @FXML
    private void modelContainerPressed(final MouseEvent event) {
        EcdarController.getActiveCanvasPresentation().getController().leaveTextAreas();
        SelectHelper.clearSelectedElements();

        if (event.isSecondaryButtonDown()) {
            dropDownMenuHelperCircle.setLayoutX(event.getX());
            dropDownMenuHelperCircle.setLayoutY(event.getY());
            DropDownMenu.x = event.getX();
            DropDownMenu.y = event.getY();

            contextMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 0, 0);
        }
    }

    @Override
    public HighLevelModelObject getModel() {
        return getSystem();
    }

    /**
     * Initializes the context menu.
     * @param system system model of this controller
     */
    private void initializeContextMenu(final EcdarSystem system) {
        dropDownMenuHelperCircle = new Circle(5);
        dropDownMenuHelperCircle.setOpacity(0);
        dropDownMenuHelperCircle.setMouseTransparent(true);

        root.getChildren().add(dropDownMenuHelperCircle);

        contextMenu = new DropDownMenu(dropDownMenuHelperCircle);

        // Component instance sub menu
        final DropDownMenu componentInstanceSubMenu = new DropDownMenu(contextMenu.getPopupContent(), 150);

        // Add sub menu element for each component
        Ecdar.getProject().getComponents().forEach(component -> {
            componentInstanceSubMenu.addMenuElement(new MenuElement(component.getName()).setClickable(() -> {
                final ComponentInstance instance = new ComponentInstance(getSystem());

                instance.setComponent(component);
                instance.getBox().setX(DropDownMenu.x);
                instance.getBox().setY(DropDownMenu.y);

                UndoRedoStack.pushAndPerform(
                        () -> getSystem().addComponentInstance(instance),
                        () -> getSystem().removeComponentInstance(instance),
                        "Added component instance '" + instance.toString() + "' to system '" + system.getName() + "'",
                        "add-circle"
                );
                componentInstanceSubMenu.hide();
                contextMenu.hide();
            }));
        });
        // the space between elements in the dropdownmenu is 38
        contextMenu.addSubMenu("Add Component Instance", componentInstanceSubMenu, 0*38, (int) contextMenu.widthProperty().get());
        contextMenu.addSmallSpacerElement();

        final DropDownMenu operatorSubMenu = new DropDownMenu(contextMenu.getPopupContent(), 150);


        operatorSubMenu.addMenuElement(new MenuElement("Add Conjunction").setClickable(() -> {
            final Conjunction operator = new Conjunction(getSystem());

            operator.getBox().setX(DropDownMenu.x);
            operator.getBox().setY(DropDownMenu.y);

            UndoRedoStack.pushAndPerform(
                    () -> getSystem().addComponentOperator(operator),
                    () -> getSystem().removeComponentOperator(operator),
                    "Added operator to system '" + system.getName() + "'",
                    "add-circle"
            );
            operatorSubMenu.hide();
            contextMenu.hide();
        }));

        operatorSubMenu.addMenuElement(new MenuElement("Add Composition").setClickable(() -> {
            final Composition operator = new Composition(getSystem());

            operator.getBox().setX(DropDownMenu.x);
            operator.getBox().setY(DropDownMenu.y);

            UndoRedoStack.pushAndPerform(
                    () -> getSystem().addComponentOperator(operator),
                    () -> getSystem().removeComponentOperator(operator),
                    "Added operator to system '" + system.getName() + "'",
                    "add-circle"
            );
            operatorSubMenu.hide();
            contextMenu.hide();
        }));

        operatorSubMenu.addMenuElement(new MenuElement("Add Quotient").setClickable(() -> {
            final Quotient operator = new Quotient(getSystem());

            operator.getBox().setX(DropDownMenu.x);
            operator.getBox().setY(DropDownMenu.y);

            UndoRedoStack.pushAndPerform(
                    () -> getSystem().addComponentOperator(operator),
                    () -> getSystem().removeComponentOperator(operator),
                    "Added operator to system '" + system.getName() + "'",
                    "add-circle"
            );
            operatorSubMenu.hide();
            contextMenu.hide();
        }));
        contextMenu.addSubMenu("Add Operator", operatorSubMenu, 42,0);

        contextMenu.addColorPicker(system, system::dye);
    }

    /**
     * Handles already added component instances.
     * Initializes handling of added and removed component instances.
     * @param system system model of this controller
     */
    private void initializeComponentInstanceHandling(final EcdarSystem system) {
        system.getComponentInstances().forEach(this::handleAddedComponentInstance);
        system.getComponentInstances().addListener((ListChangeListener<ComponentInstance>) change -> {
            if (change.next()) {
                change.getAddedSubList().forEach(this::handleAddedComponentInstance);
                change.getRemoved().forEach(this::handleRemovedComponentInstance);
            }
        });
    }

    /**
     * Handles an added component instance.
     * @param instance the component instance
     */
    private void handleAddedComponentInstance(final ComponentInstance instance) {
        final ComponentInstancePresentation presentation = new ComponentInstancePresentation(instance, getSystem());
        componentInstancePresentationMap.put(instance, presentation);
        componentInstanceContainer.getChildren().add(presentation);
    }

    /**
     * Handles a removed component instance.
     * @param instance the component instance.
     */
    private void handleRemovedComponentInstance(final ComponentInstance instance) {
        componentInstanceContainer.getChildren().remove(componentInstancePresentationMap.get(instance));
        componentInstancePresentationMap.remove(instance);
    }

    /**
     * Handles already added component operators.
     * Initializes handling of added and removed component operators.
     * @param system system model of this controller
     */
    private void initializeOperatorHandling(final EcdarSystem system) {
        system.getComponentOperators().forEach(this::handleAddedComponentOperator);
        system.getComponentOperators().addListener((ListChangeListener<ComponentOperator>) change -> {
            if (change.next()) {
                change.getAddedSubList().forEach(this::handleAddedComponentOperator);
                change.getRemoved().forEach(this::handleRemovedComponentOperator);
            }
        });
    }

    /**
     * Handles an added component operator.
     * @param operator the component operator
     */
    private void handleAddedComponentOperator(final ComponentOperator operator) {
        final ComponentOperatorPresentation presentation = new ComponentOperatorPresentation(operator, getSystem());
        componentOperatorPresentationMap.put(operator, presentation);
        componentOperatorContainer.getChildren().add(presentation);
    }

    /**
     * Handles a removed component operator.
     * @param operator the component operator.
     */
    private void handleRemovedComponentOperator(final ComponentOperator operator) {
        componentOperatorContainer.getChildren().remove(componentOperatorPresentationMap.get(operator));
        componentOperatorPresentationMap.remove(operator);
    }

    /**
     * Handles already added edges.
     * Initializes handling of added and removed edges.
     * @param system system model of this controller
     */
    private void initializeEdgeHandling(final EcdarSystem system) {
        system.getEdges().forEach(this::handleAddedEdge);
        system.getEdges().addListener((ListChangeListener<EcdarSystemEdge>) change -> {
            if (change.next()) {
                change.getAddedSubList().forEach(this::handleAddedEdge);
                change.getRemoved().forEach(this::handleRemovedEdge);
            }
        });
    }

    /**
     * Handles an added edge.
     * @param edge the edge
     */
    private void handleAddedEdge(final EcdarSystemEdge edge) {
        final SystemEdgePresentation presentation = new SystemEdgePresentation(edge, getSystem());
        edgePresentationMap.put(edge, presentation);
        edgeContainer.getChildren().add(presentation);
    }

    /**
     * Handles a removed component instance.
     * @param edge the edge
     */
    private void handleRemovedEdge(final EcdarSystemEdge edge) {
        edgeContainer.getChildren().remove(edgePresentationMap.get(edge));
        edgePresentationMap.remove(edge);

        // Set nodes to null in order to notify potential listeners
        edge.setTempNode(null);
        edge.setChild(null);
        edge.setParent(null);
    }

    /**
     * Hides the border and background.
     */
    @Override
    void hideBorderAndBackground() {
        super.hideBorderAndBackground();
        topRightLine.setVisible(false);
    }

    /**
     * Shows the border and background.
     */
    @Override
    void showBorderAndBackground() {
        super.showBorderAndBackground();
        topRightLine.setVisible(true);
    }

    /**
     * Gets the minimum allowed width when dragging the anchor.
     * It is determined by the position and size of the system nodes.
     * @return the minimum allowed width
     */
    @Override
    double getDragAnchorMinWidth() {
        double minWidth = getSystem().getSystemRoot().getX() + SystemRoot.WIDTH + 2 * Grid.GRID_SIZE;

        for (final ComponentInstance instance : getSystem().getComponentInstances()) {
            minWidth = Math.max(minWidth, instance.getBox().getX() + instance.getBox().getWidth() + Grid.GRID_SIZE);
        }

        for (final ComponentOperator operator : getSystem().getComponentOperators()) {
            minWidth = Math.max(minWidth, operator.getBox().getX() + operator.getBox().getWidth() + Grid.GRID_SIZE);
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
        double minHeight = 10 * GRID_SIZE;

        for (final ComponentInstance instance : getSystem().getComponentInstances()) {
            minHeight = Math.max(minHeight, instance.getBox().getY() + instance.getBox().getHeight() + Grid.GRID_SIZE);
        }

        for (final ComponentOperator operator : getSystem().getComponentOperators()) {
            minHeight = Math.max(minHeight, operator.getBox().getY() + operator.getBox().getHeight() + Grid.GRID_SIZE);
        }

        return minHeight;
    }
}
