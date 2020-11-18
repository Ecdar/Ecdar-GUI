package ecdar.backend;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import ecdar.abstractions.Project;
import ecdar.abstractions.QueryState;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReveaalDriver implements IBackendDriver {
    private EcdarDocument ecdarDocument;

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

    @Override
    public void buildEcdarDocument() throws BackendException {
        ecdarDocument = new EcdarDocument();
    }

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

        ProcessBuilder pb = new ProcessBuilder("src/Reveaal", "-c", Ecdar.projectDirectory.get(), query.replaceAll("\\s", "")); // Space added to signal EOI
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
