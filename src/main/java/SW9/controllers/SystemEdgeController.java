package SW9.controllers;

import SW9.abstractions.EcdarSystem;
import SW9.abstractions.EcdarSystemEdge;
import SW9.presentations.DropDownMenu;
import SW9.utility.helpers.SelectHelper;
import SW9.utility.keyboard.Keybind;
import SW9.utility.keyboard.KeyboardTracker;
import com.jfoenix.controls.JFXPopup;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for a system edge.
 */
public class SystemEdgeController implements Initializable {
    public Group root;

    private EcdarSystemEdge edge;
    private final ObjectProperty<EcdarSystem> system = new SimpleObjectProperty<>();
    private SelectHelper.ItemSelectable selectable;

    private Circle dropDownMenuHelperCircle;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        system.addListener((observable, oldValue, newValue) -> {

            // TODO only if edge is unfinished
            KeyboardTracker.registerKeybind(KeyboardTracker.ABANDON_EDGE, new Keybind(new KeyCodeCombination(KeyCode.ESCAPE), () -> {
                newValue.removeEdge(getEdge());
                KeyboardTracker.unregisterKeybind(KeyboardTracker.ABANDON_EDGE);
            }));


            initializeDropDownMenu(newValue);
        });
    }

    public EcdarSystemEdge getEdge() {
        return edge;
    }

    public void setEdge(final EcdarSystemEdge edge) {
        this.edge = edge;
    }


    // System

    public EcdarSystem getSystem() {
        return system.get();
    }

    public void setSystem(final EcdarSystem system) {
        this.system.set(system);
    }

    private void initializeDropDownMenu(final EcdarSystem system) {
        dropDownMenuHelperCircle = new Circle(5);
        dropDownMenuHelperCircle.setOpacity(0);
        dropDownMenuHelperCircle.setMouseTransparent(true);
        root.getChildren().add(dropDownMenuHelperCircle);
    }

    /**
     * Shows a context menu.
     * This method creates the menu object itself
     * (rather than having it be created in an initialize method),
     * since the parent of the root is not yet defined when initializing.
     */
    private void showContextMenu() {
        final DropDownMenu contextMenu = new DropDownMenu((Pane) root.getParent().getParent(), dropDownMenuHelperCircle, 230, true);

        contextMenu.addClickableListElement("Delete", event -> {
            getSystem().removeEdge(getEdge());

            contextMenu.close();
        });

        contextMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 0, 0);
    }

    @FXML
    private void onMouseClicked(final MouseEvent event) {
        event.consume();

        // If secondary clicked, show context menu
        if (event.getButton().equals(MouseButton.SECONDARY)) {
            dropDownMenuHelperCircle.setLayoutX(event.getX());
            dropDownMenuHelperCircle.setLayoutY(event.getY());

            showContextMenu();
            return;
        }

        // If primary clicked, select
        if (event.getButton().equals(MouseButton.PRIMARY)) {
            if (event.isShortcutDown()) {
                SelectHelper.addToSelection(selectable);
            } else {
                SelectHelper.select(selectable);
            }
        }
    }

    public void setSelectable(final SelectHelper.ItemSelectable selectable) {
        this.selectable = selectable;
    }
}
