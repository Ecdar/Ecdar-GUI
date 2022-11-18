package ecdar.presentations;

import ecdar.controllers.SimulatorOverviewController;
import javafx.scene.layout.AnchorPane;

/**
 * The presenter of the middle part of the simulator.
 * It is here where processes of a simulation will be shown.
 */
public class SimulatorOverviewPresentation extends AnchorPane {
    private final SimulatorOverviewController controller;

    public SimulatorOverviewPresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("SimulatorOverviewPresentation.fxml", this);
    }

    public SimulatorOverviewController getController() {
        return controller;
    }
}
