package SW9.controllers;

import SW9.abstractions.EcdarSystemEdge;
import SW9.abstractions.SystemModel;
import SW9.utility.colors.Color;
import SW9.utility.helpers.ItemDragHelper;
import SW9.utility.helpers.SelectHelper;
import SW9.utility.keyboard.Keybind;
import SW9.utility.keyboard.KeyboardTracker;
import com.uppaal.model.system.SystemEdge;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;

import java.net.URL;
import java.util.ResourceBundle;

public class SystemEdgeController implements Initializable {
    public Group root;
    private EcdarSystemEdge edge;
    private final ObjectProperty<SystemModel> system = new SimpleObjectProperty<>();

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        system.addListener((observable, oldValue, newValue) -> {
            KeyboardTracker.registerKeybind(KeyboardTracker.ABANDON_EDGE, new Keybind(new KeyCodeCombination(KeyCode.ESCAPE), () -> {
                newValue.removeEdge(getEdge());
                KeyboardTracker.unregisterKeybind(KeyboardTracker.ABANDON_EDGE);
            }));
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
}
