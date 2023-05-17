package ecdar.controllers;

import com.jfoenix.controls.JFXRippler;
import ecdar.abstractions.State;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * The controller class for the transition view element.
 * It represents a single transition and may be used by classes like {@see StatePaneController}
 * to show a list of transitions
 */
public class StateController implements Initializable {
    public AnchorPane root;
    public Label locations;
    public Label clockConstraints;
    public JFXRippler rippler;

    // The transition that the view represents
    private final SimpleObjectProperty<State> state = new SimpleObjectProperty<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        state.addListener((observable, oldValue, newValue) -> {
            locations.setText(newValue.getStateLocationsString());
            clockConstraints.setText(newValue.getStateClockConstraintsString());
        });
    }

    public void setState(State state) {
        this.state.set(state);
    }

    public State getState() {
        return state.get();
    }
}
