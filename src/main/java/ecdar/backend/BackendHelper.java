package ecdar.backend;

import EcdarProtoBuf.ComponentProtos;
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
    private static final List<Runnable> enginesUpdatedListeners = new ArrayList<>();

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
     * Clears all queued queries, stops all active engines, and closes all open engine connections
     */
    public static void clearEngineConnections() throws BackendException {
        BackendHelper.stopQueries();

        BackendException exception = new BackendException("Exceptions were thrown while attempting to close engine connections");
        for (Engine engine : engines) {
            try {
                engine.closeConnections();
            } catch (BackendException e) {
                exception.addSuppressed(e);
            }
        }

        if (exception.getSuppressed().length > 0) {
            throw exception;
        }
    }

    /**
     * Stop all running queries.
     */
    public static void stopQueries() {
        Ecdar.getProject().getQueries().forEach(Query::cancel);
    }

    /**
     * Returns the Engine with the specified name, or null, if no such Engine exists
     *
     * @param engineName Name of the Engine to return
     * @return The Engine with matching name
     * or the default engine, if no matching engine exists
     */
    public static Engine getEngineByName(String engineName) {
        Optional<Engine> engine = BackendHelper.engines.stream().filter(e -> e.getName().equals(engineName)).findFirst();
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
