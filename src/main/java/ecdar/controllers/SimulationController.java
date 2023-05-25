package ecdar.controllers;

import EcdarProtoBuf.ObjectProtos;
import EcdarProtoBuf.QueryProtos;
import ecdar.Ecdar;
import ecdar.backend.BackendHelper;
import ecdar.presentations.*;
import ecdar.utility.helpers.SimulationHighlighter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;

import ecdar.abstractions.*;
import ecdar.abstractions.State;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableMap;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.*;

public class SimulationController implements ModeController, Initializable {
    public StackPane root;
    public StackPane toolbar;
    public ScrollPane scrollPane;
    public Group groupContainer;
    public FlowPane processContainer;

    public final LeftSimPanePresentation leftSimPane = new LeftSimPanePresentation();
    public final RightSimPanePresentation rightSimPane = new RightSimPanePresentation(this::nextStep);

    private static final ObjectProperty<Simulation> activeSimulation = new SimpleObjectProperty<>(null);
    private static final ObjectProperty<State> selectedState = new SimpleObjectProperty<>();

    private SimulationHighlighter highlighter;

    /**
     * The amount that is going be zoomed in/out for each press on + or -
     */
    private final double SCALE_DELTA = 1.1;

    /**
     * The max that the user can zoom in
     */
    private static final double MAX_ZOOM_IN = 1.6;

    /**
     * The max that the user can zoom in
     */
    private static final double MAX_ZOOM_OUT = 0.5;

    /**
     * Offset such that the view does not overlap with the scroll bar on the right-hand side.
     */
    private static final int SCROLLPANE_OFFSET = 20;

    /**
     * Is true if a reset of the zoom have been requested, false if not.
     */
    private boolean resetZoom = false;
    private boolean isMaxZoomInReached = false;
    private boolean isMaxZoomOutReached = false;

    private final EventHandler<MouseEvent> decisionOnMouseEnter = event -> highlighter.highlightDecisionEdges(((DecisionPresentation)event.getTarget()));
    private final EventHandler<MouseEvent> decisionOnMouseExit = event -> highlighter.unhighlightEdges();

