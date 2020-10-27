package ecdar.backend;

import com.uppaal.engine.Engine;
import com.uppaal.engine.Problem;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import ecdar.abstractions.Project;
import ecdar.code_analysis.CodeAnalysis;
import javafx.application.Platform;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.function.Consumer;

public class jECDARDriver implements IBackendDriver {

    private Process jEcdarEngineInstance;
    private final BufferedReader jEcdarReader;
    private final BufferedWriter jEcdarWriter;

    public jECDARDriver(){
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", "src/libs/j-Ecdar.jar");
        pb.redirectErrorStream(true);
        try {
            jEcdarEngineInstance = pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        jEcdarReader = new BufferedReader(new InputStreamReader(jEcdarEngineInstance.getInputStream()));
        jEcdarWriter = new BufferedWriter(new OutputStreamWriter(jEcdarEngineInstance.getOutputStream()));
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
        return null;
    }

    @Override
    public String getTempDirectoryAbsolutePath() throws URISyntaxException {
        return null;
    }

    public void buildEcdarDocument() throws BackendException {
        EcdarDocument ecdarDocument = new EcdarDocument();
    }

    @Override
    public Thread runQuery(String query, Consumer<Boolean> success, Consumer<BackendException> failure) {
        return null;
    }

    @Override
    public Thread runQuery(String query, Consumer<Boolean> success, Consumer<BackendException> failure, long timeout) {
        return null;
    }

    @Override
    public Thread runQuery(String query, Consumer<Boolean> success, Consumer<BackendException> failure, Consumer<Engine> engineConsumer) {
        return null;
    }

    public Thread runQuery(final String query,
                           final Consumer<Boolean> success,
                           final Consumer<BackendException> failure,
                           final Consumer<Engine> engineConsumer,
                           final QueryListener queryListener) {
        return new Thread(() -> {
            try {
                // Create a list to store the problems of the query
                final Vector<Problem> problems = new Vector<>();

                // Get the system, and fill the problems list if any
                //final UppaalSystem system = engine.getSystem(ecdarDocument.toXmlDocument(), problems);

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

                jEcdarWriter.write("-rq -json" + Ecdar.projectDirectory.get() + " " + query + "\n"); //-rq -json samples/CarAlarm/Model/ E\u003c\u003e Alarm.L11
                //jEcdarWriter.write("-rq -json samples/EcdarUniversity specification: Spec" + "\n");
                jEcdarWriter.flush();

                String line;
                while ((line = jEcdarReader.readLine()) != null) {

                    // Process the query result
                    if (line.equals("")) {
                        success.accept(true);
                    } else if (line.startsWith("Error")) {
                        failure.accept(new BackendException.QueryErrorException(line));
                    } else if (line.charAt(0) == 'M') {
                        failure.accept(new BackendException.QueryErrorException("UPPAAL Engine was uncertain on the result"));
                    } else {
                        System.out.println(line);
                        failure.accept(new BackendException.BadUPPAALQueryException("Unable to run query"));
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
