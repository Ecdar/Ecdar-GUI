package ecdar.presentations;

import ecdar.controllers.TracePaneController;
import javafx.scene.layout.*;

/**
 * The presentation class for the trace element that can be inserted into the simulator panes
 */
public class TracePanePresentation extends VBox {
    final private TracePaneController controller;

    public TracePanePresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("TracePanePresentation.fxml", this);
    }

    public TracePaneController getController() {
        return controller;
    }
}
