package ecdar.backend;

import com.uppaal.engine.Engine;
import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import ecdar.abstractions.Project;
import ecdar.abstractions.Query;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.function.Consumer;

public interface IBackendDriver {
    String TEMP_DIRECTORY = "temporary";

    String storeBackendModel(final Project project, final String fileName) throws BackendException, IOException, URISyntaxException;

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
    String storeBackendModel(final Project project, final String relativeDirectoryPath, final String fileName) throws BackendException, IOException, URISyntaxException;

    /**
     * Stores a query as a backend XML query file in the "temporary" directory.
     *
     * @param query    the query to store.
     * @param fileName file name (without extension) of the file to store
     * @return the path of the file
     * @throws URISyntaxException if an error occurs when getting the URL of the root directory
     * @throws IOException        if an error occurs during storing of the file
     */
    String storeQuery(final String query, final String fileName) throws URISyntaxException, IOException;

    /**
     * Gets the directory path for storing temporary files.
     *
     * @return the path
     * @throws URISyntaxException if an error occurs when getting the URL of the root directory
     */
    String getTempDirectoryAbsolutePath() throws URISyntaxException;

    void buildEcdarDocument() throws BackendException;

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
