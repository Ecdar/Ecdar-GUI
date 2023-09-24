package ecdar.controllers;

import com.jfoenix.controls.JFXRippler;
import ecdar.abstractions.State;
import ecdar.utility.helpers.ClockConstraintsHandler;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * The controller class for the state view element.
 * It represents a single state within the trace of the simulation
 */
public class StateController implements Initializable {
    public AnchorPane root;
    public Label locations;
    public Text clockConstraintsHeader;
    public Label clockConstraints;
    public JFXRippler rippler;

    // The transition that the view represents
    private final SimpleObjectProperty<State> state = new SimpleObjectProperty<>();

    private final ClockConstraintsHandler clockConstraintsHandler = new ClockConstraintsHandler();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        state.addListener((observable, oldValue, newValue) -> {
            locations.setText(getStateLocationsString(newValue));

            setClockConstraintsSectionVisibility(!newValue.getClockConstraints().isEmpty());
            clockConstraints.setText(clockConstraintsHandler.getStateClockConstraintsString(newValue.getClockConstraints()));
        });
    }

    public void setState(State state) {
        this.state.set(state);
    }

    public State getState() {
        return state.get();
    }

    private void setClockConstraintsSectionVisibility(boolean visibility) {
        clockConstraintsHeader.setVisible(visibility);
        clockConstraintsHeader.setManaged(visibility);
        clockConstraints.setVisible(visibility);
        clockConstraints.setManaged(visibility);
    }

    /**
     * A helper method that returns a string representing the locations of a state in the trace log
     *
     * @return A string representing the locations
     */
    private String getStateLocationsString(State state) {
        StringBuilder locationsString = new StringBuilder();

        int i = 0;
        for (Map.Entry<String, String> componentLocation : state.getComponentLocationMap().entrySet()) {
            locationsString.append(componentLocation.getKey());
            locationsString.append('.');
            locationsString.append(componentLocation.getValue());

            i++;
            if (i < state.getComponentLocationMap().size()) {
                locationsString.append(System.lineSeparator());
            }
        }

        return locationsString.toString();
    }
}
