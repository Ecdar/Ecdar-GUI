package ecdar.controllers;

import com.jfoenix.controls.JFXRippler;
import ecdar.abstractions.Decision;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import java.net.URL;
import java.util.ResourceBundle;

public class DecisionController implements Initializable {
    public JFXRippler rippler;
    public Label decisionDescription;

    private final ObjectProperty<Decision> decision = new SimpleObjectProperty<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        decision.addListener((observable, oldValue, newValue) -> {
            // ToDo NIELS: Add all relevant information to the description
            decisionDescription.setText(newValue.composition + ": " + newValue.action);
        });
    }

    public Decision getDecision() {
        return decision.get();
    }

    public void setDecision(Decision decision) {
        this.decision.set(decision);
    }
}
