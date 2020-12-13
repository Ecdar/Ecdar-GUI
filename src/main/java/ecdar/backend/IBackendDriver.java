package ecdar.backend;

import ecdar.abstractions.Component;
import ecdar.abstractions.Location;

import java.util.function.Consumer;

public interface IBackendDriver {
    BackendThread runQuery(final String query,
                    final Consumer<Boolean> success,
                    final Consumer<BackendException> failure);

    BackendThread runQuery(final String query,
                    final Consumer<Boolean> success,
                    final Consumer<BackendException> failure,
                    final long timeout);

    BackendThread runQuery(final String query,
                    final Consumer<Boolean> success,
                    final Consumer<BackendException> failure,
                    final QueryListener queryListener);

    /**
     * Generates a reachability query based on the given location and component
     *
     * @param location  The location which should be checked for reachability
     * @param component The component where the location belong to / are placed
     * @return A reachability query string
     */
    String getLocationReachableQuery(final Location location, final Component component);

    /**
     * Generates a string for a deadlock query based on the component
     *
     * @param component The component which should be checked for deadlocks
     * @return A deadlock query string
     */
    String getExistDeadlockQuery(final Component component);

    enum TraceType {
        NONE, SOME, SHORTEST, FASTEST;

        @Override
        public String toString() {
            return "trace " + this.ordinal();
        }
    }
}
