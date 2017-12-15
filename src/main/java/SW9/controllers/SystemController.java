package SW9.controllers;

import SW9.Ecdar;
import SW9.abstractions.ComponentInstance;
import SW9.abstractions.HighLevelModelObject;
import SW9.abstractions.SystemModel;
import SW9.abstractions.SystemRoot;
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
    private final ObjectProperty<SystemModel> system;
    public Line topRightLine;

    public Pane componentInstanceContainer;
    public Map<ComponentInstance, ComponentInstancePresentation> componentInstancePresentationMap = new HashMap<>();

    public Pane systemRootContainer;

    private Circle dropDownMenuHelperCircle;
    private DropDownMenu contextMenu;

    private final SystemRoot systemRoot = new SystemRoot();;

    public SystemController() {
        system = new SimpleObjectProperty<>();
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        // Initialize when system is added
        system.addListener((observable, oldValue, newValue) -> {
            initializeContextMenu(newValue);
            initializeComponentInstanceHandling(newValue);

            systemRoot.setX(Grid.snap(newValue.getBox().getWidth() / 2)); // Place in the middle, horizontally
            systemRootContainer.getChildren().add(new SystemRootPresentation(systemRoot, newValue));
        });
    }

    public SystemModel getSystem() {
        return system.get();
    }

    public void setSystem(final SystemModel system) {
        this.system.setValue(system);
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
                instance.getColorProperty().set(system.getColor());
                instance.getColorIntensityProperty().set(system.getColorIntensity());
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

        contextMenu.addSubMenu("Add Component Instance", componentInstanceSubMenu, 0 * 35);

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
