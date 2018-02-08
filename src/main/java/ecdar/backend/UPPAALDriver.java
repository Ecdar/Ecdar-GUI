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

public class UPPAALDriver {

    public static final int MAX_ENGINES = 10;
    public static final Object engineLock = false; // Used to lock concurrent engine reference access
    private static final String SERVER_NAME = "server";
    private static final String VERIFYTGA_NAME = "verifytga";

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

    private static final String TEMP_DIRECTORY = "temporary";
    public static final String SERVERS_DIRECOTRY = "servers";

    private static EcdarDocument ecdarDocument;

    public static void generateDebugUPPAALModel() throws BackendException, IOException {
        // Generate and store the debug document
        buildEcdarDocument();
        storeUppaalFile(ecdarDocument.toXmlDocument(), Ecdar.debugDirectory + File.separator + "debug.xml");
    }

    /**
     * Stores a project as a backend XML file in the "temporary" directory.
     * @param project project to store
     * @param fileName file name (without extension) of the file to store
     * @return the path of the file
     * @throws BackendException if an error occurs during generation of backend XML
     * @throws IOException if an error occurs during storing of the file
     * @throws URISyntaxException if an error occurs when getting the URL of the root directory
     */
    public static Path storeBackendModel(final Project project, final String fileName) throws BackendException, IOException, URISyntaxException {
        FileUtils.forceMkdir(new File(getTempDirectoryAbsolutePath()));

        final String path = getTempDirectoryAbsolutePath() + File.separator + fileName + ".xml";
        storeUppaalFile(new EcdarDocument(project).toXmlDocument(),  path);

        return Paths.get(path);
    }

    /**
     * Stores a query as a backend XML query file in the "temporary" directory.
     * @param query the query to store.
     * @param fileName file name (without extension) of the file to store
     * @return the path of the file
     * @throws URISyntaxException if an error occurs when getting the URL of the root directory
     * @throws IOException if an error occurs during storing of the file
     */
    public static Path storeQuery(final String query, final String fileName) throws URISyntaxException, IOException {
        FileUtils.forceMkdir(new File(getTempDirectoryAbsolutePath()));

        final Path path = Paths.get(getTempDirectoryAbsolutePath() + File.separator + fileName + ".q");
        Files.write(
                path,
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
    public static String getTempDirectoryAbsolutePath() throws URISyntaxException {
        return Ecdar.getRootDirectory() + File.separator + TEMP_DIRECTORY;
    }

    public static void buildEcdarDocument() throws BackendException {
        ecdarDocument = new EcdarDocument();
    }

    public static Thread runQuery(final String query,
                                  final Consumer<Boolean> success,
                                  final Consumer<BackendException> failure) {
        return runQuery(query, success, failure, -1);
    }
    public static Thread runQuery(final String query,
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

    public static Thread runQuery(final String query,
                                  final Consumer<Boolean> success,
                                  final Consumer<BackendException> failure,
                                  final Consumer<Engine> engineConsumer) {
        return runQuery(query, success, failure, engineConsumer, new QueryListener());
    }

    public static Thread runQuery(final String query,
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

    private static final ArrayList<Engine> createdEngines = new ArrayList<>();
    private static final ArrayList<Engine> availableEngines = new ArrayList<>();

    /**
     * Finds the right server files, based on the system os
     * @return The server file
     */
    private static File findServerFile() {
        final String os = System.getProperty("os.name");
        final File file;

        if (os.contains("Mac")) {
            file = new File(Ecdar.serverDirectory + File.separator + "bin-MacOS" + File.separator + SERVER_NAME);
        } else if (os.contains("Linux")) {
            file = new File(Ecdar.serverDirectory + File.separator + "bin-Linux" + File.separator + SERVER_NAME);
        } else {
            file = new File(Ecdar.serverDirectory + File.separator + "bin-Win32" + File.separator + SERVER_NAME + ".exe");
        }

        return file;
    }

    /**
     * Finds the right verifytga file path, based on the system os.
     * The path is relative to the Ecdar program path.
     * @return the verifytga relative path
     */
    public static String findVerifytgaRelativePath() {
        final String os = System.getProperty("os.name");

        if (os.contains("Mac")) {
            return SERVERS_DIRECOTRY + File.separator + "bin-MacOS" + File.separator + VERIFYTGA_NAME;
        } else if (os.contains("Linux")) {
            return SERVERS_DIRECOTRY + File.separator + "bin-Linux" + File.separator + VERIFYTGA_NAME;
        } else {
            return SERVERS_DIRECOTRY + File.separator + "bin-Win32" + File.separator + VERIFYTGA_NAME + ".exe";
        }
    }

    /**
     * Finds the right verifytga file path, based on the system os.
     * The path is absolute.
     * @return the verifytga relative path
     */
    public static String findVerifytgaAbsolutePath() {
        final String os = System.getProperty("os.name");

        if (os.contains("Mac")) {
            return Ecdar.serverDirectory + File.separator + "bin-MacOS" + File.separator + VERIFYTGA_NAME;
        } else if (os.contains("Linux")) {
            return Ecdar.serverDirectory + File.separator + "bin-Linux" + File.separator + VERIFYTGA_NAME;
        } else {
            return Ecdar.serverDirectory + File.separator + "bin-Win32" + File.separator + VERIFYTGA_NAME + ".exe";
        }
    }

    private static Engine getAvailableEngineOrCreateNew() {
        if (availableEngines.size() == 0) {
            final File serverFile = findServerFile();
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

    private static Engine getOSDependentEngine() {
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

    private static void releaseOSDependentEngine(final Engine engine) {
        synchronized (createdEngines) {
            availableEngines.add(engine);
        }
    }

    public static void stopEngines() {
        synchronized (createdEngines) {
            while (createdEngines.size() != 0) {
                final Engine engine = createdEngines.get(0);
                engine.cancel(); // Cancel any running tasks on this engine
                createdEngines.remove(0);
            }
        }
    }

    private static void storeUppaalFile(final Document uppaalDocument, final String fileName) throws IOException {
        uppaalDocument.save(fileName);
    }

    /**
     * Generates a reachability query based on the given location and component
     * @param location The location which should be checked for reachability
     * @param component The component where the location belong to / are placed
     * @return A reachability query string
     */
    public static String getLocationReachableQuery(final Location location, final Component component) {
        return "E<> " + component.getName() + "." + location.getId();
    }

    /**
     * Generates a string for a deadlock query based on the component
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

        return "E<> (" + String.join(" || ", locationNames) + ") && deadlock";
    }

    public enum TraceType {
        NONE, SOME, SHORTEST, FASTEST;

        @Override
        public String toString() {
            return "trace " + this.ordinal();
        }
    }
}
