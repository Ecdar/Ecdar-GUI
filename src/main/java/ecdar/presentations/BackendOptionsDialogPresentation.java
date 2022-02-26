package ecdar.presentations;

import com.jfoenix.controls.JFXDialog;
import ecdar.controllers.BackendOptionsDialogController;

public class BackendOptionsDialogPresentation extends JFXDialog {
    private final BackendOptionsDialogController controller;

    public BackendOptionsDialogPresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("BackendOptionsDialogPresentation.fxml", this);
    }

    public BackendOptionsDialogController getController() {
        return controller;
    }
}
