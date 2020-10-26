package ecdar.backend;

import java.io.File;

public class BackendDriverManager {
    private static IBackendDriver instance = null;

    public static synchronized IBackendDriver getInstance() {

        //If the instance is null this instantiates the correct IUPPAALDriver class
        if (instance == null) {
            instance = new jECDARDriver();
        }

        return instance;
    }
}
