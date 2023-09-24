package ecdar.presentations;

import ecdar.abstractions.Decision;
import ecdar.controllers.RightSimPaneController;
import javafx.scene.layout.*;

import java.util.function.Consumer;

/**
 * Presentation class for the right pane in the simulator
 */
public class RightSimPanePresentation extends StackPane {
    private final RightSimPaneController controller;

    public RightSimPanePresentation(Consumer<Decision> onDecisionSelected) {
        controller = new EcdarFXMLLoader().loadAndGetController("RightSimPanePresentation.fxml", this);
        controller.setOnDecisionSelected(onDecisionSelected);
    }

    public RightSimPaneController getController() {
        return controller;
    }
}
