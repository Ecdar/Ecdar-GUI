package ecdar.abstractions;

import java.util.HashMap;
import java.util.List;

public class State {
    private final HashMap<String, String> componentLocationMap;
    private final List<ClockConstraint> clockConstraints;
    private final List<Decision> decisions;

    public State(HashMap<String, String> componentLocationMap, List<ClockConstraint> clockConstraints, List<Decision> decisions) {
        this.componentLocationMap = componentLocationMap;
        this.clockConstraints = clockConstraints;
        this.decisions = decisions;
    }

    public HashMap<String, String> getComponentLocationMap() {
        return componentLocationMap;
    }

    public List<ClockConstraint> getClockConstraints() {
        return clockConstraints;
    }

    public List<Decision> getDecisions() {
        return decisions;
    }
}
