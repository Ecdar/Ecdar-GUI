package SW9.controllers;

import SW9.Ecdar;
import SW9.abstractions.*;
import SW9.presentations.*;
import SW9.utility.UndoRedoStack;
import SW9.utility.helpers.SelectHelper;
import com.jfoenix.controls.JFXPopup;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

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
    }

    private void initializeSystemRoot(final EcdarSystem system) {
        systemRootContainer.getChildren().add(new SystemRootPresentation(system));
    }

    /**
     * Handles when tapping on the background of the system view.
     * @param event mouse event
     */
    @FXML
    private void modelContainerPressed(final MouseEvent event) {
        event.consume();
        CanvasController.leaveTextAreas();
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
        final DropDownMenu componentInstanceSubMenu = new DropDownMenu(dropDownMenuHelperCircle, 150);

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
        // The submenu is looking best with the -38 offset
        contextMenu.addSubMenu("Add Component Instance", componentInstanceSubMenu, 0*38, 0);

        final DropDownMenu operatorSubMenu = new DropDownMenu(dropDownMenuHelperCircle, 150);


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

            contextMenu.hide();
        }));
        contextMenu.addSubMenu("Add Operator", operatorSubMenu, 1 * 38,0);

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
}
