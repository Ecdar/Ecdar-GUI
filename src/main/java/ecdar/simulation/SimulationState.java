package ecdar.simulation;

import EcdarProtoBuf.ObjectProtos;
import EcdarProtoBuf.ObjectProtos.State;
import ecdar.Ecdar;
import ecdar.backend.SimulationHandler;
import ecdar.controllers.SimulatorController;
import javafx.collections.ObservableMap;
import javafx.util.Pair;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

public class SimulationState {
    // locations and edges are saved as key-value pair where key is component name and value = id
    private final ArrayList<Pair<String, String>> locations;
    private final ArrayList<Pair<String, String>> edges;
    private final State state;

    public SimulationState(ObjectProtos.DecisionPoint decisionPoint) {
        locations = new ArrayList<>();
        for (ObjectProtos.Location location : decisionPoint.getSource().getLocationTuple().getLocationsList()) {
            locations.add(new Pair<>(location.getSpecificComponent().getComponentName(), location.getId()));
        }

        edges = new ArrayList<>();
        if (decisionPoint.getEdgesList().isEmpty()) {
            Ecdar.showToast("No available transitions.");
        }
        for (ObjectProtos.Edge edge : decisionPoint.getEdgesList()) {
            edges.add(new Pair<>(getComponentName(edge.getId()), edge.getId()));
        }
        state = decisionPoint.getSource();
    }

    /**
     * All the clocks connected to the current simulation.
     *
     * @return a {@link Map} where the component name (String) is the key, and the location name is the value (String)
     * @see SimulationHandler#getSimulationVariables()
     */
    public ArrayList<Pair<String, String>> getLocations() {
        return locations;
    }

    public ArrayList<Pair<String, String>> getEnabledEdges() {
        return edges;
    }

    public State getState() {
        return state;
    }

    private String getComponentName(String id) {
        var components = Ecdar.getProject().getComponents();
        for (var component : components) {
            for (var edge : component.getEdges()) {
                if (edge.getId().equals(id)) {
                    return component.getName();
                }
            }
        }
        throw new RuntimeException("Could not find component name for edge with id " + id);
    }

     /**
     * All the clocks connected to the current simulation.
     *
     * @return a {@link Map} where the name (String) is the key, and a {@link BigDecimal} is the clock value
     * @see SimulationHandler#getSimulationVariables()
     */
    public ObservableMap<String, BigDecimal> getSimulationClocks() {
        // TODO move clocks from SimulationHandler to SimulationState
        return SimulatorController.getSimulationHandler().getSimulationClocks();
    }
}
