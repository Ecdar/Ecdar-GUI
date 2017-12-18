package SW9.controllers;

import SW9.abstractions.EcdarSystemEdge;
import SW9.abstractions.SystemModel;
import SW9.abstractions.SystemRoot;
import com.jfoenix.controls.JFXPopup;
import javafx.fxml.FXML;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Polygon;

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

    @FXML
    private void onMouseClicked(final MouseEvent event) {
        event.consume();

        final EcdarSystemEdge unfinishedEdge = getSystem().getUnfinishedEdge();

        // if primary clicked and there is an unfinished edge, finish it with the system root as target
        if (unfinishedEdge != null && event.getButton().equals(MouseButton.PRIMARY)) {
            unfinishedEdge.setTarget(systemRoot);
        }
    }
}
