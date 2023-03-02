package ecdar.backend;

import ecdar.Ecdar;
import ecdar.abstractions.*;
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
    private static Engine defaultEngine = null;
    private static ObservableList<Engine> engines = new SimpleListProperty<>();
    private static List<Runnable> enginesUpdatedListeners = new ArrayList<>();

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

    /**
     * Generates a reachability query based on the given location and component
     *
     * @param location  The location which should be checked for reachability
     * @param component The component where the location belong to / are placed
     * @return A reachability query string
     */
    public static String getLocationReachableQuery(final Location location, final Component component) {
        return component.getName() + "." + location.getId();
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
     * Returns the Engine with the specified name, or null, if no such Engine exists
     *
     * @param engineName Name of the Engine to return
     * @return The Engine with matching name
     * or the default engine, if no matching engine exists
     */
    public static Engine getEngineByName(String engineName) {
        Optional<Engine> engine = BackendHelper.engines.stream().filter(engine -> engine.getName().equals(engineName)).findFirst();
        return engine.orElse(BackendHelper.getDefaultEngine());
    }

    /**
     * Returns the default Engine
     *
     * @return The default Engine
     */
    public static Engine getDefaultEngine() {
        return defaultEngine;
    }

    /**
     * Sets the list of engines to match the provided list
     *
     * @param updatedEngines The list of engines that should be stored
     */
    public static void updateEngineInstances(ArrayList<Engine> updatedEngines) {
        BackendHelper.engines = FXCollections.observableList(updatedEngines);
        for (Runnable runnable : BackendHelper.enginesUpdatedListeners) {
            runnable.run();
        }
    }

    /**
     * Returns the ObservableList of engines
     *
     * @return The ObservableList of engines
     */
    public static ObservableList<Engine> getEngines() {
        return BackendHelper.engines;
    }

    /**
     * Sets the default Engine to the provided object
     *
     * @param newDefaultEngine The new default engine
     */
    public static void setDefaultEngine(Engine newDefaultEngine) {
        BackendHelper.defaultEngine = newDefaultEngine;
    }

    public static void addEngineInstanceListener(Runnable runnable) {
        BackendHelper.enginesUpdatedListeners.add(runnable);
    }
}
