package ecdar.backend;

public class BackendDriverManager {
    private static IBackendDriver instance = null;

    public static synchronized IBackendDriver getInstance() {

        //If the instance is null this instantiates the correct IBackendDriver class
        if (instance == null) {
            //ToDo: introduce logic to select to correct backend
            instance = new jECDARDriver();
        }

        return instance;
    }
}
