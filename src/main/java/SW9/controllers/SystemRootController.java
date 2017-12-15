package SW9.controllers;

import SW9.abstractions.ComponentInstance;
import SW9.abstractions.SystemModel;
import SW9.abstractions.SystemRoot;
import javafx.fxml.Initializable;
import javafx.scene.shape.Polygon;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for a system root.
 */
public class SystemRootController {
    public Polygon root;
    private SystemRoot systemRoot;
    private SystemModel system;

    public SystemRoot getSystemRoot() {
        return systemRoot;
    }

    public void setSystemRoot(final SystemRoot systemRoot) {
        this.systemRoot = systemRoot;
    }

    public SystemModel getSystem() {
        return system;
    }

    public void setSystem(final SystemModel system) {
        this.system = system;
    }
}
