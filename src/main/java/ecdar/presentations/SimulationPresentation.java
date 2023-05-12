package ecdar.presentations;

import ecdar.controllers.SimulationController;
import javafx.scene.layout.StackPane;

public class SimulationPresentation extends StackPane {
    private final SimulationController controller;

    public SimulationPresentation(String composition) {
        controller = new EcdarFXMLLoader().loadAndGetController("SimulationPresentation.fxml", this);
        controller.resetSimulation(composition);
    }

    /**
     * The way to get the associated/linked controller of this presenter
     * @return the controller linked to this presenter
     */
    public SimulationController getController() {
        return controller;
    }
}
