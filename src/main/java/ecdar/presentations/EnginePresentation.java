package ecdar.presentations;

import com.jfoenix.controls.JFXRippler;
import ecdar.Ecdar;
import ecdar.backend.Engine;
import ecdar.controllers.EngineInstanceController;
import ecdar.utility.colors.Color;
import javafx.application.Platform;
import javafx.scene.Cursor;
import javafx.scene.layout.StackPane;

public class EnginePresentation extends StackPane {
    private final EngineInstanceController controller;

    public EnginePresentation(Engine engine) {
        this();
        controller.setEngine(engine);

        // Ensure that the icons are scaled to current font scale
        Platform.runLater(() -> Ecdar.getPresentation().getController().scaleIcons(this));
    }

    public EnginePresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("EnginePresentation.fxml", this);

        controller.pickPathToEngine.setCursor(Cursor.HAND);
        controller.pickPathToEngine.setRipplerFill(Color.GREY.getColor(Color.Intensity.I500));
        controller.pickPathToEngine.setMaskType(JFXRippler.RipplerMask.CIRCLE);
    }

    public EngineInstanceController getController() {
        return controller;
    }
}
