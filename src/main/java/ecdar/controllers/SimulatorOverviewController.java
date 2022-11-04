package ecdar.controllers;

import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.presentations.ProcessPresentation;
import ecdar.simulation.SimulationState;
import ecdar.simulation.Transition;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.beans.InvalidationListener;
import javafx.collections.*;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.util.Pair;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * The controller of the middle part of the simulator.
 * It is here where processes of a simulation will be shown.
 */
public class SimulatorOverviewController implements Initializable {
    public AnchorPane root;
    public ScrollPane scrollPane;
    public FlowPane processContainer;
    public Group groupContainer;

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
     * Offset such that the view does not overlap with the scroll bar on the right hand sig.
     */
    private static final int SUPER_SPECIAL_SCROLLPANE_OFFSET = 20;

    private final ObservableList<Component> componentArrayList = FXCollections.observableArrayList();
    private final ObservableMap<String, ProcessPresentation> processPresentations = FXCollections.observableHashMap();

    /**
     * Is true if a reset of the zoom have been requested, false if not.
     */
    private boolean resetZoom = false;
    private boolean isMaxZoomInReached = false;
    private boolean isMaxZoomOutReached = false;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        groupContainer = new Group();
        processContainer = new FlowPane();
        //In case that the processContainer gets moved around we have to keep in into place.
        initializeProcessContainer();

