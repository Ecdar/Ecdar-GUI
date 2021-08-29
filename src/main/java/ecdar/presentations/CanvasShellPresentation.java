package ecdar.presentations;

import com.jfoenix.controls.JFXRippler;
import ecdar.controllers.CanvasShellController;
import ecdar.controllers.EcdarController;
import ecdar.controllers.MainController;
import ecdar.utility.colors.Color;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

public class CanvasShellPresentation extends StackPane {
    private final CanvasShellController controller;

    public CanvasShellPresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("CanvasShellPresentation.fxml", this);

        getController().root.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            MainController.getActiveCanvasPresentation().getParent().setOpacity(0.75);
            this.setOpacity(1);
            MainController.setActiveCanvasPresentation(getController().canvasPresentation);
        });

        initializeToolbar();
    }

    private void initializeToolbar() {
        final Color color = Color.GREY_BLUE;
        final Color.Intensity intensity = Color.Intensity.I700;

        // Set the background for the top toolbar
        controller.toolbar.setBackground(
                new Background(new BackgroundFill(color.getColor(intensity),
                        CornerRadii.EMPTY,
                        Insets.EMPTY)
                ));

        initializeToolbarButton(controller.zoomIn);
        initializeToolbarButton(controller.zoomOut);
        initializeToolbarButton(controller.zoomToFit);
        initializeToolbarButton(controller.resetZoom);

        widthProperty().addListener((width) -> setClipForChildren());
        heightProperty().addListener((height) -> setClipForChildren());
    }

    private void initializeToolbarButton(final JFXRippler button) {
        final Color color = Color.GREY_BLUE;
        final Color.Intensity colorIntensity = Color.Intensity.I800;

        button.setMaskType(JFXRippler.RipplerMask.CIRCLE);
        button.setRipplerFill(color.getTextColor(colorIntensity));
        button.setPosition(JFXRippler.RipplerPos.BACK);
    }

    public CanvasShellController getController() {
        return controller;
    }

    private void setClipForChildren() {
        Platform.runLater(() -> setClip(new Rectangle(getWidth(), getHeight())));
    }
}
