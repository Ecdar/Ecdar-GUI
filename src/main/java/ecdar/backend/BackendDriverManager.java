package ecdar.backend;

public class BackendDriverManager {
    private static IBackendDriver instance = null;
    private static BackendNames currentBackend = BackendNames.jEcdar;

    public static synchronized IBackendDriver getInstance() {

        //If the instance is null this instantiates the correct IBackendDriver class
        if (instance == null) {
            if (currentBackend == BackendNames.jEcdar) {
                instance = new jECDARDriver();
            } else if (currentBackend == BackendNames.Reveaal) {
                instance = new ReveaalDriver();
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
}
