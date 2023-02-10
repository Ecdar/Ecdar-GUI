package ecdar.presentations;

import com.jfoenix.controls.JFXRippler;
import ecdar.Ecdar;
import ecdar.abstractions.BackendInstance;
import ecdar.controllers.BackendInstanceController;
import ecdar.utility.colors.Color;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.layout.StackPane;

public class BackendInstancePresentation extends StackPane {
    private final BackendInstanceController controller;

    public BackendInstancePresentation(BackendInstance backendInstance) {
        this();
        controller.setBackendInstance(backendInstance);

        // Ensure that the icons are scaled to current font scale
        Platform.runLater(() -> Ecdar.getPresentation().getController().scaleIcons(this));
    }

    public BackendInstancePresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("BackendInstancePresentation.fxml", this);

        controller.pickPathToBackend.setCursor(Cursor.HAND);
        controller.pickPathToBackend.setRipplerFill(Color.GREY.getColor(Color.Intensity.I500));
        controller.pickPathToBackend.setMaskType(JFXRippler.RipplerMask.CIRCLE);
    }

    public BackendInstanceController getController() {
        return controller;
    }
}
