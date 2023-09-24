package ecdar.presentations;

import ecdar.controllers.LeftSimPaneController;
import javafx.scene.layout.*;

public class LeftSimPanePresentation extends StackPane {
    private final LeftSimPaneController controller;

    public LeftSimPanePresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("LeftSimPanePresentation.fxml", this);
    }

    public LeftSimPaneController getController() {
        return controller;
    }
}