    private final ObservableMap<String, ProcessPresentation> componentNameProcessPresentationMap = FXCollections.observableHashMap();

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        //In case that the processContainer gets moved around we have to keep it in place.
        initializeProcessContainer();
        initializeWindowResizing();
        initializeZoom();
        highlighter = new SimulationHighlighter(componentNameProcessPresentationMap, activeSimulation);
    }

    /**
     * Initializes the {@link #processContainer} with its correct styling, and placement on the view.
     * It also adds a {@link ListChangeListener} on the list of simulated components where it adds the
     * {@link Component}<code>s</code> which are needed to the <code>processContainer</code>.
     */
    private void initializeProcessContainer() {
        processContainer.translateXProperty().addListener((observable, oldValue, newValue) -> {
            processContainer.setTranslateX(0);
        });
        //Sets the space between the processes
        processContainer.setHgap(10);
        processContainer.setVgap(10);

        // padding to the scrollpane
        processContainer.setPadding(new Insets(5));
    }

    /**
     * Initializes the zoom functionality in {@link #processContainer}
     */
    private void initializeZoom() {
        processContainer.scaleXProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() > MAX_ZOOM_IN) isMaxZoomInReached = true;
            if (newValue.doubleValue() < MAX_ZOOM_OUT) isMaxZoomOutReached = true;

            handleWidthOnScale(oldValue, newValue);
        });
    }

    /**
     * Initializes listener for change of width in {@link #scrollPane} which also affects {@link #processContainer} <br />
     * This does also take the zooming into account when doing the resizing.
     */
    private void initializeWindowResizing() {
        scrollPane.widthProperty().addListener((observable, oldValue, newValue) -> {
            final double width = (newValue.doubleValue()) * (1 + (1 - processContainer.getScaleX()));
            if (processContainer.getScaleX() > 1) { //Zoomed in
                processContainer.setMinWidth(width);
                processContainer.setMaxWidth(width);
            } else if (processContainer.getScaleX() < 1) { //Zoomed out
                processContainer.setMinWidth(width);
                processContainer.setMaxWidth(width);
                final double deltaWidth = newValue.doubleValue() - groupContainer.layoutBoundsProperty().get().getWidth();
                processContainer.setMinWidth(processContainer.getWidth() + (deltaWidth - SCROLLPANE_OFFSET) * (1 + (1 - processContainer.getScaleX())));
                processContainer.setMaxWidth(processContainer.getWidth() + (deltaWidth - SCROLLPANE_OFFSET) * (1 + (1 - processContainer.getScaleX())));
            } else { // Reset
                processContainer.setMinWidth(newValue.doubleValue() - SCROLLPANE_OFFSET);
                processContainer.setMaxWidth(newValue.doubleValue() - SCROLLPANE_OFFSET);
            }
        });
    }

    /**
     * Initializes the simulation based on the received proto response and current composition
     *
     * @param response ProtoBuf response received from a step request
     * @param composition current composition being simulated
     */
    private void initializeActiveSimulation(QueryProtos.SimulationStepResponse response, String composition) {
        State newState = createStateFromResponse(response);

        Simulation newSimulation = new Simulation(composition, newState);
        activeSimulation.set(newSimulation);

        newSimulation.simulatedComponents.forEach(component -> {
            var processPresentation = new ProcessPresentation(component);
            componentNameProcessPresentationMap.put(component.getName(), processPresentation);
            processContainer.getChildren().addAll(processPresentation);
        });

        leftSimPane.getController().setTraceLog(newSimulation.traceLog);
        rightSimPane.getController().setDecisionsList(newSimulation.availableDecisions);

        updateSimulationState(response.getNewDecisionPointsList());

        // Update highlighting when state changes
        newSimulation.currentState.addListener(observable -> {
            updateSimulationVariablesAndClocks();
            highlighter.updateHighlighting();
            highlighter.fadeSucceedingTraceStates(leftSimPane.getController().tracePanePresentation.getController().traceList.getChildren());
        });

        rightSimPane.getController().availableDecisionsVBox.getChildren().addListener((ListChangeListener<? super Node>) c -> {
            while (c.next()) {
                c.getAddedSubList().forEach(decisionPresentation -> {
                    decisionPresentation.setOnMouseEntered(decisionOnMouseEnter);
                    decisionPresentation.setOnMouseExited(decisionOnMouseExit);
                });
            }
        });
    }

    // ToDo NIELS: Remove static
    public static State getCurrentState() throws NullPointerException {
        return activeSimulation.get().currentState.get();
    }

    public static List<Component> getSimulatedComponents() {
        return activeSimulation.get().simulatedComponents;
    }

    private Simulation getActiveSimulation() {
        return activeSimulation.get();
    }

    private List<ProcessPresentation> getProcessPresentations() {
        return new ArrayList<>(componentNameProcessPresentationMap.values());
    }

    /**
     * Reloads the whole simulation sets the initial transitions, states, etc
     *
     * @param composition current composition being simulated
     */
    public void initialStep(String composition) {
        BackendHelper.getDefaultEngine().enqueueRequest(new Decision(composition),
                (response) -> Platform.runLater(() -> initializeActiveSimulation(response, composition)),
                (error) -> Ecdar.showToast("Could not start simulation:\n" + error.getMessage()));
    }

    /**
     * Take a step in the simulation.
     *
     * @param decision basis of the step to take
     */
    public void nextStep(Decision decision) {
        // removes invalid states from the log when stepping forward after previewing a previous state
        removeStatesFromLog(getActiveSimulation().traceLog.filtered(statePresentation -> statePresentation.getController().getState() == getActiveSimulation().currentState.get()).stream().findFirst().orElse(null));

        BackendHelper.getDefaultEngine().enqueueRequest(decision,
                (response) -> {
                    // ToDo: This is temp solution to compile but should be fixed to handle ambiguity
                    State newState = createStateFromResponse(response);
                    getActiveSimulation().currentState.set(newState);
                    updateSimulationState(response.getNewDecisionPointsList());
                },
                (error) -> Ecdar.showToast("Could not take next step in simulation\nError: " + error.getMessage()));
    }

    /**
     * Resets the current simulation, and prepares for a new simulation by clearing the
     * {@link SimulationController#processContainer} and adding the processes of the new simulation.
     */
    public void resetSimulation(String queryToSimulate) {
        clearOverview();
        initialStep(queryToSimulate);
    }

    private void previewState(final StatePresentation statePresentation) throws NullPointerException {
        getActiveSimulation().currentState.set(statePresentation.getController().getState());
        highlighter.fadeSucceedingTraceStates(leftSimPane.getController().tracePanePresentation.getController().traceList.getChildren());
    }

    private void updateSimulationState(List<ObjectProtos.Decision> availableDecisions) {
        Platform.runLater(() -> {
            getActiveSimulation().addStateToTraceLog(getActiveSimulation().currentState.get(), this::previewState);

            getActiveSimulation().availableDecisions.clear();
            if (availableDecisions.isEmpty()) {
                // If no edges are available in any of the returned decisions
                Ecdar.showToast("No available decisions.");
            } else {
                getActiveSimulation().addDecisionsFromProto(availableDecisions);
            }

            updateSimulationVariablesAndClocks();
            highlighter.updateHighlighting();
            highlighter.fadeSucceedingTraceStates(leftSimPane.getController().tracePanePresentation.getController().traceList.getChildren());
        });
    }

    /**
     * Setup listeners for displaying clock and variable values on the {@link ProcessPresentation}
     */
    private void updateSimulationVariablesAndClocks() {
        getActiveSimulation().variables.forEach((var, val) -> {
            if (var.equals("t(0)")) {// As t(0) does not belong to any process
                final String[] spittedString = var.split("\\.");
                // If the process containing the var is not there we just skip it
                if (spittedString.length > 0 && componentNameProcessPresentationMap.size() > 0) {
                    componentNameProcessPresentationMap.entrySet().stream()
                            .filter(processPair -> processPair.getKey().equals(spittedString[0]))
                            .findFirst().get().getValue().getController().getVariables()
                            .put(spittedString[1], val);
                }
            }
        });

        if (componentNameProcessPresentationMap.size() == 0) return;
        getActiveSimulation().clocks.forEach((clock, val) -> {
            if (!clock.equals("t(0)")) {// As t(0) does not belong to any process
                final String[] spittedString = clock.split("\\.");
                // If the process containing the clock is not there we just skip it
                if (spittedString.length > 0 && componentNameProcessPresentationMap.size() > 0) {
                    componentNameProcessPresentationMap.entrySet().stream()
                            .filter(processPair -> processPair.getKey().equals(spittedString[0]))
                            .findFirst().get().getValue().getController().getClocks()
                            .put(spittedString[1], val);
                }
            }
        });
    }

    /**
     * Clears the {@link #processContainer} and the {@link #componentNameProcessPresentationMap}.
     */
    private void clearOverview() {
        processContainer.getChildren().clear();
        componentNameProcessPresentationMap.clear();
    }

    /**
     * Handles the scaling of the width of the {@link #processContainer}
     *
     * @param oldValue the width of {@link #scrollPane} before the change
     * @param newValue the width of {@link #scrollPane} after the change
     */
    private void handleWidthOnScale(final Number oldValue, final Number newValue) {
        if (resetZoom) { //Zoom reset
            resetZoom = false;
            processContainer.setMinWidth(scrollPane.getWidth() - SCROLLPANE_OFFSET);
            processContainer.setMaxWidth(scrollPane.getWidth() - SCROLLPANE_OFFSET);
        } else if (oldValue.doubleValue() > newValue.doubleValue()) { //Zoom in
            resetZoom = false;
            processContainer.setMinWidth(Math.round(processContainer.getWidth() * SCALE_DELTA));
            processContainer.setMaxWidth(Math.round(processContainer.getWidth() * SCALE_DELTA));
        } else { // Zoom out
            resetZoom = false;
            processContainer.setMinWidth(Math.round(processContainer.getWidth() * (1 / SCALE_DELTA)));
            processContainer.setMaxWidth(Math.round(processContainer.getWidth() * (1 / SCALE_DELTA)));
        }
    }

    /**
     * Removes all states from the trace log, succeeding the given state
     */
    private void removeStatesFromLog(StatePresentation statePresentation) {
        if (statePresentation == null) return;

        var newLastStateIndex = getActiveSimulation().traceLog.indexOf(statePresentation);
        getActiveSimulation().traceLog.remove(newLastStateIndex + 1, getActiveSimulation().traceLog.size());
    }

    private State createStateFromResponse(QueryProtos.SimulationStepResponse response) {
        return new State(response.getNewDecisionPoints(0).getSource()); // ToDo NIELS: Each source is only a subset, we should combine them to the full state
    }

    public static void setSelectedState(State selectedState) {
        SimulationController.selectedState.set(selectedState); // ToDo NIELS: Handle selection
    }

    public static String getComposition() {
        return activeSimulation.get().composition;
    }

    @Override
    public StackPane getLeftPane() {
        return leftSimPane;
    }

    @Override
    public StackPane getRightPane() {
        return rightSimPane;
    }
}
