package ecdar.presentations;

import ecdar.controllers.EcdarController;
import ecdar.controllers.SyncTextFieldController;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.StringProperty;
import javafx.scene.layout.StackPane;

public class SyncTextFieldPresentation extends StackPane {
    private final SyncTextFieldController controller;
    private String placeholder;

    public SyncTextFieldPresentation(String placeholder, StringProperty edgeSyncTextProperty) {
        this.controller = new EcdarFXMLLoader().loadAndGetController("SyncTextFieldPresentation.fxml", this);
        this.placeholder = placeholder;

        Platform.runLater(() -> {
            // Visualize active text field by offsetting the text field
            controller.textField.focusedProperty().addListener((observable, oldFocused, newFocused) -> {
                if (newFocused) {
                    controller.textField.setTranslateY(2);
                } else {
                    controller.textField.setTranslateY(0);
                    EcdarController.getActiveCanvasPresentation().getController().leaveTextAreas();
                }
            });

            controller.textField.setPromptText(this.placeholder);

            controller.label.textProperty()
                    .bind(new When(controller.textField.textProperty().isNotEmpty())
                            .then(controller.textField.textProperty())
                            .otherwise(controller.textField.promptTextProperty()));

            controller.textField.textProperty().unbind();
            edgeSyncTextProperty.bind(controller.textField.textProperty());
        });
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public SyncTextFieldController getController() {
        return this.controller;
    }
}
