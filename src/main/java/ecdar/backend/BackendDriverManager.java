package ecdar.backend;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class BackendDriverManager {
    private static IBackendDriver instance = null;
    private static BackendNames currentBackend = BackendNames.jEcdar;
    private static final BooleanProperty supportsInputOutputParameters = new SimpleBooleanProperty();

    public static synchronized IBackendDriver getInstance() {

        //If the instance is null this instantiates the correct IBackendDriver class
        if (instance == null) {
            if (currentBackend == BackendNames.jEcdar) {
                instance = new jECDARDriver();
                supportsInputOutputParameters.setValue(false);
            } else if (currentBackend == BackendNames.Reveaal) {
                instance = new ReveaalDriver();
                supportsInputOutputParameters.setValue(true);
            }
        }

        return instance;
    }

    public static void setCurrentBackend(BackendNames backendName) {
        instance = null;
        currentBackend = backendName;
    }

    public enum BackendNames {
        jEcdar, Reveaal;

        @Override
        public String toString() {
            return "" + this.ordinal();
        }
    }

    public static BooleanProperty backendSupportsInputOutputs() {
        // ToDo: Make BackendDriverNames parameter so that the query can have individual BackendDrivers
        return supportsInputOutputParameters;
    }

    public static void swapBackendDriver() {
        if(instance instanceof jECDARDriver) {
            setCurrentBackend(BackendNames.Reveaal);
        } else {
            setCurrentBackend(BackendNames.jEcdar);
        }
    }
}
