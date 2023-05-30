package ecdar.backend;

import EcdarProtoBuf.ObjectProtos;
import EcdarProtoBuf.QueryProtos;
import ecdar.abstractions.ClockConstraint;
import ecdar.abstractions.Decision;
import ecdar.abstractions.State;

import java.util.*;
import java.util.stream.Collectors;

public class StateFactory {
    /**
     * Create a state instance from the composition and simulation step response
     *
     * @param composition of the client requesting the state
     * @param response the simulation step response to generate the state from
     * @return the generated state
     */
    public State createState(String composition, QueryProtos.SimulationStepResponse response) {
        ObjectProtos.State sourceState = response.getNewDecisionPoints(0).getSource();

        HashMap<String, String> componentLocationsMap = loadLocations(sourceState.getLocationTree());

        // ToDo: Handle all conjunctions
        List<ClockConstraint> clockConstraints = loadClockConstraints(sourceState.getZone().getConjunctions(0).getConstraintsList());
        List<Decision> decisions = loadDecisions(composition, response.getNewDecisionPointsList());

        return new State(componentLocationsMap, clockConstraints, decisions);
    }

    /**
     * Create the state instance representing the initial state
     *
     * @param composition of the client requesting the state
     * @param response the simulation step response to generate the state from
     * @return the generated initial state
     */
    public State createInitialState(String composition, QueryProtos.SimulationStepResponse response) {
        ObjectProtos.State sourceState = response.getNewDecisionPoints(0).getSource();

        HashMap<String, String> componentLocationsMap = loadLocations(sourceState.getLocationTree());
        List<Decision> decisions = loadDecisions(composition, response.getNewDecisionPointsList());

        // ToDO: Clock constraints are currently not specified for the initial state.
        //  Should account for the initial locations' invariant.
        List<ClockConstraint> clockConstraints = new ArrayList<>();

        return new State(componentLocationsMap, clockConstraints, decisions);
    }

    private HashMap<String, String> loadLocations(ObjectProtos.LocationTree rootLocationNode) {
        HashMap<String, String> componentLocationsMap = new HashMap<>();

        Queue<ObjectProtos.LocationTree> locationNodes = new ArrayDeque<>();
        locationNodes.add(rootLocationNode);

        ObjectProtos.LocationTree currentNode;
        while ((currentNode = locationNodes.poll()) != null) {
            switch (currentNode.getNodeTypeCase()) {
                case LEAF_LOCATION:
                    componentLocationsMap.put(
                            currentNode.getLeafLocation().getComponentInstance().getComponentName(),
                            currentNode.getLeafLocation().getId());

                case BINARY_LOCATION_OP: {
                    locationNodes.add(currentNode.getBinaryLocationOp().getLeft());
                    locationNodes.add(currentNode.getBinaryLocationOp().getRight());
                }

                case SPECIAL_LOCATION: // ToDo: Implement visualization of inconsistent and universal locations

                case NODETYPE_NOT_SET: // Will never happen
            }
        }

        return componentLocationsMap;
    }

    private List<Decision> loadDecisions(String composition, List<ObjectProtos.Decision> decisionPointsList) {
        List<Decision> decisions = new ArrayList<>();

        for (ObjectProtos.Decision protoDecision : decisionPointsList) {
            List<String> edgeIds = protoDecision.getEdgesList().stream()
                    .map(ObjectProtos.Edge::getId)
                    .collect(Collectors.toList());

            List<ClockConstraint> clockConstraints = protoDecision.getSource().getZone().getConjunctions(0)
                    .getConstraintsList().stream()
                    .map(this::generateClockConstraint)
                    .collect(Collectors.toList());

            decisions.add(new Decision(
                    composition,
                    edgeIds,
                    protoDecision.getAction(),
                    clockConstraints,
                    protoDecision));
        }

        return decisions;
    }

    private List<ClockConstraint> loadClockConstraints(List<ObjectProtos.Constraint> constraintsList) {
        List<ClockConstraint> clockConstraints = new ArrayList<>();

        for (var constraint : constraintsList) {
            clockConstraints.add(generateClockConstraint(constraint));
        }

        return clockConstraints;
    }

    /**
     * Generates a ClockConstraint object from a ProtoBuf constraint
     *
     * @param constraint the ProtoBuf constraint to generate from
     * @return the generated ClockConstraint
     */
    private ClockConstraint generateClockConstraint(ObjectProtos.Constraint constraint) {
        var clock1 = constraint.getX().getComponentClock().getClockName();
        var clock2 = constraint.getY().getComponentClock().getClockName();
        var constant = constraint.getC();
        var strict = constraint.getStrict();

        if (clock1.equals("")) {
            // The first clock is the zero-clock
            return new ClockConstraint(clock2, null, Math.abs(constant), '>', strict);
        } else if (clock2.equals("")) {
            // The second clock is the zero-clock
            return new ClockConstraint(clock1, null, Math.abs(constant), '<', strict);
        } else {
            // None of the clocks are the zero-clock
            if (constant >= 0) {
                return new ClockConstraint(clock1, clock2, Math.abs(constant), '<', strict);
            } else {
                return new ClockConstraint(clock2, clock1, Math.abs(constant), '>', strict);
            }
        }
    }
}
