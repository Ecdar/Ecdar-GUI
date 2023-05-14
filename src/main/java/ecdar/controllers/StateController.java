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
    public Label locationsLabel;
    public Label clocksLabel;
    public JFXRippler rippler;

    // The transition that the view represents
    private final SimpleObjectProperty<State> state = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<String> locationsString = new SimpleObjectProperty<>();
    private final SimpleObjectProperty<String> clocksString = new SimpleObjectProperty<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        locationsString.addListener(((observable, oldValue, newValue) -> {
            locationsLabel.setText(newValue);
        }));

        clocksString.addListener(((observable, oldValue, newValue) -> {
            clocksLabel.setText(newValue);
        }));
    }

    public void setLocationsString(String locationsString) {
        this.locationsString.set(locationsString);
    }

    public void setClocksString(String clocksString) {
        this.clocksString.set(clocksString);
    }

    public void setState(State state) {
        this.state.set(state);
    }

    public State getState() {
        return state.get();
    }
}
