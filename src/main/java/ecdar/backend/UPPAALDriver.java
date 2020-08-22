package ecdar.backend;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import ecdar.abstractions.Project;
import ecdar.code_analysis.CodeAnalysis;
import com.uppaal.engine.Engine;
import com.uppaal.engine.EngineException;
import com.uppaal.engine.Problem;
import com.uppaal.model.core2.Document;
import com.uppaal.model.system.UppaalSystem;
import javafx.application.Platform;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

public class UPPAALDriver implements IUPPAALDriver {
    private static final String ECDAR_DEFAULT_OPTIONS = "order 0\n" +
            "order2 1\n" +
            "tigaOrder 0\n" +
            "reduction 1\n" +
            "representation 0\n" +
            "trace 0\n" +
            "extrapolation 0\n" +
            "hashsize 27\n" +
            "reuse 0\n" +
            "tigaWarnIO 0";

    private final String TEMP_DIRECTORY = "temporary";

    private EcdarDocument ecdarDocument;
    private final File serverFile;
    private final File verifytgaFile;

    public UPPAALDriver(File serverFile, File verifytgaFile) {
        this.serverFile = serverFile;
        this.verifytgaFile = verifytgaFile;
    }

    /**
     * Stores a project as a backend XML file in the "temporary" directory.
     * @param project project to store
     * @param fileName file name (without extension) of the file to store
     * @return the absolute path of the file
     * @throws BackendException if an error occurs during generation of backend XML
     * @throws IOException if an error occurs during storing of the file
     * @throws URISyntaxException if an error occurs when getting the URL of the root directory
     */
    public String storeBackendModel(final Project project, final String fileName) throws BackendException, IOException, URISyntaxException {
        return storeBackendModel(project, TEMP_DIRECTORY, fileName);
    }

    /**
     * Stores a project as a backend XML file in a specified path
     * @param project project to store
     * @param relativeDirectoryPath path to the directory to store the model, relative to the Ecdar path
     * @param fileName file name (without extension) of the file to store
     * @return the absolute path of the file
     * @throws BackendException if an error occurs during generation of backend XML
     * @throws IOException if an error occurs during storing of the file
     * @throws URISyntaxException if an error occurs when getting the URL of the root directory
     */
    public String storeBackendModel(final Project project, final String relativeDirectoryPath, final String fileName) throws BackendException, IOException, URISyntaxException {
        final String directoryPath = Ecdar.getRootDirectory() + File.separator + relativeDirectoryPath;

        FileUtils.forceMkdir(new File(directoryPath));

        final String path = directoryPath + File.separator + fileName + ".xml";
        storeUppaalFile(new EcdarDocument(project).toXmlDocument(),  path);

        return path;
    }

