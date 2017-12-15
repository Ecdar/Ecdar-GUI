package SW9.controllers;

import SW9.Ecdar;
import SW9.abstractions.*;
import SW9.presentations.*;
import SW9.utility.UndoRedoStack;
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
import java.util.function.Consumer;

/**
 * Controller for a system.
 */
public class SystemController extends ModelController implements Initializable {
    private final ObjectProperty<SystemModel> system;
    public Line topRightLine;

    public Pane componentInstanceContainer;
    public Map<ComponentInstance, ComponentInstancePresentation> componentInstancePresentationMap = new HashMap<>();
    public Pane componentOperatorContainer;
    public Map<ComponentOperator, ComponentOperatorPresentation> componentOperatorPresentationMap = new HashMap<>();


    private Circle dropDownMenuHelperCircle;
    private DropDownMenu contextMenu;

    public SystemController() {
        system = new SimpleObjectProperty<>();
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        // Initialize when system is added
        system.addListener((observable, oldValue, newValue) -> {
            initializeContextMenu(newValue);
            initializeComponentInstanceHandling(newValue);
            initializeComponentInstanceOperator(newValue);
        });
    }

    public SystemModel getSystem() {
        return system.get();
    }

    public void setSystem(final SystemModel system) {
        this.system.setValue(system);
    }

    @FXML
    private void modelContainerPressed(final MouseEvent event) {
        event.consume();
        CanvasController.leaveTextAreas();

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
    private void initializeContextMenu(final SystemModel system) {
        dropDownMenuHelperCircle = new Circle(5);
        dropDownMenuHelperCircle.setOpacity(0);
        dropDownMenuHelperCircle.setMouseTransparent(true);

        root.getChildren().add(dropDownMenuHelperCircle);

        contextMenu = new DropDownMenu(root, dropDownMenuHelperCircle, 230, true);

        // Component instance sub menu
        final DropDownMenu componentInstanceSubMenu = new DropDownMenu(root, dropDownMenuHelperCircle, 150, false);

        // Add sub menu element for each component
        Ecdar.getProject().getComponents().forEach(component -> {
            componentInstanceSubMenu.addMenuElement(new MenuElement(component.getName()).setClickable(() -> {
                final ComponentInstance instance = new ComponentInstance();

                instance.setComponent(component);
                instance.getColor().set(system.getColor());
                instance.getColorIntensity().set(system.getColorIntensity());
                instance.getBox().setX(DropDownMenu.x);
                instance.getBox().setY(DropDownMenu.y);

                UndoRedoStack.pushAndPerform(
                        () -> getSystem().addComponentInstance(instance),
                        () -> getSystem().removeComponentInstance(instance),
                        "Added component instance '" + instance.toString() + "' to system '" + system.getName() + "'",
                        "add-circle"
                );

                contextMenu.close();
            }));
        });

        final DropDownMenu operatorSubMenu = new DropDownMenu(root, dropDownMenuHelperCircle, 150, false);


        operatorSubMenu.addMenuElement(new MenuElement("Add Conjunction").setClickable(() -> {
            final Conjunction operator = new Conjunction();

            operator.getColor().set(system.getColor());
            operator.getColorIntensity().set(system.getColorIntensity());
            operator.getBox().setX(DropDownMenu.x);
            operator.getBox().setY(DropDownMenu.y);

            UndoRedoStack.pushAndPerform(
                    () -> getSystem().addComponentOperator(operator),
                    () -> getSystem().removeComponentOperator(operator),
                    "Added operator '" + operator.toString() + "' to system '" + system.getName() + "'",
                    "add-circle"
            );

            contextMenu.close();
        }));

        contextMenu.addSubMenu("Add Component Instance", componentInstanceSubMenu, 0 * 35);
        contextMenu.addSubMenu("Add Operator", operatorSubMenu, 1 * 35);

        contextMenu.addColorPicker(system, system::dye);
    }

    /**
     * Handles already added component instances.
     * Initializes handling of added and removed component instances.
     * @param system system model of this controller
     */
    private void initializeComponentInstanceHandling(final SystemModel system) {
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
     * Handles already added component instances.
     * Initializes handling of added and removed component instances.
     * @param system system model of this controller
     */
    private void initializeComponentInstanceOperator(final SystemModel system) {
        system.getComponentOperators().forEach(this::handleAddedComponentOperator);
        system.getComponentOperators().addListener((ListChangeListener<ComponentOperator>) change -> {
            if (change.next()) {
                change.getAddedSubList().forEach(this::handleAddedComponentOperator);
                change.getRemoved().forEach(this::handleRemovedComponentOperator);
            }
        });
    }

    /**
     * Handles an added component instance.
     * @param operator the component instance
     */
    private void handleAddedComponentOperator(final ComponentOperator operator) {
        final ComponentOperatorPresentation presentation = new ComponentOperatorPresentation(operator, getSystem());
        componentOperatorPresentationMap.put(operator, presentation);
        componentOperatorContainer.getChildren().add(presentation);
    }

    /**
     * Handles a removed component instance.
     * @param operator the component instance.
     */
    private void handleRemovedComponentOperator(final ComponentOperator operator) {
        componentOperatorContainer.getChildren().remove(componentOperatorPresentationMap.get(operator));
        componentOperatorPresentationMap.remove(operator);
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
