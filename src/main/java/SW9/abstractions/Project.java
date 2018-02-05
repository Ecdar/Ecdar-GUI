package SW9.abstractions;

import SW9.Ecdar;
import SW9.mutation.MutationTestPlan;
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
    private static final String FOLDER_NAME_COMPONENTS = "Components";
    private static final String FOLDER_NAME_SYSTEMS = "Systems";

    private final ObservableList<Query> queries;
    private final ObservableList<Component> components;
    private final ObservableList<EcdarSystem> systems;
    private final ObservableList<MutationTestPlan> testPlans;
    private final ObjectProperty<Declarations> globalDeclarations;
    private final ObjectProperty<Declarations> systemDeclarations;

    public Project() {
        queries = FXCollections.observableArrayList();
        components = FXCollections.observableArrayList();
        systems = FXCollections.observableArrayList();
        testPlans = FXCollections.observableArrayList();
        globalDeclarations = new SimpleObjectProperty<>(new Declarations("Global Declarations"));
        systemDeclarations = new SimpleObjectProperty<>(new Declarations("System Declarations"));
    }

    public ObservableList<Query> getQueries() {
        return queries;
    }

    public ObservableList<Component> getComponents() {
        return components;
    }

    public ObservableList<EcdarSystem> getSystemsProperty() {
        return systems;
    }

    public ObservableList<MutationTestPlan> getTestPlans() {
        return testPlans;
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
     * Serializes and stores this as JSON files at a given directory.
     * @param directory object containing path to the desired directory to store at
     * @throws IOException if an IO error happens
     */
    public void serialize(final File directory) throws IOException {
        // Clear the project folder
        FileUtils.forceMkdir(directory);
        FileUtils.cleanDirectory(directory);
        FileUtils.forceMkdir(new File(getComponentsFolderName()));
        FileUtils.forceMkdir(new File(getSystemsFolderName()));

        {
            // Save global declarations
            final Writer globalWriter = getSaveFileWriter(GLOBAL_DCL_FILENAME);
            getNewGson().toJson(getGlobalDeclarations().serialize(), globalWriter);
            globalWriter.close();
        }

        {
            // Save system declarations
            final Writer systemDclWriter = getSaveFileWriter(SYSTEM_DCL_FILENAME);
            getNewGson().toJson(getSystemDeclarations().serialize(), systemDclWriter);
            systemDclWriter.close();
        }

        // Save components
        for (final Component component : getComponents()) {
            final Writer writer = getSaveFileWriter(component.getName(), FOLDER_NAME_COMPONENTS);
            getNewGson().toJson(component.serialize(), writer);
            writer.close();
        }

        // Save systems
        for (final EcdarSystem system : getSystemsProperty()) {
            final Writer writer = getSaveFileWriter(system.getName(), FOLDER_NAME_SYSTEMS);
            getNewGson().toJson(system.serialize(), writer);
            writer.close();
        }

        // Serializes queries
        {
            final JsonArray queries = new JsonArray();
            getQueries().forEach(query -> queries.add(query.serialize()));
            final Writer queriesWriter = getSaveFileWriter(QUERIES_FILENAME);
            getNewGson().toJson(queries, queriesWriter);
            queriesWriter.close();
        }

        Ecdar.showToast("Project saved.");
    }

    /**
     * Gets a new GSON object.
     * @return the GSON object
     */
    private static Gson getNewGson() {
        return new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Gets a new file writer for saving a file.
     * @param filename name of file without extension.
     * @return the file writer
     */
    private static FileWriter getSaveFileWriter(final String filename) throws IOException {
        return new FileWriter(Ecdar.projectDirectory.getValue() + File.separator + filename + ".json");
    }

    private static FileWriter getSaveFileWriter(final String filename, final String folderName) throws IOException {
        return new FileWriter(Ecdar.projectDirectory.getValue() + File.separator + folderName + File.separator + filename + ".json");
    }

    private static String getComponentsFolderName() {
        return Ecdar.projectDirectory.getValue() + File.separator + FOLDER_NAME_COMPONENTS;
    }

    private static String getSystemsFolderName() {
        return Ecdar.projectDirectory.getValue() + File.separator + FOLDER_NAME_SYSTEMS;
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

        for (final File file : projectFiles) {
            if (file.isDirectory()) {
                // If components folder
                if (file.getName().equals(FOLDER_NAME_COMPONENTS)) {
                    deserializeComponents(file);
                } else if (file.getName().equals(FOLDER_NAME_SYSTEMS)) {
                    deserializeSystems(file);
                }
                continue;
            }

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
            }
        }
    }

    /**
     * Deserializes the components in a folder.
     * @param componentsFolder the folder containing the JSON components files
     * @throws IOException if an IO error occurs
     */
    private void deserializeComponents(final File componentsFolder) throws IOException {
        // If there are no files do not try to deserialize
        final File[] files = componentsFolder.listFiles();
        if (files == null || files.length == 0) return;

        // Create map for deserialization
        final Map<String, JsonObject> nameJsonMap = new HashMap<>();

        for (final File file : files) {
            // If JSON file
            if (file.getName().endsWith(JSON_FILENAME_EXTENSION)) {
                final String fileContent = Files.toString(file, Charset.defaultCharset());

                // Parse the file to an json object
                final JsonObject jsonObject = new JsonParser().parse(fileContent).getAsJsonObject();

                // Fetch the name of the component
                final String componentName = jsonObject.get("name").getAsString();

                // Add the name and the json object to the map
                nameJsonMap.put(componentName, jsonObject);
            }
        }

        final List<Map.Entry<String, JsonObject>> list = new LinkedList<>(nameJsonMap.entrySet());
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

    private void deserializeSystems(final File systemsFolder) throws IOException {
        // If there are no files do not try to deserialize
        final File[] files = systemsFolder.listFiles();
        if (files == null || files.length == 0) return;

        // Create map for deserialization
        final Map<String, JsonObject> nameJsonMap = new HashMap<>();

        for (final File file : files) {
            // If JSON file
            if (file.getName().endsWith(JSON_FILENAME_EXTENSION)) {
                final String fileContent = Files.toString(file, Charset.defaultCharset());
                final JsonObject json = new JsonParser().parse(fileContent).getAsJsonObject();
                final String name = json.get("name").getAsString();
                nameJsonMap.put(name, json);
            }
        }

        final List<Map.Entry<String, JsonObject>> list = new LinkedList<>(nameJsonMap.entrySet());

        list.sort(Comparator.comparing(Map.Entry::getKey));

        final List<JsonObject> orderedJsonSystems = new ArrayList<>();

        for (final Map.Entry<String, JsonObject> mapEntry : list) {
            orderedJsonSystems.add(mapEntry.getValue());
        }

        // Reverse the list such that the greatest depth is first in the list
        Collections.reverse(orderedJsonSystems);

        // Add the systems to the list
        orderedJsonSystems.forEach(json -> getSystemsProperty().add(new EcdarSystem(json)));
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

        systems.clear();
    }

    /**
     * gets the id of all locations in the project and inserts it into a set
     * @return the set of all location ids
     */
    Set<String> getLocationIds(){
        final Set<String> ids = new HashSet<>();

        for (final Component component : getComponents()) {
            ids.addAll(component.getLocationIds());
        }

        return ids;
    }

    /**
     * gets the id of all systems in the project and inserts it into a set
     * @return the set of all system names
     */
    HashSet<String> getSystemNames(){
        final HashSet<String> names = new HashSet<>();

        for(final EcdarSystem system : getSystemsProperty()){
            names.add(system.getName());
        }

        return names;
    }

    /**
     * Gets universal/inconsistent ids for all components in the project
     * @return a set of universal/inconsistent ids
     */
    HashSet<String> getUniIncIds() {
        final HashSet<String> ids = new HashSet<>();
        for (final Component component : getComponents()){
            ids.add(component.getUniIncId());
        }

        return ids;
    }

    /**
     * Gets the name of all components in the project and inserts it into a set
     * @return the set of all component names
     */
    HashSet<String> getComponentNames(){
        final HashSet<String> names = new HashSet<>();

        for(final Component component : getComponents()){
            names.add(component.getName());
        }

        return names;
    }

    /**
     * Find a component by its name.
     * @param name the name of the component looking for
     * @return the component, or null if none is found
     */
    Component findComponent(final String name) {
        for (final Component component : getComponents()) {
            if (component.getName().equals(name)) return component;
        }

        return null;
    }
}
