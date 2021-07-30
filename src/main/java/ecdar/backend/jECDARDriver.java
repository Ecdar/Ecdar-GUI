package ecdar.backend;

import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import java.util.*;
import java.util.function.Consumer;

public class jECDARDriver implements IBackendDriver {
    @Override
    public BackendThread getBackendThreadForQuery(String query, Consumer<Boolean> success, Consumer<BackendException> failure) {
        return null;
    }

    @Override
    public BackendThread getBackendThreadForQuery(String query, Consumer<Boolean> success, Consumer<BackendException> failure, long timeout) {
        return null;
    }

    synchronized public BackendThread getBackendThreadForQuery(final String query,
                                                               final Consumer<Boolean> success,
                                                               final Consumer<BackendException> failure,
                                                               final QueryListener queryListener) {
        return new jEcdarThread(query, success, failure, queryListener);
    }

    /**
     * Generates a reachability query based on the given location and component.
     *
     * @param location The location which should be checked for reachability.
     * @param component The component where the location belong to / are placed.
     * @return A reachability query string.
     */
    public String getLocationReachableQuery(final Location location, final Component component) {
        return "E<> " + component.getName() + "." + location.getId() + " (" + component.getName() + ")";
    }

    /**
     * Generates a string for a deadlock query based on the component.
     *
     * @param component The component which should be checked for deadlocks.
     * @return A deadlock query string.
     */
    public String getExistDeadlockQuery(final Component component) {
        // Get the names of the locations of this component. Used to produce the deadlock query
        final String templateName = component.getName();
        final List<String> locationNames = new ArrayList<>();

        for (final Location location : component.getLocations()) {
            locationNames.add(templateName + "." + location.getId());
        }

        return "E<> (" + String.join(" || ", locationNames) + ") && deadlock";
    }

    public Process getJEcdarProcessForRefinementCheckAndStrategyIfNonRefinement(String pathToModel, String pathToQueryFile) throws BackendException {
        // ToDo: Not yet implemented
        throw new BackendException("Not implemented");
    }

    public enum TraceType {
        NONE, SOME, SHORTEST, FASTEST;

        @Override
        public String toString() {
            return "trace " + this.ordinal();
        }
    }
}
