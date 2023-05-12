package ecdar.presentations;

import com.jfoenix.controls.JFXDialog;
import ecdar.presentations.EcdarFXMLLoader;
import ecdar.controllers.SimulationInitializationDialogController;

public class SimulationInitializationDialogPresentation extends JFXDialog {
    private final SimulationInitializationDialogController controller;

    public SimulationInitializationDialogPresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("SimulationInitializationDialogPresentation.fxml", this);
    }

    public SimulationInitializationDialogController getController() {
        return controller;
    }

}
