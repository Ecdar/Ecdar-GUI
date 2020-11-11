package ecdar.backend;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import ecdar.abstractions.Project;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.function.Consumer;

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
}
