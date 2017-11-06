package SW9.abstractions;

import SW9.Ecdar;
import com.google.common.io.Files;
import com.google.gson.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.*;

/**
 * A project of models.
 */
public class Project {
    private final static String GLOBAL_DCL_FILENAME = "GlobalDeclarations";
    private final static String SYSTEM_DCL_FILENAME = "SystemDeclarations";
    private final static String QUERIES_FILENAME = "Queries";
    private final static String JSON_FILENAME_EXTENSION = ".json";

    private final ObservableList<Query> queries;
    private final ObservableList<Component> components;
    private final ObjectProperty<Declarations> globalDeclarations;
    private final ObjectProperty<Declarations> systemDeclarations;

    public Project() {
        queries = FXCollections.observableArrayList();
        components = FXCollections.observableArrayList();
        globalDeclarations = new SimpleObjectProperty<>(new Declarations("Global Declarations"));
        systemDeclarations = new SimpleObjectProperty<>(new Declarations("System Declarations"));
    }

    public ObservableList<Query> getQueries() {
        return queries;
    }

    public ObservableList<Component> getComponents() {
        return components;
    }

    public Declarations getGlobalDeclarations() {
        return globalDeclarations.get();
    }

    public void setGlobalDeclarations(final Declarations declarations) {
        globalDeclarations.set(declarations);
    }

    public Declarations getSystemDeclarations() {
        return systemDeclarations.get();
    }

    public void setSystemDeclarations(final Declarations declarations) {
        systemDeclarations.set(declarations);
    }

    /**
     * Serializes and stores this at the Ecdar project directory as JSON files.
     */
    public void serialize() throws IOException {
        // Clear the project folder
        final File directory = new File(Ecdar.projectDirectory.getValue());
        FileUtils.forceMkdir(directory);
        FileUtils.cleanDirectory(directory);

        // Save global declarations
        Writer writer = getSaveFileWriter(GLOBAL_DCL_FILENAME);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        gson.toJson(getGlobalDeclarations().serialize(), writer);
        writer.close();

        // Save system declarations
        writer = getSaveFileWriter(SYSTEM_DCL_FILENAME);
        gson = new GsonBuilder().setPrettyPrinting().create();
        gson.toJson(getSystemDeclarations().serialize(), writer);
        writer.close();

        // Save components
        for (final Component component : getComponents()) {
            writer = getSaveFileWriter(component.getName());
            gson = new GsonBuilder().setPrettyPrinting().create();

            gson.toJson(component.serialize(), writer);

            writer.close();
        }

        // Serializes queries
        final JsonArray queries = new JsonArray();
        getQueries().forEach(query -> queries.add(query.serialize()));

        writer = getSaveFileWriter(QUERIES_FILENAME);
        gson = new GsonBuilder().setPrettyPrinting().create();

        gson.toJson(queries, writer);
        writer.close();

        Ecdar.showToast("Project saved!");
    }

    /**
     * Gets a new file writer for saving a file.
     * @param filename name of file without extension.
     * @return the file writer
     */
    private static FileWriter getSaveFileWriter(final String filename) throws IOException {
        return new FileWriter(Ecdar.projectDirectory.getValue() + File.separator + filename + ".json");
    }

    /**
     * Reads files in a folder and serializes this based on the files.
     * @param projectFolder the folder to read files from
     * @throws IOException throws iff an IO error occurs
     */
    public void deserialize(final File projectFolder) throws IOException {
        // If there are no files do not try to deserialize
        final File[] projectFiles = projectFolder.listFiles();
        if (projectFiles == null || projectFiles.length == 0) return;

        // Create map for deserialization
        final Map<String, JsonObject> componentJsonMap = new HashMap<>();

        for (final File file : projectFiles) {
            final String fileContent = Files.toString(file, Charset.defaultCharset());

            if (file.getName().equals(GLOBAL_DCL_FILENAME + JSON_FILENAME_EXTENSION)) {
                final JsonObject jsonObject = new JsonParser().parse(fileContent).getAsJsonObject();
                setGlobalDeclarations(new Declarations(jsonObject));
                continue;
            }

            if (file.getName().equals(SYSTEM_DCL_FILENAME + JSON_FILENAME_EXTENSION)) {
                final JsonObject jsonObject = new JsonParser().parse(fileContent).getAsJsonObject();
                setSystemDeclarations(new Declarations(jsonObject));
                continue;
            }

            // If the file represents the queries
            if (file.getName().equals(QUERIES_FILENAME + JSON_FILENAME_EXTENSION)) {
                new JsonParser().parse(fileContent).getAsJsonArray().forEach(jsonElement -> {
                    final Query newQuery = new Query((JsonObject) jsonElement);
                    getQueries().add(newQuery);
                });
                // Do not parse Queries.json as a component
                continue;
            }

            // Parse the file to an json object
            final JsonObject jsonObject = new JsonParser().parse(fileContent).getAsJsonObject();

            // Fetch the name of the component
            final String componentName = jsonObject.get("name").getAsString();

            // Add the name and the json object to the map
            componentJsonMap.put(componentName, jsonObject);

        }

        final List<Map.Entry<String, JsonObject>> list = new LinkedList<>(componentJsonMap.entrySet());
        // Defined Custom Comparator here
        list.sort(Comparator.comparing(Map.Entry::getKey));

        final List<JsonObject> orderedJsonComponents = new ArrayList<>();

        for (final Map.Entry<String, JsonObject> mapEntry : list) {
            orderedJsonComponents.add(mapEntry.getValue());
        }

        // Reverse the list such that the greatest depth is first in the list
        Collections.reverse(orderedJsonComponents);

        // Add the components to the list
        orderedJsonComponents.forEach(jsonObject -> getComponents().add(new Component(jsonObject)));
    }

    /**
     * Resets components.
     * After this, there is only one component.
     * Be sure to disable code analysis before call and enable after call.
     */
    public void reset() {
        clean();
        components.add(new Component(true));
    }

    /**
     * Cleans the project.
     * Be sure to disable code analysis before call and enable after call.
     */
    public void clean() {
        getGlobalDeclarations().clearDeclarationsText();
        getSystemDeclarations().clearDeclarationsText();

        queries.clear();

        components.clear();
    }
}
