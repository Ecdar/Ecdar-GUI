package ecdar.presentations;

import com.jfoenix.controls.JFXDialog;
import ecdar.controllers.InformationDialogController;
import javafx.scene.layout.Region;

public class InformationDialogPresentation extends JFXDialog {
    private final InformationDialogController controller;

    public InformationDialogPresentation(String title, Region content) {
        controller = new EcdarFXMLLoader().loadAndGetController("InformationDialogPresentation.fxml", this);
        controller.okButton.setOnMouseClicked(event -> this.close());
        controller.contentContainer.getChildren().add(content);
        controller.headline.setText(title);
    }

    public InformationDialogController getController() {
        return controller;
    }
}
