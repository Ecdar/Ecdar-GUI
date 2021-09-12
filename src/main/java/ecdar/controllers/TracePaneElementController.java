package ecdar.controllers;

import com.jfoenix.controls.JFXRippler;
import com.uppaal.model.system.SystemLocation;
import com.uppaal.model.system.concrete.ConcreteState;
import ecdar.Ecdar;
import ecdar.presentations.TransitionPresentation;
import ecdar.simulation.EcdarSimulationController;
import ecdar.simulation.EcdarSimulationHandler;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * The controller class for the trace pane element that can be inserted into a simulator pane
 */
public class TracePaneElementController implements Initializable {
    public AnchorPane toolbar;
    public Label traceTitle;
    public JFXRippler expandTrace;
    public VBox traceList;
    public VBox traceVbox;
    public FontIcon expandTraceIcon;
    public AnchorPane traceSummary;
    public Label summaryTitleLabel;
    public Label summarySubtitleLabel;

    private SimpleBooleanProperty isTraceExpanded = new SimpleBooleanProperty(false);
    private Map<ConcreteState, TransitionPresentation> transitionPresentationMap = new LinkedHashMap<>();
    private SimpleIntegerProperty numberOfSteps = new SimpleIntegerProperty(0);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
//        Ecdar.getSimulationHandler().getTraceLog().addListener((ListChangeListener<ConcreteState>) c -> {
//            while (c.next()) {
//                for(final ConcreteState state: c.getAddedSubList()) {
//                    insertTraceState(state, true);
//                }
//
//                for(final ConcreteState state: c.getRemoved()) {
//                    traceList.getChildren().remove(transitionPresentationMap.get(state));
//                    transitionPresentationMap.remove(state);
//                }
//            }
//
//            numberOfSteps.set(transitionPresentationMap.size());
//        });

        initializeTraceExpand();
    }

    /**
     * Initializes the expand functionality that allows the user to show or hide the trace.
     * By default the trace is shown.
     */
    private void initializeTraceExpand() {
        isTraceExpanded.addListener((obs, oldVal, newVal) -> {
            if(newVal) {
                showTrace();
                expandTraceIcon.setIconLiteral("gmi-expand-less");
                expandTraceIcon.setIconSize(24);
            } else {
                hideTrace();
                expandTraceIcon.setIconLiteral("gmi-expand-more");
                expandTraceIcon.setIconSize(24);
            }
        });

        isTraceExpanded.set(true);
    }


    /**
     * Removes all the trace view elements as to hide the trace from the user
     * Also shows the summary view when the trace is hidden
     */
    private void hideTrace() {
        traceList.getChildren().clear();
        traceVbox.getChildren().add(traceSummary);
    }

    /**
     * Shows the trace by inserting a {@link TransitionPresentation} for each trace state
     * Also hides the summary view, since it should only be visible when the trace is hidden
     */
    private void showTrace() {
        transitionPresentationMap.forEach((state, presentation) -> {
            insertTraceState(state, false);
        });
        traceVbox.getChildren().remove(traceSummary);
    }

    /**
     * Instantiates a {@link TransitionPresentation} for a {@link ConcreteState} and adds it to the view
     * @param state The state the should be inserted into the trace log
     * @param shouldAnimate A boolean that indicates whether the trace should fade in when added to the view
     */
    private void insertTraceState(final ConcreteState state, final boolean shouldAnimate) {
        final TransitionPresentation transitionPresentation = new TransitionPresentation();
        transitionPresentationMap.put(state, transitionPresentation);

        transitionPresentation.setOnMouseReleased(event -> {
            event.consume();
            final EcdarSimulationHandler simHandler = Ecdar.getSimulationHandler();
            if (simHandler == null) return;
            //Ecdar.getSimulationHandler().selectTransitionFromLog(state);
        });

        EventHandler mouseEntered = transitionPresentation.getOnMouseEntered();
        transitionPresentation.setOnMouseEntered(event -> {
            EcdarSimulationController.setSelectedState(state);
            mouseEntered.handle(event);
        });

        EventHandler mouseExited = transitionPresentation.getOnMouseExited();
        transitionPresentation.setOnMouseExited(event -> {
            EcdarSimulationController.setSelectedState(null);
            mouseExited.handle(event);
        });


        String title = traceString(state);
        transitionPresentation.getController().setTitle(title);

        // Only insert the presentation into the view if the trace is expanded
        if(isTraceExpanded.get()) {
            traceList.getChildren().add(transitionPresentation);
            if(shouldAnimate) {
                transitionPresentation.playFadeAnimation();
            }
        }
    }

    /**
     * A helper method that returns a string representing a state in the trace log
     * @param state The ConcreteState to represent
     * @return A string representing the state
     */
    private String traceString(ConcreteState state) {
        String title = "(";
        int length = state.getLocations().length;
        for (int i = 0; i < length ; i++) {
            SystemLocation sysLocation = state.getLocations()[i];
            String locationName = sysLocation.getName();
            if (i == length-1) {
                title += locationName;
            } else {
                title += locationName + ", ";
            }
        }
        title += ")";

        return title;
    }

    /**
     * Method to be called when clicking on the expand rippler in the trace toolbar
     */
    @FXML
    private void expandTrace() {
        if(isTraceExpanded.get()) {
            isTraceExpanded.set(false);
        } else {
            isTraceExpanded.set(true);
        }
    }

    public SimpleIntegerProperty getNumberOfStepsProperty() {
        return numberOfSteps;
    }
}
