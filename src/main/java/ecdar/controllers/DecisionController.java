package ecdar.controllers;

import com.jfoenix.controls.JFXRippler;
import ecdar.abstractions.Decision;
import ecdar.utility.helpers.ClockConstraintsHandler;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class DecisionController implements Initializable {
    public JFXRippler rippler;
    public Label action;
    public Label clockConstraints;

    private final ObjectProperty<Decision> decision = new SimpleObjectProperty<>();
    private final ClockConstraintsHandler clockConstraintsHandler = new ClockConstraintsHandler();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        decision.addListener((observable, oldValue, newValue) -> {
            action.setText(newValue.action);
            clockConstraints.setText(
                    clockConstraintsHandler.getStateClockConstraintsString(newValue.clockConstraints)
            );
        });
    }

    public Decision getDecision() {
        return decision.get();
    }

    public void setDecision(Decision decision) {
        this.decision.set(decision);
    }
}
