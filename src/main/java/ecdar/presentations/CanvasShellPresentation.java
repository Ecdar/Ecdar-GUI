package ecdar.presentations;

import com.jfoenix.controls.JFXRippler;
import ecdar.controllers.CanvasShellController;
import ecdar.utility.colors.Color;
import ecdar.utility.keyboard.Keybind;
import ecdar.utility.keyboard.KeyboardTracker;
import javafx.geometry.Insets;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;

public class CanvasShellPresentation extends Pane {
    private final CanvasShellController controller;

    public CanvasShellPresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("CanvasShellPresentation.fxml", this);

        //Add keybindings for zoom functionality
        KeyboardTracker.registerKeybind(KeyboardTracker.ZOOM_IN, new Keybind(new KeyCodeCombination(KeyCode.PLUS, KeyCombination.SHORTCUT_DOWN), controller.zoomHelper::zoomIn));
        KeyboardTracker.registerKeybind(KeyboardTracker.ZOOM_OUT, new Keybind(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN), controller.zoomHelper::zoomOut));
        KeyboardTracker.registerKeybind(KeyboardTracker.ZOOM_TO_FIT, new Keybind(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN), controller.zoomHelper::zoomToFit));
        KeyboardTracker.registerKeybind(KeyboardTracker.RESET_ZOOM, new Keybind(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN), controller.zoomHelper::resetZoom));

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
}
