package ecdar.presentations;

import com.jfoenix.controls.JFXRippler;
import ecdar.controllers.CanvasController;
import ecdar.controllers.CanvasShellController;
import ecdar.controllers.EcdarController;
import ecdar.utility.colors.Color;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

public class CanvasShellPresentation extends StackPane {
    private final CanvasShellController controller;
    private final BooleanProperty gridToggle = new SimpleBooleanProperty(false);

    public CanvasShellPresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("CanvasShellPresentation.fxml", this);

        getController().root.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            EcdarController.setActiveCanvasShellPresentation(this);
        });

        initializeGridAndZoomHelper();
        initializeToolbar();

        controller.allowGridProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && gridToggle.get()) showGrid();
            else hideGrid();
        });

        getStyleClass().add("canvas-shell-presentation");
    }

    /**
     * Toggles the user option for whether or not to show the grid on components and system views.
     * @return a Boolean property that is true if the grid has been turned on and false if the grid has been turned off
     */
    public BooleanProperty toggleGridUi() {
        if (gridToggle.get()) {
            if (controller.isGridAllowed()) hideGrid();

            gridToggle.setValue(false);
        } else {
            showGrid();
            gridToggle.setValue(true);
        }
        return gridToggle;
    }

    private void initializeGridAndZoomHelper() {
        gridToggle.setValue(true);

        controller.allowGridProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && gridToggle.get()) showGrid();
            else hideGrid();
        });

        controller.zoomHelper.setGrid(controller.grid);
        controller.zoomHelper.setCanvas(controller.canvasPresentation);
    }

    private void showGrid() {
        controller.grid.setOpacity(1);
    }

    private void hideGrid() {
        controller.grid.setOpacity(0);
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

    private void setClipForChildren() {
        Platform.runLater(() -> setClip(new Rectangle(getWidth(), getHeight())));
    }

    public CanvasShellController getController() {
        return controller;
    }

    public CanvasController getCanvasController() {
        return controller.canvasPresentation.getController();
    }
}
