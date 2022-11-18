package ecdar.simulation;

import EcdarProtoBuf.ObjectProtos;
import javafx.util.Pair;

import java.math.BigDecimal;
import java.util.ArrayList;

public class SimulationState {
    private final ArrayList<Pair<String, String>> locations;

    public SimulationState(ObjectProtos.State protoBufState) {
        locations = new ArrayList<>();
        for (ObjectProtos.Location location : protoBufState.getLocationTuple().getLocationsList()) {
            locations.add(new Pair<>(location.getId(), location.getSpecificComponent().getComponentName()));
        }
    }

    public void setTime(BigDecimal value) {
        // ToDo: Implement
    }

    public Number getTime() {
        // ToDo: Implement
        return new Number() {
            @Override
            public int intValue() {
                return 0;
            }

            @Override
            public long longValue() {
                return 0;
            }

            @Override
            public float floatValue() {
                return 0;
            }

            @Override
            public double doubleValue() {
                return 0;
            }
        };
    }

    public ArrayList<Pair<String, String>> getLocations() {
        return locations;
    }
}
