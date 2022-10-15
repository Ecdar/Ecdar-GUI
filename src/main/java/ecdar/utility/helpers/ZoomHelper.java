package ecdar.utility.helpers;

import ecdar.controllers.EcdarController;
import ecdar.presentations.CanvasPresentation;
import ecdar.presentations.ModelPresentation;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class ZoomHelper {
    public DoubleProperty currentZoomFactor = new SimpleDoubleProperty(1);
    public double minZoomFactor = 0.4;
    public double maxZoomFactor = 4;

    private CanvasPresentation canvasPresentation;
    private ModelPresentation model;
    private boolean active = true;

    /**
     * Set the CanvasPresentation and add listeners for both the width and height of the new CanvasPresentation
     *
     * @param newCanvasPresentation the new CanvasPresentation
     */
    public void setCanvas(CanvasPresentation newCanvasPresentation) {
        canvasPresentation = newCanvasPresentation;
        model = canvasPresentation.getController().getActiveComponentPresentation();

        // Update the model whenever the component is updated
        canvasPresentation.getController().activeComponentProperty().addListener((observable) -> {
            // Run later to ensure that the active component presentation is up-to-date
            Platform.runLater(() -> {
                model = canvasPresentation.getController().getActiveComponentPresentation();
            });
        });

        Platform.runLater(this::resetZoom);
    }

    public Double getZoomLevel() {
        return currentZoomFactor.get();
    }

    public void setZoomLevel(Double zoomLevel) {
        if (active && model != null) {
            currentZoomFactor.set(zoomLevel);
        }
    }

    /**
     * Zoom in with a delta of 1.2
     */
    public void zoomIn() {
        if (active) {
            double delta = 1.2;
            double newScale = currentZoomFactor.get() * delta;

            //Limit for zooming in
            if (newScale > maxZoomFactor) {
                return;
            }

            currentZoomFactor.set(newScale);
        }
    }

    /**
     * Zoom out with a delta of 1.2
     */
    public void zoomOut() {
        if (active) {
            double delta = 1.2;
            double newScale = currentZoomFactor.get() / delta;

            //Limit for zooming out
            if (newScale < minZoomFactor) {
                return;
            }

            currentZoomFactor.set(newScale);
        }
    }

    /**
     * Set the zoom multiplier to 1
     */
    public void resetZoom() {
        currentZoomFactor.set(1);
    }

    /**
     * Zoom in to fit the component on screen
     */
    public void zoomToFit() {
        if (active) {
            if (EcdarController.getActiveCanvasPresentation().getController().getActiveModel() == null) {
                resetZoom();
                return;
            }

            double neededWidth = canvasPresentation.getWidth() / (model.getWidth()
                    + canvasPresentation.getController().getActiveComponentPresentation().getController().inputSignatureContainer.getWidth()
                    + canvasPresentation.getController().getActiveComponentPresentation().getController().outputSignatureContainer.getWidth());
            double newScale = Math.min(neededWidth, canvasPresentation.getHeight() / model.getHeight() - 0.2); //0.1 for width and 0.2 for height subtracted for margin

            currentZoomFactor.set(newScale);
            centerComponent();
        }
    }

    /**
     * Set zoom as active/disabled
     */
    public void setActive(boolean activeState) {
        this.active = activeState;
        if (!activeState) {
            // If zoom has been disabled, reset the zoom level
            resetZoom();
        }
    }

    private void centerComponent() {
        EcdarController.getActiveCanvasPresentation().getController().modelPane.setTranslateX(0);
        EcdarController.getActiveCanvasPresentation().getController().modelPane.setTranslateY(-20); // 0 is slightly below center, this looks better

        // Center the model within the modelPane to account for resized model
        model.setTranslateX(0);
        model.setTranslateY(0);
    }
}
