package ecdar.backend;

import ecdar.Ecdar;
import ecdar.abstractions.Project;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

public final class BackendHelper {
    final static String TEMP_DIRECTORY = "temporary";
    private static EcdarDocument ecdarDocument;
    public static String storeBackendModel(Project project, String fileName) throws BackendException, IOException, URISyntaxException {
        return null;
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
        return null;
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

    public static void buildEcdarDocument() throws BackendException {
        ecdarDocument = new EcdarDocument();
    }

    public enum BackendNames {
        jEcdar, Reveaal;

        @Override
        public String toString() {
            return this.ordinal() == 0 ? "jEcdar" : "Reveaal";
        }
    }
}
