package ecdar.backend;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReveaalDriver implements IBackendDriver {
    @Override
    public BackendThread getBackendThreadForQuery(String query, Consumer<Boolean> success, Consumer<BackendException> failure) {
        return null;
    }

    @Override
    public BackendThread getBackendThreadForQuery(String query, Consumer<Boolean> success, Consumer<BackendException> failure, long timeout) {
        return null;
    }

    @Override
    public BackendThread getBackendThreadForQuery(String query, Consumer<Boolean> success, Consumer<BackendException> failure, QueryListener queryListener) {
        return new ReveaalThread(query, success, failure, queryListener);
    }

    /**
     * Generates a reachability query based on the given location and component.
     *
     * @param location The location which should be checked for reachability.
     * @param component The component where the location belong to / are placed.
     * @return A reachability query string.
     */
    public String getLocationReachableQuery(final Location location, final Component component) {
        return "E<> " + component.getName() + "." + location.getId();
    }

    /**
     * Generates a string for a deadlock query based on the component.
     *
     * @param component The component which should be checked for deadlocks.
     * @return A deadlock query string.
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

    public Pair<ArrayList<String>, ArrayList<String>> getInputOutputs(String query) {
        if(!query.startsWith("refinement")) {
            return null;
        }

        // Pair is used as a tuple, not a key-value pair
        Pair<ArrayList<String>, ArrayList<String>> inputOutputs = new Pair<>(new ArrayList<>(), new ArrayList<>());

        ProcessBuilder pb = new ProcessBuilder("src/Reveaal", "-c", Ecdar.projectDirectory.get(), query.replaceAll("\\s", ""));
        pb.redirectErrorStream(true);
        try {
            //Start the j-Ecdar process
            Process ReveaalEngineInstance = pb.start();

            //Communicate with the j-Ecdar process
            try (
                    var ReveaalReader = new BufferedReader(new InputStreamReader(ReveaalEngineInstance.getInputStream()));
            ) {
                //Read the result of the query from the j-Ecdar process
                String line;
                while ((line = ReveaalReader.readLine()) != null) {
                    // Process the query result
                    if (line.endsWith("extra inputs")){
                        Matcher m = Pattern.compile("[\"]([^\"]+)[\"]").matcher(line);
                        while(m.find()){
                            inputOutputs.getKey().add(m.group(1));
                        }
                    } else if (line.startsWith("extra outputs")) {
                        Matcher m = Pattern.compile("[\"]([^\"]+)[\"]").matcher(line);
                        while(m.find()){
                            inputOutputs.getValue().add(m.group(1));
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return inputOutputs;
    }
}
