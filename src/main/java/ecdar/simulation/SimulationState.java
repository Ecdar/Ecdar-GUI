package ecdar.simulation;

import EcdarProtoBuf.ObjectProtos;
import ecdar.abstractions.Location;
import javafx.util.Pair;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class SimulationState {
    private final ArrayList<Pair<String, String>> locations;

    public SimulationState(ObjectProtos.State protoBufState) {
        locations = new ArrayList<>();
        // ToDo: Initialize with correct locations from protoBuf response
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
