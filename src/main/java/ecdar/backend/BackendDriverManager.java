package ecdar.backend;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class BackendDriverManager {
    private static IBackendDriver instance = null;
    private static jECDARDriver jEcdarDriverInstance = null;
    private static ReveaalDriver reveaalDriverInstance = null;
    private static BackendHelper.BackendNames currentGeneralBackend = BackendHelper.BackendNames.jEcdar;
    private static final BooleanProperty supportsInputOutputParameters = new SimpleBooleanProperty();

    /**
     * Get the backend instance currently initialized of the given backend type.
     *
     * @param backend the name of the backend for which to retrieve the driver for
     * @return the IBackendDriver of the backend parsed
     */
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

    /**
     * Get the backend instance used for non-query specific tasks.
     *
     * @return the jECDARDriver currently available
     */
    public static synchronized IBackendDriver getInstance() {
        return getInstance(BackendHelper.BackendNames.jEcdar);
    }

    /**
     * Check if the given backend supports ignored inputs and outputs as parameters.
     *
     * @param backend the name of the backend to check
     * @return true if the backend supports ignored inputs and outputs, else false
     */
    public static Boolean backendSupportsInputOutputs(BackendHelper.BackendNames backend) {
        return backend == BackendHelper.BackendNames.Reveaal;
    }
}
