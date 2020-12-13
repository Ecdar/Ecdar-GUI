package ecdar.backend;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReveaalDriver implements IBackendDriver {
    @Override
    public BackendThread runQuery(String query, Consumer<Boolean> success, Consumer<BackendException> failure) {
        return null;
    }

    @Override
    public BackendThread runQuery(String query, Consumer<Boolean> success, Consumer<BackendException> failure, long timeout) {
        return null;
    }

    @Override
    public BackendThread runQuery(String query, Consumer<Boolean> success, Consumer<BackendException> failure, QueryListener queryListener) {
        return new ReveaalThread(query, success, failure, queryListener);
    }

    @Override
    public String getLocationReachableQuery(Location location, Component component) {
        return null;
    }

    @Override
    public String getExistDeadlockQuery(Component component) {
        return null;
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
