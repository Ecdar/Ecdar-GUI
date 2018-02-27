package ecdar.mutation;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.abstractions.Location;
import ecdar.utility.ExpressionHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simulation of a component.
 * It simulates the current location and clock valuations.
 * It does not simulate local variables.
 */
public class ComponentSimulation {
    private final Component component;

    private Location currentLocation;
    private final Map<String, Double> valuations = new HashMap<>();
    private final List<String> clocks = new ArrayList<>();

    public ComponentSimulation(final Component component) {
        this.component = component;
        currentLocation = component.getInitialLocation();

        component.getClocks().forEach(clock -> {
            clocks.add(clock);
            valuations.put(clock, 0.0);
        });
    }


    /* Getters and setters */

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public Map<String, Double> getValuations() {
        return valuations;
    }


    /* Other methods */

    /**
     * Delays.
     * The delay is run successfully if the invariant of the current location still holds.
     * @param time the amount to delay in engine time units
     * @return true iff the delay was run successfully
     */
    public boolean delay(final double time) {
        clocks.forEach(c -> valuations.put(c, valuations.get(c) + time));

        final String invariant = getCurrentLocation().getInvariant();

        return invariant.isEmpty() || ExpressionHelper.evaluateBooleanExpression(invariant, valuations);
    }

    /**
     * Runs an update property by updating valuations.
     * @param property the update property
     */
    private void runUpdateProperty(final String property) {
        ExpressionHelper.parseUpdateProperty(property).forEach(valuations::put);
    }

    /**
     * Returns if the current state is deterministic with respect to a specified action.
     * The state is deterministic iff at most one transition with the specified action is available.
     * @param sync synchronization property without ? or !
     * @param status the status of the action
     * @return true iff the state is deterministic
     */
    @Deprecated
    public boolean isDeterministic(final String sync, final EdgeStatus status) {
        return currentLocation.getType().equals(Location.Type.UNIVERSAL) ||
                getAvailableEdgeStream(sync, status).count() > 1;

    }

    /**
     * Gets a stream containing the available edges matching a specified action.
     * @param sync the specified synchronization output without ? or !
     * @param status the status of the action that you look for
     * @return the stream
     */
    private Stream<Edge> getAvailableEdgeStream(final String sync, final EdgeStatus status) {
        return component.getOutgoingEdges(currentLocation).stream()
                .filter(e -> e.getStatus() == status)
                .filter(e -> e.getSync().equals(sync))
                .filter(e -> e.getGuard().trim().isEmpty() ||
                        ExpressionHelper.evaluateBooleanExpression(e.getGuard(), getValuations()))
                .filter(e -> e.getTargetLocation().getInvariant().isEmpty() ||
                ExpressionHelper.evaluateBooleanExpression(e.getTargetLocation().getInvariant(), getValuations()));
    }

    /**
     * If a valid action is available, runs a transition of that action.
     * The edge must be outgoing from the current location,
     * must be an edge with the given synchronization property,
     * and its guard must be satisfied.
     * @param sync synchronization property without ? or !
     * @param status the status of the action that you look for
     * @return true iff the action succeeded
     * @throws MutationTestingException if multiple transitions with the specified output are available
     */
    @Deprecated
    public boolean runAction(final String sync, final EdgeStatus status) throws MutationTestingException {
        if (currentLocation.getType().equals(Location.Type.UNIVERSAL)) return true;

        final List<Edge> edges = getAvailableEdgeStream(sync, status).collect(Collectors.toList());

        if (edges.size() > 1) throw new MutationTestingException("Simulation of " +
                (status.equals(EdgeStatus.INPUT) ? "input" : "output") +
                " " + sync + " yields a non-deterministic choice between " + edges.size() + " edges");

        if (edges.size() < 1) return false;

        currentLocation = edges.get(0).getTargetLocation();
        runUpdateProperty(edges.get(0).getUpdate());
        return true;
    }

    /**
     * Simulates an input action.
     * @param sync synchronization property without ?
     * @throws MutationTestingException if simulation yields a non-deterministic choice, no choices, or the Universal
     * or Inconsistent locations.
     */
    public void runInputAction(final String sync) throws MutationTestingException {
        final List<Edge> edges = getAvailableEdgeStream(sync, EdgeStatus.INPUT).collect(Collectors.toList());

        if (edges.size() > 1) throw new MutationTestingException("Simulation of input " + sync +
                " yields a non-deterministic choice between " + edges.size() + " edges");

        if (edges.size() < 1) throw new MutationTestingException("Simulation of input " + sync +
                " yields a no choices. Thus, the component is not input-enabled");

        final Location newLoc = edges.get(0).getTargetLocation();

        if (newLoc.isUniversalOrInconsistent()) throw new MutationTestingException("Simulation of input " + sync +
                " yields the Universal or Inconsistent location. This should not happen");

        currentLocation = newLoc;

        runUpdateProperty(edges.get(0).getUpdate());
    }

    /**
     * Simulations an output action.
     * @param sync synchronization property without !
     * @return true iff the simulation succeeded, e.g. we found and run the transition
     * @throws MutationTestingException if simulation yields a non-deterministic choice or the Universal or
     * Inconsistent locations.
     */
    public boolean runOutAction(final String sync) throws MutationTestingException {
        final List<Edge> edges = getAvailableEdgeStream(sync, EdgeStatus.OUTPUT).collect(Collectors.toList());

        if (edges.size() > 1) throw new MutationTestingException("Simulation of input " + sync +
                " yields a non-deterministic choice between " + edges.size() + " edges");

        if (edges.size() < 1) return false;

        final Location newLoc = edges.get(0).getTargetLocation();

        if (newLoc.isUniversalOrInconsistent()) throw new MutationTestingException("Simulation of input " + sync +
                " yields the Universal or Inconsistent location. This should not happen");

        currentLocation = newLoc;

        runUpdateProperty(edges.get(0).getUpdate());
        return true;
    }
}
