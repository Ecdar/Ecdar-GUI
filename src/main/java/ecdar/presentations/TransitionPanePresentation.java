package ecdar.presentations;

import ecdar.controllers.TransitionPaneController;
import javafx.scene.layout.*;

/**
 * The presentation class for the transition pane element that can be inserted into the simulator panes
 */
public class TransitionPanePresentation extends VBox {
    final private TransitionPaneController controller;

    public TransitionPanePresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("TransitionPanePresentation.fxml", this);
    }

    public TransitionPaneController getController() {
        return controller;
    }
}
