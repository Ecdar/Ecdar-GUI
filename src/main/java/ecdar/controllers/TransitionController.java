package ecdar.controllers;

import com.jfoenix.controls.JFXRippler;
import ecdar.simulation.Transition;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * The controller class for the transition view element.
 * It represents a single transition and may be used by classes like {@see TransitionPaneElementController}
 * to show a list of transitions
 */
public class TransitionController implements Initializable {
    public AnchorPane root;
    public Label titleLabel;
    public JFXRippler rippler;

    // The transition that the view represents
    private SimpleObjectProperty<Transition> transition = new SimpleObjectProperty<>();
    private SimpleObjectProperty<String> title = new SimpleObjectProperty<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        title.addListener(((observable, oldValue, newValue) -> {
            titleLabel.setText(newValue);
        }));
    }

    public void setTitle(String title) {
        this.title.set(title);
    }

    public void setTransition(Transition transition) {
        this.transition.set(transition);
    }

    public Transition getTransition() {
        return transition.get();
    }
}
