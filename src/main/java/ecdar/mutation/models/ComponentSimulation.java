package ecdar.mutation.models;

import java.util.Map;

/**
 * A component simulation that has getters to get som information about the component.
 */
public interface ComponentSimulation {
    /**
     * Gets the name of the component.
     * @return the name
     */
    String getName();

    /**
     * Gets the id of the current location.
     * @return the id
     */
    String getCurrentLocId();

    /**
     * Gets the valuations of the local variables.
     * @return the valuations
     */
    Map<String, Integer> getLocalVariableValuations();

    /**
     * Gets the valuations of the clocks.
     * @return the valuations
     */
    Map<String, Double> getClockValuations();
}
