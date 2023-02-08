package ecdar.presentations;

import com.jfoenix.controls.JFXDialog;
import ecdar.controllers.EngineOptionsDialogController;

public class EngineOptionsDialogPresentation extends JFXDialog {
    private final EngineOptionsDialogController controller;

    public EngineOptionsDialogPresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("EngineOptionsDialogPresentation.fxml", this);
    }

    public EngineOptionsDialogController getController() {
        return controller;
    }
}
