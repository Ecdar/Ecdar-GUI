package ecdar.backend;

import com.uppaal.engine.Engine;
import com.uppaal.model.core2.Document;
import ecdar.Ecdar;
import ecdar.abstractions.Project;
import ecdar.abstractions.Query;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

public final class BackendHelper {
    // ToDO NIELS: To be removed due to being UPPAAL dependent {
    public static final int MAX_ENGINES = 10;
    public static final Object engineLock = false; // Used to lock concurrent engine reference access
    // ToDO NIELS: To be removed due to being UPPAAL dependent }
    final static String TEMP_DIRECTORY = "temporary";
    private static EcdarDocument ecdarDocument;
    public static String storeBackendModel(Project project, String fileName) throws BackendException, IOException, URISyntaxException {
        return storeBackendModel(project, TEMP_DIRECTORY, fileName);
    }

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
    public static String storeBackendModel(Project project, String relativeDirectoryPath, String fileName) throws BackendException, IOException, URISyntaxException {
        final String directoryPath = Ecdar.getRootDirectory() + File.separator + relativeDirectoryPath;

        FileUtils.forceMkdir(new File(directoryPath));

        final String path = directoryPath + File.separator + fileName + ".xml";
        storeEcdarFile(new EcdarDocument(project).toXmlDocument(),  path);

        return path;
    }

    private static void storeEcdarFile(final Document EcdarDocument, String path) throws IOException {
        EcdarDocument.save(path);
    }

    /**
     * Stores a query as a backend XML query file in the "temporary" directory.
     *
     * @param query    the query to store.
     * @param fileName file name (without extension) of the file to store
     * @return the path of the file
     * @throws URISyntaxException if an error occurs when getting the URL of the root directory
     * @throws IOException        if an error occurs during storing of the file
     */
    public static String storeQuery(String query, String fileName) throws URISyntaxException, IOException {
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
     *
     * @return the path
     * @throws URISyntaxException if an error occurs when getting the URL of the root directory
     */
    public static String getTempDirectoryAbsolutePath() throws URISyntaxException {
        return Ecdar.getRootDirectory() + File.separator + TEMP_DIRECTORY;
    }

    /**
     * Build the Ecdar document to see if an exception is thrown.
     *
     * @throws BackendException if the document could not be built
     */
    public static void buildEcdarDocument() throws BackendException {
        ecdarDocument = new EcdarDocument();
    }

    // ToDO NIELS: To be removed due to being UPPAAL dependent
    private static final ArrayList<Engine> createdEngines = new ArrayList<>();
    private static final ArrayList<Engine> availableEngines = new ArrayList<>();

    // ToDO NIELS: To be removed due to being UPPAAL dependent
    public static Document getEcdarDocument() throws BackendException {
        if (ecdarDocument == null) {
            buildEcdarDocument();
        }

        return ecdarDocument.toXmlDocument();
    }

    /**
     * Stop all running queries.
     */
    public static void stopQueries() {
        Ecdar.getProject().getQueries().forEach(Query::cancel);
    }

    // ToDO NIELS: To be removed due to being UPPAAL dependent
    public static Engine getEngine() {
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

    // ToDO NIELS: To be removed due to being UPPAAL dependent
    private static Engine getAvailableEngineOrCreateNew() {
        if (availableEngines.size() == 0) {
            final File serverFile = findServerFile();

            // Allows us to use the server file
            if(!serverFile.setExecutable(true)){
                //The user does not have permission to do so
                System.out.println("Insufficient permission when trying to set the server as executable");
                Ecdar.showToast("Insufficient permission when trying to set the server as executable");
            }

            // Check if the user copied the file correctly
            if (!serverFile.exists()) {
                System.out.println("Could not find backend-file: "
                        + serverFile.getAbsolutePath()
                        + ". Please make sure to copy UPPAAL binaries to this location.");
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

    // ToDO NIELS: To be removed due to being UPPAAL dependent
    private static File findServerFile() {
        final String os = System.getProperty("os.name");
        final File file;

        if (os.contains("Mac")) {
            file = new File(Ecdar.getServerPath() + File.separator + "bin-MacOS" + File.separator + "server");
        } else if (os.contains("Linux")) {
            file = new File(Ecdar.getServerPath() + File.separator + "bin-Linux" + File.separator + "server");
        } else {
            file = new File(Ecdar.getServerPath() + File.separator + "bin-Win32" + File.separator + "server" + ".exe");
        }

        return file;
    }

    /**
     * Enum for the available backends. Used for saving and loading the queries.
     */
    public enum BackendNames {
        jEcdar, Reveaal;

        @Override
        public String toString() {
            return this.ordinal() == 0 ? "jEcdar" : "Reveaal";
        }
    }
}
