package ecdar.abstractions;

import EcdarProtoBuf.ObjectProtos;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

public class State {
    private final ObjectProtos.LocationTree locationTree;
    private final ObjectProtos.State protoState;
    public final ObservableMap<String, BigDecimal> clocks = FXCollections.observableHashMap();

    public State(ObjectProtos.State state) {
        this.locationTree = state.getLocationTree();
        this.protoState = state;
    }

    public void consumeLeafLocations(Consumer<ObjectProtos.LeafLocation> consumer) {
        consumeLeafLocations(locationTree, consumer);
    }

    private void consumeLeafLocations(ObjectProtos.LocationTree tree, Consumer<ObjectProtos.LeafLocation> consumer) {
        switch (tree.getNodeTypeCase()) {
            case LEAF_LOCATION:
                consumer.accept(tree.getLeafLocation());

            case BINARY_LOCATION_OP: {
                consumeLeafLocations(tree.getBinaryLocationOp().getLeft(), consumer);
                consumeLeafLocations(tree.getBinaryLocationOp().getRight(), consumer);
            }

            case SPECIAL_LOCATION: // ToDo: Implement visualization of inconsistent and universal locations

            case NODETYPE_NOT_SET: // Will never happen
        }
    }

    /**
     * All the clocks connected to the current simulation.
     *
     * @return a {@link Map} where the name (String) is the key, and a {@link BigDecimal} is the clock value
     */
    public ObservableMap<String, BigDecimal> getSimulationClocks() {
        return this.clocks;
    }

    public ObjectProtos.State getProtoState() {
        return protoState;
    }

    /**
     * A helper method that returns a string representing the clock constraints of a state in the trace log
     *
     * @return A string representing the clock constraints
     */
    public String getStateClockConstraintsString() {
        StringBuilder clocksString = new StringBuilder();
        for (var constraint : getProtoState().getZone().getConjunctions(0).getConstraintsList()) {
            var x = constraint.getX().getComponentClock().getClockName();
            var y = constraint.getY().getComponentClock().getClockName();
            var c = constraint.getC();
            var strict = constraint.getStrict();
            clocksString.append(x).append(" - ").append(y).append(strict ? " < " : " <= ").append(c).append("\n");
        }

        return clocksString.toString();
    }

    /**
     * A helper method that returns a string representing the locations of a state in the trace log
     *
     * @return A string representing the locations
     */
    public String getStateLocationsString() {
        StringBuilder locationsString = new StringBuilder();

        var leafLocations = new ArrayList<ObjectProtos.LeafLocation>();
        consumeLeafLocations(leafLocations::add);

        int length = leafLocations.size();
        for (int i = 0; i < length; i++) {
            locationsString.append(leafLocations.get(i).getComponentInstance().getComponentName());
            locationsString.append('.');
            locationsString.append(leafLocations.get(i).getId());

            if (i != length - 1) {
                locationsString.append("\n");
            }
        }

        return locationsString.toString();
    }
}
