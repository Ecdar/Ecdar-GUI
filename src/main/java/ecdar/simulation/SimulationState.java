package ecdar.simulation;

import ecdar.abstractions.Location;

import java.math.BigDecimal;
import java.util.ArrayList;

public class SimulationState {
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

    public ArrayList<Location> getLocations() {
        // ToDo: Implement
        return new ArrayList<>();
    }
}
