package ecdar.mutation;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.abstractions.Location;
import ecdar.mutation.models.ActionRule;
import ecdar.utility.ExpressionHelper;

import java.util.*;

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

    private Component getComponent() {
        return component;
    }

    private void setCurrentLocation(final Location currentLocation) {
        this.currentLocation = currentLocation;
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
     * Runs an action rule.
     * This method does not check for guards.
     * It simply updates the current location and runs the update property.
     * @param rule the rule to run
     */
    public void runActionRule(final ActionRule rule) {
        setCurrentLocation(component.findLocation(rule.getEndLocationName()));

        ExpressionHelper.parseUpdateProperty(rule.getUpdateProperty()).forEach(valuations::put);
    }

    /**
     * Runs an update property by updating valuations.
     * @param property the update property
     */
    private void runUpdateProperty(final String property) {
        ExpressionHelper.parseUpdateProperty(property).forEach(valuations::put);
    }

    /**
     * If a valid output edge is available, runs a transition of that edge.
     * The edge must be outgoing from the current location,
     * must be an output edge with the given synchronization property,
     * and its guard must be satisfied.
     * @param outputSync synchronization property without !
     * @return true iff the action succeeded
     */
    public boolean triggerOutput(final String outputSync) {
        final Optional<Edge> edge = component.getOutgoingEdges(currentLocation).stream()
                .filter(e -> e.getStatus() == EdgeStatus.OUTPUT)
                .filter(e -> e.getSync().equals(outputSync))
                .filter(e -> ExpressionHelper.evaluateBooleanExpression(e.getGuard(), getValuations()))
                .findAny();

        if (!edge.isPresent()) return false;

        currentLocation = edge.get().getTargetLocation();
        runUpdateProperty(edge.get().getUpdate());
        return true;
    }
}
