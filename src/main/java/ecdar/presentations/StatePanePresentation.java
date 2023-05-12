package ecdar.presentations;

import ecdar.controllers.StatePaneController;
import javafx.scene.layout.*;

/**
 * The presentation class for the transition pane element that can be inserted into the simulator panes
 */
public class StatePanePresentation extends VBox {
    final private StatePaneController controller;

    public StatePanePresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("StatePanePresentation.fxml", this);
    }

    public StatePaneController getController() {
        return controller;
    }
}