    /**
     * Stores a query as a backend XML query file in the "temporary" directory.
     * @param query the query to store.
     * @param fileName file name (without extension) of the file to store
     * @return the path of the file
     * @throws URISyntaxException if an error occurs when getting the URL of the root directory
     * @throws IOException if an error occurs during storing of the file
     */
    public String storeQuery(final String query, final String fileName) throws URISyntaxException, IOException {
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
     * @return the path
     * @throws URISyntaxException if an error occurs when getting the URL of the root directory
     */
    public String getTempDirectoryAbsolutePath() throws URISyntaxException {
        return Ecdar.getRootDirectory() + File.separator + TEMP_DIRECTORY;
    }

    public void buildEcdarDocument() throws BackendException {
        ecdarDocument = new EcdarDocument();
    }

    public Thread runQuery(final String query,
                                  final Consumer<Boolean> success,
                                  final Consumer<BackendException> failure) {
        return runQuery(query, success, failure, -1);
    }
    public Thread runQuery(final String query,
                                  final Consumer<Boolean> success,
                                  final Consumer<BackendException> failure,
                                  final long timeout) {

        final Consumer<Engine> engineConsumer = engine -> {
            if(timeout >= 0) {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        synchronized (engineLock) {
                            if(engine == null) return;
                            engine.cancel();
                        }
                    }
                }, timeout);
            }
        };

        return runQuery(query, success, failure, engineConsumer);
    }

    public Thread runQuery(final String query,
                                  final Consumer<Boolean> success,
                                  final Consumer<BackendException> failure,
                                  final Consumer<Engine> engineConsumer) {
        return runQuery(query, success, failure, engineConsumer, new QueryListener());
    }

    public Thread runQuery(final String query,
                                   final Consumer<Boolean> success,
                                   final Consumer<BackendException> failure,
                                   final Consumer<Engine> engineConsumer,
                                   final QueryListener queryListener) {
        return new Thread() {
            Engine engine;

            @Override
            public void run() {
                try {
                    // "Create" the engine and set the correct server path
                    while (engine == null) {
                        if (isInterrupted()) return;
                        engine = getOSDependentEngine();
                        if (engine == null) {
                            // Waiting for engine
                            Thread.yield();
                        } else {
                            break;
                        }
                    }

                    engine.connect();
                    engineConsumer.accept(engine);

                    // Create a list to store the problems of the query
                    final Vector<Problem> problems = new Vector<>();

                    // Get the system, and fill the problems list if any
                    final UppaalSystem system = engine.getSystem(ecdarDocument.toXmlDocument(), problems);

                    // Run on UI thread
                    Platform.runLater(() -> {
                        // Clear the UI for backend-errors
                        CodeAnalysis.clearBackendErrors();

                        // Check if there is any problems
                        if (!problems.isEmpty()) {
                            problems.forEach(problem -> {
                                System.out.println("problem: " + problem);
                                CodeAnalysis.addBackendError(new CodeAnalysis.Message(problem.getMessage(), CodeAnalysis.MessageType.ERROR));
                            });
                        }
                    });

                    final char result = engine.query(system, ECDAR_DEFAULT_OPTIONS, query, queryListener);

                    // Process the query result
                    if (result == 'T') {
                        success.accept(true);
                    } else if (result == 'F') {
                        success.accept(false);
                    } else if (result == 'M') {
                        failure.accept(new BackendException.QueryErrorException("UPPAAL Engine was uncertain on the result"));
                    } else {
                        failure.accept(new BackendException.BadUPPAALQueryException("Unable to run query"));
                    }

                } catch (EngineException | IOException | NullPointerException e) {
                    // Something went wrong
                    failure.accept(new BackendException.BadUPPAALQueryException("Unable to run query", e));
                } finally {
                    synchronized (engineLock) {
                        releaseOSDependentEngine(engine);
                        engine = null;
                    }

                }
            }
        };
    }

    private final ArrayList<Engine> createdEngines = new ArrayList<>();
    private final ArrayList<Engine> availableEngines = new ArrayList<>();

    private Engine getAvailableEngineOrCreateNew() {
        if (availableEngines.size() == 0) {
            serverFile.setExecutable(true); // Allows us to use the server file

            // Check if the user copied the file correctly
            if (!serverFile.exists()) {
                System.out.println("Could not find backend-file: " + serverFile.getAbsolutePath() + ". Please make sure to copy UPPAAL binaries to this location.");
            }

            // Create a new engine, set the server path, and return it
            final Engine engine = new Engine();
            engine.setServerPath(serverFile.getPath());
            createdEngines.add(engine);
            return engine;
        } else {
            final Engine engine = availableEngines.get(0);
            availableEngines.remove(0);
            return engine;
        }
    }

    private Engine getOSDependentEngine() {
        synchronized (createdEngines) {
            if (!(createdEngines.size() >= MAX_ENGINES && availableEngines.size() == 0)) {
                final Engine engine = getAvailableEngineOrCreateNew();
                if (engine != null) {
                    return engine;
                }
            }
        }

        // No engines are available currently, check back again later
        return null;
    }

    private void releaseOSDependentEngine(final Engine engine) {
        synchronized (createdEngines) {
            availableEngines.add(engine);
        }
    }

    public void stopEngines() {
        synchronized (createdEngines) {
            while (createdEngines.size() != 0) {
                final Engine engine = createdEngines.get(0);
                engine.cancel(); // Cancel any running tasks on this engine
                createdEngines.remove(0);
            }
        }
    }

    private void storeUppaalFile(final Document uppaalDocument, final String fileName) throws IOException {
        uppaalDocument.save(fileName);
    }

    /**
     * Generates a reachability query based on the given location and component
     * @param location The location which should be checked for reachability
     * @param component The component where the location belong to / are placed
     * @return A reachability query string
     */
    public String getLocationReachableQuery(final Location location, final Component component) {
        return "E<> " + component.getName() + "." + location.getId();
    }

    /**
     * Generates a string for a deadlock query based on the component
     * @param component The component which should be checked for deadlocks
     * @return A deadlock query string
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

    public String getVerifytgaAbsolutePath() {
        return verifytgaFile.getAbsolutePath();
    }

    public enum TraceType {
        NONE, SOME, SHORTEST, FASTEST;

        @Override
        public String toString() {
            return "trace " + this.ordinal();
        }
    }
}
