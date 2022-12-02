package ecdar.simulation;

import EcdarProtoBuf.ObjectProtos;
import EcdarProtoBuf.ObjectProtos.State;
import ecdar.Ecdar;
import javafx.util.Pair;

import java.util.ArrayList;

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

    public ArrayList<Pair<String, String>> getLocations() {
        return locations;
    }
    public ArrayList<Pair<String, String>> getEdges() {
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
}
