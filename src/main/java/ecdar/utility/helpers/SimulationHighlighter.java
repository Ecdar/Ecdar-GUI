package ecdar.utility.helpers;

import EcdarProtoBuf.ObjectProtos;
import ecdar.abstractions.Simulation;
import ecdar.abstractions.State;
import ecdar.presentations.DecisionPresentation;
import ecdar.presentations.ProcessPresentation;
import ecdar.presentations.StatePresentation;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableMap;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class SimulationHighlighter {
    // ToDo NIELS: Clean up by removing extraneous methods
    private final ObservableMap<String, ProcessPresentation> componentNameProcessPresentationMap;
    private final ObjectProperty<Simulation> simulation = new SimpleObjectProperty<>();
    private ArrayList<String> temporarilyHighlightedLocations = new ArrayList<>();

    public SimulationHighlighter(ObservableMap<String, ProcessPresentation> componentNameProcessPresentationMap, ObjectProperty<Simulation> simulation) {
        this.componentNameProcessPresentationMap = componentNameProcessPresentationMap;
        this.simulation.bind(simulation);
    }

    private Simulation getActiveSimulation() {
        return simulation.get();
    }

    private List<ProcessPresentation> getProcessPresentations() {
        return new ArrayList<>(componentNameProcessPresentationMap.values());
    }

    /**
     * Initializer method to set up listeners that handle highlighting when selected/current state/transition changes
     */
    public void updateHighlighting() {
        Platform.runLater(() -> {
            unhighlightProcesses();
            highlightProcessState(getActiveSimulation().currentState.get());
        });
    }

    /**
     * Unhighlights all processes
     */
    public void unhighlightProcesses() {
        for (final ProcessPresentation presentation : getProcessPresentations()) {
            presentation.getController().unhighlightProcess();
            presentation.showActive();
        }
    }

    /**
     * Initializes the fading of states in the trace list when a state is previewed
     */
    public void fadeSucceedingTraceStates(List<Node> traceListStates) {
        var activeStatePresentation = traceListStates.stream()
                .filter(n -> ((StatePresentation) n)
                        .getController().getState()
                        .equals(getActiveSimulation().currentState.get()))
                .findFirst().orElse(null);

        if (activeStatePresentation == null) return;

        traceListStates.forEach(trace -> trace.setOpacity(1));
        int i = traceListStates.size() - 1;
        while (traceListStates.get(i) != activeStatePresentation) {
            traceListStates.get(i).setOpacity(0.4);
            i--;
        }
    }

    /**
     * Highlights the edges from the reachability response ToDO NIELS: Trigger reachability highlighting
     */
    public void highlightReachabilityEdges(ArrayList<String> ids) throws NullPointerException {
        //unhighlight all edges
        for (var comp : getActiveSimulation().simulatedComponents) {
            for (var edge : comp.getEdges()) {
                edge.setIsHighlightedForReachability(false);
            }
        }
        //highlight the edges from the reachability response
        for (var comp : getActiveSimulation().simulatedComponents) {
            for (var edge : comp.getEdges()) {
                for (var id : ids) {
                    if (edge.getId().equals(id)) {
                        edge.setIsHighlightedForReachability(true);
                    }
                }
            }
        }
    }

    /**
     * Finds the processes for the input locations in the input {@link State} and highlights the locations.
     *
     * @param state The state with the locations to highlight
     */
    public void highlightProcessState(final State state) {
        Consumer<ObjectProtos.LeafLocation> leafLocationConsumer =
                (leaf) -> componentNameProcessPresentationMap.get(leaf.getComponentInstance().getComponentName())
                        .getController().highlightLocation(leaf.getId());

        Platform.runLater(() -> state.consumeLeafLocations(leafLocationConsumer));
    }

    /**
     * Highlights the edges involved in the action of a decision
     *
     * @param decision decision presentations to extract the action from
     */
    public void highlightDecisionEdges(DecisionPresentation decision) {
        List<String> involvedEdgeIds = decision.getController()
                .getDecision()
                .protoDecision.getEdgesList()
                .stream().map(ObjectProtos.Edge::getId)
                .collect(Collectors.toList());

        componentNameProcessPresentationMap.values().forEach(p -> p.getController()
                .getComponent().getEdges()
                .forEach(e -> e.setIsHighlighted(involvedEdgeIds.contains(e.getId()))));
    }

    /**
     * Removes highlighting from all edges
     */
    public void unhighlightEdges() {
        componentNameProcessPresentationMap.values().forEach(p -> p.getController()
                .getComponent().getEdges()
                .forEach(e -> e.setIsHighlighted(false)));
    }

    public void highlightPreview(final State state) {
        Consumer<ObjectProtos.LeafLocation> leafLocationConsumer =
                (leaf) -> {
                    componentNameProcessPresentationMap.get(leaf.getComponentInstance().getComponentName()).getController().highlightLocation(leaf.getId());
                    temporarilyHighlightedLocations.add(leaf.getId());
                };

        Platform.runLater(() -> state.consumeLeafLocations(leafLocationConsumer));
    }
}
