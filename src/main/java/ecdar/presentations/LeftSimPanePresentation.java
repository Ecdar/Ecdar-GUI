package ecdar.presentations;

import ecdar.controllers.LeftSimPaneController;
import javafx.scene.layout.*;

public class LeftSimPanePresentation extends StackPane {
    private LeftSimPaneController controller;

    public LeftSimPanePresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("LeftSimPanePresentation.fxml", this);
    }
}
