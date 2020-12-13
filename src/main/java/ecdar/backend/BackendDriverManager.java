package ecdar.backend;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class BackendDriverManager {
    private static IBackendDriver instance = null;
    private static jECDARDriver jEcdarDriverInstance = null;
    private static ReveaalDriver reveaalDriverInstance = null;
    private static BackendHelper.BackendNames currentGeneralBackend = BackendHelper.BackendNames.Reveaal; // ToDo: add setCurrentGeneralBackend method if relevant
    private static final BooleanProperty supportsInputOutputParameters = new SimpleBooleanProperty();

    public static synchronized IBackendDriver getInstance(BackendHelper.BackendNames backend) {
        // Return the backend based on the requested name, but ensure that only a single instance of each backend is instantiated at a time
        if (backend == BackendHelper.BackendNames.jEcdar) {
            supportsInputOutputParameters.setValue(false);
            if(jEcdarDriverInstance == null) {
                jEcdarDriverInstance = new jECDARDriver();
            }
            return jEcdarDriverInstance;
        } else {
            supportsInputOutputParameters.setValue(true);
            if(reveaalDriverInstance == null) {
                reveaalDriverInstance = new ReveaalDriver();
            }
            return reveaalDriverInstance;
        }
    }

    public static synchronized IBackendDriver getInstance() {
        if(instance == null){
            if (currentGeneralBackend == BackendHelper.BackendNames.jEcdar) {
                instance = getInstance(BackendHelper.BackendNames.jEcdar);
            } else {
                instance = getInstance(BackendHelper.BackendNames.Reveaal);
            }
        }

        return instance;
    }

    public static Boolean backendSupportsInputOutputs(BackendHelper.BackendNames backend) {
        return backend == BackendHelper.BackendNames.Reveaal;
    }
}
