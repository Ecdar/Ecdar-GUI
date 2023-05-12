package ecdar.abstractions;

import EcdarProtoBuf.ObjectProtos;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Consumer;

public class State {
    // locations and edges are saved as key-value pair where key is component name and value = id
    private final ObjectProtos.LocationTree locationTree;
    private final ObjectProtos.State protoState;
    public final ObservableMap<String, BigDecimal> clocks = FXCollections.observableHashMap();

    public State(ObjectProtos.State state) {
        this.locationTree = state.getLocationTree(); // ToDo NIELS: Ensure that the source is indeed the same for all decisions
        this.protoState = state;
    }

    /**
     * All the clocks connected to the current simulation.
     *
     * @return a {@link Map} where the component name (String) is the key, and the location name is the value (String)
     */
    public ObjectProtos.LocationTree getLocationTree() {
        return locationTree;
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
}
