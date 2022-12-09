package ecdar.controllers;

import com.jfoenix.controls.JFXRippler;
import com.jfoenix.controls.JFXTextField;
import ecdar.Ecdar;
import ecdar.abstractions.Edge;
import ecdar.backend.SimulationHandler;
import ecdar.simulation.Transition;
import ecdar.presentations.TransitionPresentation;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * The controller class for the transition pane element that can be inserted into the simulator panes
 */
public class TransitionPaneElementController implements Initializable {
    public VBox root;
    public VBox transitionList;
    public HBox toolbar;
    public Label toolbarTitle;
    public JFXRippler refreshRippler;
    public JFXRippler expandTransition;
    public FontIcon expandTransitionIcon;
    public VBox delayChooser;
    public JFXTextField delayTextField;

    private SimpleBooleanProperty isTransitionExpanded = new SimpleBooleanProperty(false);
    private Map<Transition, TransitionPresentation> transitionPresentationMap = new HashMap<>();
    private SimpleObjectProperty<BigDecimal> delay = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private SimulationHandler simulationHandler;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        simulationHandler = Ecdar.getSimulationHandler();
        initializeTransitionExpand();
        initializeDelayChooser();
    }

    /**
     * Sets up listeners for the delay chooser.
     * Listens for changes in text property and updates the textfield with a sanitized value (e.g. no letters in delay).
     * Also listens for changes in focus, so there's always a value in the textfield, even if the user deleted the text.
     * Adds tooltip for the textfield.
     */
    private void initializeDelayChooser() {
        delayTextField.textProperty().addListener(((observable, oldValue, newValue) -> {
            delayTextChanged(oldValue, newValue);
        }));

        delayTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            // If the textfield loses focus and the user didn't enter anything
            // show the value 0.0
            if(!newValue && delay.get().equals(BigDecimal.ZERO)) {
                delayTextField.setText("0.0");
            }
        });

        Tooltip.install(delayTextField, new Tooltip("Enter delay to use for next transition"));
    }

    /**
     * Initializes the expand functionality that allows the user to show or hide the transitions.
     * By default the transitions are shown.
     */
    private void initializeTransitionExpand() {
        isTransitionExpanded.addListener((obs, oldVal, newVal) -> {
            if(newVal) {
                if(!root.getChildren().contains(delayChooser)) {
                    // Add the delay chooser just below the toolbar
                    root.getChildren().add(1, delayChooser);
                }
                showTransitions();
                expandTransitionIcon.setIconLiteral("gmi-expand-less");
                expandTransitionIcon.setIconSize(24);
            } else {
                root.getChildren().remove(delayChooser);
                hideTransitions();
                expandTransitionIcon.setIconLiteral("gmi-expand-more");
                expandTransitionIcon.setIconSize(24);
            }
        });

        isTransitionExpanded.set(true);
    }

    /**
     * Removes all the transition view elements as to hide the transitions from the user
     */
    private void hideTransitions() {
        transitionList.getChildren().clear();
    }

    /**
     * Shows the available transitions by inserting a {@link TransitionPresentation} for each transition
     */
    private void showTransitions() {
        transitionPresentationMap.forEach((transition, presentation) -> {
            insertTransition(transition);
        });
    }

    /**
     * Instantiates a TransitionPresentation for a Transition and adds it to the view
     * @param transition The transition that should be inserted into the view
     */
    private void insertTransition(Transition transition) {
        final TransitionPresentation transitionPresentation = new TransitionPresentation();
        String title = transitionString(transition);
        transitionPresentation.getController().setTitle(title);
        transitionPresentation.getController().setTransition(transition);

        // Update the selected transition when mouse entered.
        // Add the event to existing mouseEntered events
        // e.g. TransitionPresentation already has mouseEntered functionality and we want to keep it
        EventHandler mouseEntered = transitionPresentation.getOnMouseEntered();
        // transitionPresentation.setOnMouseEntered(event -> {
        //     SimulatorController.setSelectedTransition(transitionPresentation.getController().getTransition());
        //     mouseEntered.handle(event);
        // });

        EventHandler mouseExited = transitionPresentation.getOnMouseExited();
        transitionPresentation.setOnMouseExited(event -> {
            mouseExited.handle(event);
        });

        transitionPresentationMap.put(transition, transitionPresentation);

        // Only insert the presentation into the view if the transitions are expanded
        // Avoids inserting duplicate elements in the view (it's still added to the map)
        if(isTransitionExpanded.get()) {
            transitionList.getChildren().add(transitionPresentation);
        }
    }

    /**
     * A helper method that returns a string representing a transition in the transition chooser
     * @param transition The {@link Transition} to represent
     * @return A string representing the transition
     */
    private String transitionString(Transition transition) {
        String title = transition.getLabel();
        if(transition.getEdges() != null) {
            for (Edge edge : transition.getEdges()) {
                title += " " + edge.getId();
            }
        }
        return title;
    }

    /**
     * Method to be called when clicking on the expand rippler in the transition toolbar
     */
    @FXML
    private void expandTransitions() {
        if(isTransitionExpanded.get()) {
            isTransitionExpanded.set(false);
        } else {
            isTransitionExpanded.set(true);
        }
    }

    /**
     * Restart simulation to the initial step.
     */
    @FXML
    private void restartSimulation() {
        simulationHandler.resetToInitialLocation();
    }

    /**
     * Sanitizes the input that the user inserts into the delay textfield.
     * Checks if the text can be converted into a BigDecimal otherwise show the previous value.
     * For example avoids users entering letters.
     * @param oldValue The old value to show if the newvalue cannot be converted to a BigDecimal
     * @param newValue The new value to show in the textfield
     */
    @FXML
    private void delayTextChanged(String oldValue, String newValue) {
        // If the value is empty (the user deleted the value), assume that the value is 0.0 but do not update the text
        if(newValue.isEmpty()) {
            this.delay.set(BigDecimal.ZERO);
        } else {
            // Try to convert the new value into a BigDecimal
            // Note that we don't setText here, as the new value is already shown in the textfield
            try {
                BigDecimal bd = new BigDecimal(newValue);

                // Checking the string for "-" instead of whether bd is negative is due to the case of -0.0
                // So checking the string is just simpler
                if(newValue.contains("-")) {
                    throw new NumberFormatException();
                }

                this.delay.set(bd);

            } catch (NumberFormatException ex) {
                // If the conversion was not possible, show the old value
                this.delayTextField.setText(oldValue);
            }
        }

    }

}
