package ecdar.presentations;

import ecdar.abstractions.Decision;
import ecdar.controllers.DecisionController;
import javafx.scene.layout.AnchorPane;

public class DecisionPresentation extends AnchorPane {
    private final DecisionController controller;

    public DecisionPresentation(Decision decision) {
        controller = new EcdarFXMLLoader().loadAndGetController("DecisionPresentation.fxml", this);
        controller.setDecision(decision);
    }

    public DecisionController getController() {
        return controller;
    }
}
