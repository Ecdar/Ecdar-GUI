package ecdar.backend;

import com.uppaal.model.core2.Document;
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
    private static EcdarDocument ecdarDocument;
    private static BackendInstance defaultBackend = null;
    private static ObservableList<BackendInstance> backendInstances = new SimpleListProperty<>();
    private static List<Runnable> backendInstancesUpdatedListeners = new ArrayList<>();

    public static String storeBackendModel(Project project, String fileName) throws BackendException, IOException, URISyntaxException {
        return storeBackendModel(project, TEMP_DIRECTORY, fileName);
    }

    /**
     * Stores a project as a backend XML file in a specified path
     *
     * @param project               project to store
     * @param relativeDirectoryPath path to the directory to store the model, relative to the Ecdar path
     * @param fileName              file name (without extension) of the file to store
     * @return the absolute path of the file
     * @throws BackendException   if an error occurs during generation of backend XML
     * @throws IOException        if an error occurs during storing of the file
     * @throws URISyntaxException if an error occurs when getting the URL of the root directory
     */
    public static String storeBackendModel(Project project, String relativeDirectoryPath, String fileName) throws BackendException, IOException, URISyntaxException {
        final String directoryPath = Ecdar.getRootDirectory() + File.separator + relativeDirectoryPath;

        FileUtils.forceMkdir(new File(directoryPath));

        final String path = directoryPath + File.separator + fileName + ".xml";
        storeEcdarFile(new EcdarDocument(project).toXmlDocument(), path);

        return path;
    }

    private static void storeEcdarFile(final Document EcdarDocument, String path) throws IOException {
        EcdarDocument.save(path);
    }

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
     * Check if the given backend supports ignored inputs and outputs as parameters.
     *
     * @param backend the name of the backend to check
     * @return true if the backend supports ignored inputs and outputs, else false
     */
    public static Boolean backendSupportsInputOutputs(BackendInstance backend) {
        return true;
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
     * Build the Ecdar document to see if an exception is thrown.
     *
     * @throws BackendException if the document could not be built
     */
    public static void buildEcdarDocument() throws BackendException {
        BackendHelper.ecdarDocument = new EcdarDocument();
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
}
