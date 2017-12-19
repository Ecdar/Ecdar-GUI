package SW9.controllers;

import SW9.abstractions.EcdarSystemEdge;
import SW9.abstractions.SystemModel;
import SW9.presentations.DropDownMenu;
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

public class SystemEdgeController implements Initializable {
    public Group root;

    private EcdarSystemEdge edge;
    private final ObjectProperty<SystemModel> system = new SimpleObjectProperty<>();

    private DropDownMenu contextMenu;
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

    public SystemModel getSystem() {
        return system.get();
    }

    public void setSystem(final SystemModel system) {
        this.system.set(system);
    }

    private void initializeDropDownMenu(final SystemModel system) {
        dropDownMenuHelperCircle = new Circle(5);
        dropDownMenuHelperCircle.setOpacity(0);
        dropDownMenuHelperCircle.setMouseTransparent(true);
        root.getChildren().add(dropDownMenuHelperCircle);
    }

    private void showContextMenu() {

        contextMenu = new DropDownMenu((Pane) root.getParent().getParent(), dropDownMenuHelperCircle, 230, true);

        contextMenu.addClickableListElement("Delete", event -> {
            // TODO

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
        }
    }
}
