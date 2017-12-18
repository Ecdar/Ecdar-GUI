package SW9.controllers;

import SW9.abstractions.EcdarSystemEdge;
import SW9.abstractions.SystemModel;
import SW9.presentations.CanvasPresentation;
import SW9.presentations.DropDownMenu;
import SW9.presentations.MenuElement;
import SW9.utility.colors.Color;
import SW9.utility.helpers.ItemDragHelper;
import SW9.utility.helpers.SelectHelper;
import SW9.utility.keyboard.Keybind;
import SW9.utility.keyboard.KeyboardTracker;
import com.jfoenix.controls.JFXPopup;
import com.uppaal.model.system.SystemEdge;
import javafx.beans.property.DoubleProperty;
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
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

public class SystemEdgeController implements Initializable {
    public Group root;
    public StackPane contextMenuContainer;
    public Group contextMenuSource;

    private EcdarSystemEdge edge;
    private final ObjectProperty<SystemModel> system = new SimpleObjectProperty<>();
    private DropDownMenu contextMenu;

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
        contextMenu = new DropDownMenu((Pane) contextMenuContainer.getParent(), contextMenuSource, 230, true);

        DropDownMenu.x = CanvasPresentation.mouseTracker.getGridX();
        DropDownMenu.y = CanvasPresentation.mouseTracker.getGridY();

        contextMenu.addClickableListElement("Delete", event -> {
            // TODO

            contextMenu.close();
        });
    }

    @FXML
    private void onMouseClicked(final MouseEvent event) {
        event.consume();

        // If secondary clicked, show context menu
        if (event.getButton().equals(MouseButton.SECONDARY)) {
            contextMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 0, 0);
        }
    }
}
