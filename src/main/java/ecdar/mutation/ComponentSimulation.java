package ecdar.mutation;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.abstractions.Location;
import ecdar.mutation.models.ActionRule;
import ecdar.utility.ExpressionHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ComponentSimulation {
    private final Component component;

    private Location currentLocation;
    private final Map<String, Double> clockValuations = new HashMap<>();

    public ComponentSimulation(final Component component) {
        this.component = component;
        currentLocation = component.getInitialLocation();

        component.getClocks().forEach(clock -> clockValuations.put(clock, 0.0));
    }


    public Location getCurrentLocation() {
        return currentLocation;
    }

    public Map<String, Double> getValuations() {
        return clockValuations;
    }

    private Component getComponent() {
        return component;
    }

    private void setCurrentLocation(final Location currentLocation) {
        this.currentLocation = currentLocation;
    }


    public boolean delay(final double time) {
        // Replace clock valuations with updated ones
        final Map<String, Double> newClockValuations = new HashMap<>();
        clockValuations.forEach((k, v) -> newClockValuations.put(k, v + time));
        newClockValuations.forEach(clockValuations::put);

        final String invariant = getCurrentLocation().getInvariant();

        return invariant.isEmpty() || ExpressionHelper.evaluateBooleanExpression(invariant, getValuations());
    }

    public void runActionRule(final ActionRule rule) {
        setCurrentLocation(component.findLocation(rule.getEndLocationName()));

        ExpressionHelper.parseUpdateProperty(rule.getUpdateProperty()).forEach(clockValuations::put);
    }

    private void runUpdateProperty(final String property) {
        ExpressionHelper.parseUpdateProperty(property).forEach(clockValuations::put);
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
