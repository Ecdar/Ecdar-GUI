package ecdar.presentations;

import ecdar.abstractions.Edge;
import ecdar.controllers.EcdarController;
import ecdar.controllers.SyncTextFieldController;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.scene.layout.StackPane;

public class SyncTextFieldPresentation extends StackPane {
    private final SyncTextFieldController controller;
    private String placeholder;

    public SyncTextFieldPresentation(String placeholder, Edge edge) {
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

            // Set text when using redo stack to re-add sync
            if(edge != null) {
                controller.textField.setText(edge.syncProperty().get());
                controller.connectedEdge = edge;

                controller.textField.textProperty().unbind();
                edge.syncProperty().bind(controller.textField.textProperty());
            }

            controller.textField.setPromptText(this.placeholder);

            controller.label.textProperty()
                    .bind(new When(controller.textField.textProperty().isNotEmpty())
                            .then(controller.textField.textProperty())
                            .otherwise(controller.textField.promptTextProperty()));
        });
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public SyncTextFieldController getController() {
        return this.controller;
    }
}
