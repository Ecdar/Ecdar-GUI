package ecdar.backend;

import EcdarProtoBuf.ComponentProtos;
import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.simulation.SimulationState;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class BackendHelper {
    final static String TEMP_DIRECTORY = "temporary";
    private static BackendInstance defaultBackend = null;
    private static ObservableList<BackendInstance> backendInstances = new SimpleListProperty<>();
    private static List<Runnable> backendInstancesUpdatedListeners = new ArrayList<>();

    /**
     * Stores a query as a backend XML query file in the "temporary" directory.
     *
     * @param query    the query to store.
     * @param fileName file name (without extension) of the file to store
     * @return the path of the file
     * @throws URISyntaxException if an error occurs when getting the URL of the root directory
     * @throws IOException        if an error occurs during storing of the file
     */
    public static String storeQuery(String query, String fileName) throws URISyntaxException, IOException {
        FileUtils.forceMkdir(new File(getTempDirectoryAbsolutePath()));

        final String path = getTempDirectoryAbsolutePath() + File.separator + fileName + ".q";
        Files.write(
                Paths.get(path),
                Collections.singletonList(query),
                Charset.forName("UTF-8")
        );

        return path;
    }

    /**
     * Gets the directory path for storing temporary files.
     *
     * @return the path
     * @throws URISyntaxException if an error occurs when getting the URL of the root directory
     */
    public static String getTempDirectoryAbsolutePath() throws URISyntaxException {
        return Ecdar.getRootDirectory() + File.separator + TEMP_DIRECTORY;
    }

    /**
     * Stop all running queries.
     */
    public static void stopQueries() {
        Ecdar.getProject().getQueries().forEach(Query::cancel);
    }

    public static String getLocationReachableQuery(final Location endLocation, final Component component, final String query) {
        return getLocationReachableQuery(endLocation, component, query, null);
    }
        /**
         * Generates a reachability query based on the given location and component
         *
         * @param endLocation  The location which should be checked for reachability
         * @return A reachability query string
         */
    public static String getLocationReachableQuery(final Location endLocation, final Component component, final String query, final SimulationState state) {
        var stringBuilder = new StringBuilder();

        // append simulation query
        stringBuilder.append(query);

        // append arrow
        stringBuilder.append(" -> ");

        // append start location here TODO
        if (state != null){
            stringBuilder.append(getStartStateString(state));
            stringBuilder.append(";");
        }

        // append end state
        stringBuilder.append(getEndStateString(component.getName(), endLocation.getId()));

        //  return example: m1||M2->[L1,L4](y<3);[L2, L7](y<2)
        System.out.println(stringBuilder);
        return stringBuilder.toString();
    }

    private static String getStartStateString(SimulationState state) {
        var stringBuilder = new StringBuilder();

        // append locations
        var locations = state.getLocations();
        stringBuilder.append("[");
        var appendLocationWithSeparator = false;
        for(var componentName:Ecdar.getSimulationHandler().getComponentsInSimulation()){
            var locationFound = false;

            for(var location:locations){
                if (location.getKey().equals(componentName)){
                    if (appendLocationWithSeparator){
                        stringBuilder.append("," + location.getValue());
                    }
                    else{
                        stringBuilder.append(location.getValue());
                    }
                    locationFound = true;
                }
                if (locationFound){
                    // don't go through more locations, when a location is found for the specific component that we're looking at
                    break;
                }
            }
            appendLocationWithSeparator = true;
        }
        stringBuilder.append("]");

        // append clock values
        var clocks = state.getSimulationClocks();
        stringBuilder.append("()");

        return stringBuilder.toString();
    }

    private static String getEndStateString(String componentName, String endLocationId) {
        var stringBuilder = new StringBuilder();

        stringBuilder.append("[");
        var appendLocationWithSeparator = false;

        for (var component:Ecdar.getSimulationHandler().getComponentsInSimulation())
        {
            if (component.equals(componentName)){
                if (appendLocationWithSeparator){
                    stringBuilder.append("," + endLocationId);
                }
                else{
                    stringBuilder.append(endLocationId);
                }
            }
            else{ // add underscore to indicate, that we don't care about the end locations in the other components
                if (appendLocationWithSeparator){
                    stringBuilder.append(",_");
                }
                else{
                    stringBuilder.append("_");
                }
            }
            if (!appendLocationWithSeparator) {
                appendLocationWithSeparator = true;
            }
        }
        stringBuilder.append("]");
        stringBuilder.append("()");

        return stringBuilder.toString();
    }

    /**
     * Generates a string for a deadlock query based on the component
     *
     * @param component The component which should be checked for deadlocks
     * @return A deadlock query string
     */
    public static String getExistDeadlockQuery(final Component component) {
        // Get the names of the locations of this component. Used to produce the deadlock query
        final String templateName = component.getName();
        final List<String> locationNames = new ArrayList<>();

        for (final Location location : component.getLocations()) {
            locationNames.add(templateName + "." + location.getId());
        }

        return "(" + String.join(" || ", locationNames) + ") && deadlock";
    }

    /**
     * Returns the BackendInstance with the specified name, or null, if no such BackendInstance exists
     *
     * @param backendInstanceName Name of the BackendInstance to return
     * @return The BackendInstance with matching name
     * or the default backend instance, if no matching backendInstance exists
     */
    public static BackendInstance getBackendInstanceByName(String backendInstanceName) {
        Optional<BackendInstance> backendInstance = BackendHelper.backendInstances.stream().filter(bi -> bi.getName().equals(backendInstanceName)).findFirst();
        return backendInstance.orElse(BackendHelper.getDefaultBackendInstance());
    }

    /**
     * Returns the default BackendInstance
     *
     * @return The default BackendInstance
     */
    public static BackendInstance getDefaultBackendInstance() {
        return defaultBackend;
    }

    /**
     * Sets the list of BackendInstances to match the provided list
     *
     * @param updatedBackendInstances The list of BackendInstances that should be stored
     */
    public static void updateBackendInstances(ArrayList<BackendInstance> updatedBackendInstances) {
        BackendHelper.backendInstances = FXCollections.observableList(updatedBackendInstances);
        for (Runnable runnable : BackendHelper.backendInstancesUpdatedListeners) {
            runnable.run();
        }
    }

    /**
     * Returns the ObservableList of BackendInstances
     *
     * @return The ObservableList of BackendInstances
     */
    public static ObservableList<BackendInstance> getBackendInstances() {
        return BackendHelper.backendInstances;
    }

    /**
     * Sets the default BackendInstance to the provided object
     *
     * @param newDefaultBackend The new defaultBackend
     */
    public static void setDefaultBackendInstance(BackendInstance newDefaultBackend) {
        BackendHelper.defaultBackend = newDefaultBackend;
    }

    public static void addBackendInstanceListener(Runnable runnable) {
        BackendHelper.backendInstancesUpdatedListeners.add(runnable);
    }

    public static ComponentProtos.ComponentsInfo.Builder getComponentsInfoBuilder(String query) {
        ComponentProtos.ComponentsInfo.Builder componentsInfoBuilder = ComponentProtos.ComponentsInfo.newBuilder();
        for (Component c : Ecdar.getProject().getComponents()) {
            if (query.contains(c.getName())) {
                componentsInfoBuilder.addComponents(ComponentProtos.Component.newBuilder().setJson(c.serialize().toString()).build());
            }
        }
        componentsInfoBuilder.setComponentsHash(componentsInfoBuilder.getComponentsList().hashCode());
        return componentsInfoBuilder;
    }
}