        initializeWindowResizing();
        initializeZoom();
        initializeHighlighting();
        initializeSimulationVariables();
        // Add the processes and group to the view
        addProcessesToGroup();
        scrollPane.setContent(groupContainer);
    }

    /**
     * Initializes the {@link #processContainer} with its correct styling, and placement on the view.
     * It also adds a {@link ListChangeListener} on {@link #componentArrayList} where it adds the
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

        componentArrayList.addListener((ListChangeListener<Component>) c -> {
            final Map<String, ProcessPresentation> processes = new HashMap<>();
            while (c.next()) {
                if (c.wasRemoved()) {
                    clearOverview();
                } else {
                    c.getAddedSubList().forEach(o -> processes.put(o.getName(), new ProcessPresentation(o)));
                }
            }
            // Highlight the current state when the processes change
            highlightProcessState(Ecdar.getSimulationHandler().getCurrentState()); // ToDo NIELS: Throws NullPointerException inside method due to currentState
            processContainer.getChildren().addAll(processes.values());
            processPresentations.putAll(processes);
        });

        final Map<String, ProcessPresentation> processes = new HashMap<>();
        componentArrayList.forEach(o -> processes.put(o.getName(), new ProcessPresentation(o)));

        processContainer.getChildren().addAll(processes.values());
        processPresentations.putAll(processes);
    }

    /**
     * Clears the {@link #processContainer} and the {@link #processPresentations}.
     */
    void clearOverview() {
        processContainer.getChildren().clear();
        processPresentations.clear();
    }

    /**
     * Setup listeners for displaying clock and variable values on the {@link ProcessPresentation}
     */
    private void initializeSimulationVariables() {
        Ecdar.getSimulationHandler().getSimulationVariables().addListener((InvalidationListener) obs -> {
            Ecdar.getSimulationHandler().getSimulationVariables().forEach((s, bigDecimal) -> {
                if (!s.equals("t(0)")) {// As t(0) does not belong to any process
                    final String[] spittedString = s.split("\\.");
                    // If the process containing the var is not there we just skip it
                    if (spittedString.length > 0 && processPresentations.size() > 0) {
                        processPresentations.get(spittedString[0]).getController().getVariables().put(spittedString[1], bigDecimal);
                    }
                }
            });
        });
        Ecdar.getSimulationHandler().getSimulationClocks().addListener((InvalidationListener) obs -> {
            if (processPresentations.size() == 0) return;
            Ecdar.getSimulationHandler().getSimulationClocks().forEach((s, bigDecimal) -> {
                if (!s.equals("t(0)")) {// As t(0) does not belong to any process
                    final String[] spittedString = s.split("\\.");
                    // If the process containing the clock is not there we just skip it
                    if (spittedString.length > 0 && processPresentations.size() > 0) {
                        processPresentations.get(spittedString[0]).getController().getClocks().put(spittedString[1], bigDecimal);
                    }
                }
            });
        });
    }

    /**
     * Removes {@link #processContainer} from the {@link #groupContainer}. <br />
     * In this way the {@link Component}<code>s</code> in the <code>processContainer</code> will then again be resizable,
     * as the class {@link Group} makes its children not resizeable.
     *
     * @see Group
     */
    void removeProcessesFromGroup() {
        groupContainer.getChildren().removeAll(processContainer);
    }

    /**
     * Adds the {@link #processContainer} to the {@link #groupContainer}. <br />
     * This method is usually needed to called if {@link #removeProcessesFromGroup()} have been called, or
     * if the <code>processContainer</code> just need to be added to the <code>groupContainer</code>.<br />
     * This method makes sure that the <code>processContainer</code> will be added to the <code>groupContainer</code>
     * which is needed to show the {@link ProcessPresentation}<code>s</code> in the {@link #scrollPane}.
     * If the <code>processContainer</code> is already contained in the <code>groupContainer</code>
     * the method does nothing but return.
     *
     * @see #removeProcessesFromGroup()
     */
    void addProcessesToGroup() {
        if (groupContainer.getChildren().contains(processContainer)) return;
        groupContainer.getChildren().add(processContainer);
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

        // to support pinch zooming
        //TODO this should be fixed at as it does not work as it should
        /*
        processContainer.setOnZoom(event -> {
            //Tries to zoom in/out but max is reached
            if(event.getZoomFactor() >= 1 && isMaxZoomInReached) return;
            if(event.getZoomFactor() < 1 && isMaxZoomOutReached) return;

            isMaxZoomInReached = false;
            isMaxZoomOutReached = false;

            processContainer.setScaleX(processContainer.getScaleX() * event.getZoomFactor());
            processContainer.setScaleY(processContainer.getScaleY() * event.getZoomFactor());
        });*/
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
                processContainer.setMinWidth(processContainer.getWidth() + (deltaWidth - SUPER_SPECIAL_SCROLLPANE_OFFSET) * (1 + (1 - processContainer.getScaleX())));
                processContainer.setMaxWidth(processContainer.getWidth() + (deltaWidth - SUPER_SPECIAL_SCROLLPANE_OFFSET) * (1 + (1 - processContainer.getScaleX())));
            } else { // Reset
                processContainer.setMinWidth(newValue.doubleValue() - SUPER_SPECIAL_SCROLLPANE_OFFSET);
                processContainer.setMaxWidth(newValue.doubleValue() - SUPER_SPECIAL_SCROLLPANE_OFFSET);
            }
        });
    }

    /**
     * Increments the {@link #processContainer} scaleX and scaleY properties
     * which creates the zoom-in feeling. Resizing of the view is handled by {@link #handleWidthOnScale(Number, Number)}
     *
     * @see FlowPane#scaleXProperty()
     * @see FlowPane#scaleYProperty()
     */
    void zoomIn() {
        if (isMaxZoomInReached) return;
        isMaxZoomOutReached = false;
        processContainer.setScaleX(processContainer.getScaleX() * SCALE_DELTA);
        processContainer.setScaleY(processContainer.getScaleY() * SCALE_DELTA);
    }


    /**
     * Decrements the {@link #processContainer} scaleX and scaleY properties
     * which creates the zoom-in feeling. Resizing of the view is handled by {@link #handleWidthOnScale(Number, Number)}
     *
     * @see FlowPane#scaleXProperty()
     * @see FlowPane#scaleYProperty()
     */
    void zoomOut() {
        if (isMaxZoomOutReached) return;
        isMaxZoomInReached = false;
        processContainer.setScaleX(processContainer.getScaleX() * (1 / SCALE_DELTA));
        processContainer.setScaleY(processContainer.getScaleY() * (1 / SCALE_DELTA));
    }

    /**
     * Resets the scaling of the {@link #processContainer}, and hereby the zoom
     *
     * @see FlowPane#scaleXProperty()
     * @see FlowPane#scaleYProperty()
     */
    void resetZoom() {
        if (processContainer.getScaleX() == 1) return;
        resetZoom = true;
        isMaxZoomInReached = false;
        isMaxZoomOutReached = false;
        processContainer.setScaleX(1);
        processContainer.setScaleY(1);
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
            processContainer.setMinWidth(scrollPane.getWidth() - SUPER_SPECIAL_SCROLLPANE_OFFSET);
            processContainer.setMaxWidth(scrollPane.getWidth() - SUPER_SPECIAL_SCROLLPANE_OFFSET);
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
     * Initializer method to setup listeners that handle highlighting when selected/current state/transition changes
     */
    private void initializeHighlighting() {
        SimulatorController.getSelectedTransitionProperty().addListener((observable, oldTransition, newTransition) -> {
            unhighlightProcesses();

            // If the new transition is not null, we want to highlight the locations and edges in the new value
            // otherwise we highlight the current state
            if (newTransition != null) {
                highlightProcessTransition(newTransition);
            } else {
                highlightProcessState(Ecdar.getSimulationHandler().getCurrentState());
            }
        });

        SimulatorController.getSelectedStateProperty().addListener((observable, oldState, newState) -> {
            unhighlightProcesses();

            // If the new state is not null, we want to highlight the locations in the new value
            // otherwise we highlight the current state
            if (newState != null) {
                highlightProcessState(newState);
            } else {
                highlightProcessState(Ecdar.getSimulationHandler().getCurrentState());
            }
        });
    }

    /**
     * Highlights all the processes involved in the transition.
     * Finds the processes involved in the transition (processes with edges in the transition) and highlights their edges
     * Also fades processes that are not active in the selected transition
     *
     * @param transition The transition for which we highlight the involved processes
     */
    public void highlightProcessTransition(final Transition transition) {
        final var edges = transition.getEdges();

        // List of all processes to show as inactive if they are not involved in a transition
        // Processes are removed from this list, if they have an edge in the transition
        final ArrayList<ProcessPresentation> processesToHide = new ArrayList<>(processPresentations.values());

        for (final ProcessPresentation processPresentation : processPresentations.values()) {

            // Find the processes that have edges involved in this transition
            processPresentation.getController().highlightEdges(edges);
            processesToHide.remove(processPresentation);
        }

        processesToHide.forEach(ProcessPresentation::showInactive);
    }

    /**
     * Unhighlights all processes
     */
    public void unhighlightProcesses() {
        for (final ProcessPresentation presentation : processPresentations.values()) {
            presentation.getController().unhighlightProcess();
            presentation.showActive();
        }
    }

    /**
     * Finds the processes for the input locations in the input {@link SimulationState} and highlights the locations.
     *
     * @param state The state with the locations to highlight
     */
    public void highlightProcessState(final SimulationState state) {
        if (state == null) return;
        for (int i = 0; i < state.getLocations().size(); i++) {
            final Pair<String, String> loc = state.getLocations().get(i);

            for (final ProcessPresentation presentation : processPresentations.values()) {
                final String processName = presentation.getController().getComponent().getName();

                if (processName.equals(loc.getKey())) {
                    presentation.getController().highlightLocation(loc.getValue());
                }
            }
        }
    }

    public ObservableList<Component> getComponentObservableList() {
        return componentArrayList;
    }
}
