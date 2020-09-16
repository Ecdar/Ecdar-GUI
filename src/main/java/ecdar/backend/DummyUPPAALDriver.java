package ecdar.backend;

import com.uppaal.engine.Engine;
import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import ecdar.abstractions.Project;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.function.Consumer;

public class DummyUPPAALDriver implements IUPPAALDriver {
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

    @Override
    public void buildEcdarDocument() throws BackendException {

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

    @Override
    public Thread runQuery(String query, Consumer<Boolean> success, Consumer<BackendException> failure, Consumer<Engine> engineConsumer, QueryListener queryListener) {
        return null;
    }

    @Override
    public String getVerifytgaAbsolutePath() {
        return null;
    }

    @Override
    public void stopEngines() {

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
