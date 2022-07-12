package ecdar.presentations;

import ecdar.controllers.SimulatorController;
import javafx.scene.layout.StackPane;

public class SimulatorPresentation extends StackPane {
    private final SimulatorController controller;

    public SimulatorPresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("SimulatorPresentation.fxml", this);
    }


    /**
     * The way to get the associated/linked controller of this presenter
     * @return the controller linked to this presenter
     */
    public SimulatorController getController() {
        return controller;
    }
}
