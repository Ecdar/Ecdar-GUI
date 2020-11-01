package ecdar.backend;

import com.uppaal.engine.Engine;
import com.uppaal.engine.Problem;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import ecdar.abstractions.Project;
import ecdar.code_analysis.CodeAnalysis;
import javafx.application.Platform;
import org.apache.commons.io.FileUtils;

import java.awt.desktop.SystemSleepEvent;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class jECDARDriver implements IBackendDriver {
    private final String TEMP_DIRECTORY = "temporary";
    private EcdarDocument ecdarDocument;
    private ReentrantLock jEcdarLock = new ReentrantLock(false);

    public jECDARDriver(){
    }
    
    @Override
    public String storeBackendModel(Project project, String fileName) throws BackendException, IOException, URISyntaxException {
        return null;
    }

    @Override
    public String storeBackendModel(Project project, String relativeDirectoryPath, String fileName) throws BackendException, IOException, URISyntaxException {
        return null;
    }

    @Override
    public String storeQuery(String query, String fileName) throws URISyntaxException, IOException {
        FileUtils.forceMkdir(new File(getTempDirectoryAbsolutePath()));

        final String path = getTempDirectoryAbsolutePath() + File.separator + fileName + ".q";
        Files.write(
                Paths.get(path),
                Collections.singletonList(query),
                Charset.forName("UTF-8")
        );

        return path;
    }

    @Override
    public String getTempDirectoryAbsolutePath() throws URISyntaxException {
        return Ecdar.getRootDirectory() + File.separator + TEMP_DIRECTORY;
    }

    public void buildEcdarDocument() throws BackendException {
        ecdarDocument = new EcdarDocument();
    }

    @Override
    public Thread runQuery(String query, Consumer<Boolean> success, Consumer<BackendException> failure) {
        return null;
    }

    @Override
    public Thread runQuery(String query, Consumer<Boolean> success, Consumer<BackendException> failure, long timeout) {
        return null;
    }

    synchronized public Thread runQuery(final String query,
                           final Consumer<Boolean> success,
                           final Consumer<BackendException> failure,
                           final QueryListener queryListener) {
        return new Thread(() -> {
            // Create a list to store the problems of the query
            final Vector<Problem> problems = new Vector<>();

            // Get the system, and fill the problems list if any
            //final UppaalSystem system = engine.getSystem(ecdarDocument.toXmlDocument(), problems);

            // Run on UI thread
            Platform.runLater(() -> {
                // Clear the UI for backend-errors
                CodeAnalysis.clearBackendErrors();

                // Check if there are any problems
                if (!problems.isEmpty()) {
                    problems.forEach(problem -> {
                        System.out.println("problem: " + problem);
                        CodeAnalysis.addBackendError(new CodeAnalysis.Message(problem.getMessage(), CodeAnalysis.MessageType.ERROR));
                    });
                }
            });

            ProcessBuilder pb = new ProcessBuilder("java", "-jar", "src/libs/j-Ecdar.jar");
            pb.redirectErrorStream(true);
            try {
                //Start the j-Ecdar process
                Process jEcdarEngineInstance = pb.start();

                //Communicate with the j-Ecdar process
                try (
                        var jEcdarReader = new BufferedReader(new InputStreamReader(jEcdarEngineInstance.getInputStream()));
                        var jEcdarWriter = new BufferedWriter(new OutputStreamWriter(jEcdarEngineInstance.getOutputStream()));
                ) {
                    //Run the query with the j-Ecdar process
                    jEcdarWriter.write("-rq -json " + Ecdar.projectDirectory.get() + " " + query.replaceAll("\\s", "") + "\n");
                    jEcdarWriter.flush();

                    //Read the result of the query from the j-Ecdar process
                    String line;
                    while ((line = jEcdarReader.readLine()) != null) {
                        // Process the query result
                        if (line.equals("true") || line.equals("")) {
                            success.accept(true);
                        } else if (line.startsWith("Error")) {
                            failure.accept(new BackendException.QueryErrorException(query + " gave " + line));
                        } else if (line.equals("uncertain")/*ToDo: insert uncertain result case
                         line.charAt(0) == 'M'*/) {
                            failure.accept(new BackendException.QueryErrorException(query + ": the jECDAR Engine was uncertain on the result"));
                        } else {
                            failure.accept(new BackendException.BadUPPAALQueryException(query + " could not be run"));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
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

    public enum TraceType {
        NONE, SOME, SHORTEST, FASTEST;

        @Override
        public String toString() {
            return "trace " + this.ordinal();
        }
    }
}
