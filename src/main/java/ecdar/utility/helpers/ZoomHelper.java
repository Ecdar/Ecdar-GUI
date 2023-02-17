package ecdar.utility.helpers;

import ecdar.Ecdar;
import ecdar.controllers.ComponentController;
import ecdar.presentations.*;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class ZoomHelper {
    public DoubleProperty currentZoomFactor = new SimpleDoubleProperty(1);
    public double minZoomFactor = 0.4;
    public double maxZoomFactor = 4;

    private CanvasPresentation canvasPresentation;
    private HighLevelModelPresentation model;
    private boolean active = true;

    /**
     * Set the CanvasPresentation and add listeners for both the width and height of the new CanvasPresentation
     *
     * @param newCanvasPresentation the new CanvasPresentation
     */
    public void setCanvas(CanvasPresentation newCanvasPresentation) {
        canvasPresentation = newCanvasPresentation;
        model = canvasPresentation.getController().getActiveModelPresentation();

        // Update the model whenever the component is updated
        canvasPresentation.getController().activeModelProperty().addListener((observable) -> {
            // Run later to ensure that the active component presentation is up-to-date
            Platform.runLater(() -> {
                model = canvasPresentation.getController().getActiveModelPresentation();
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
            if (!(canvasPresentation.getController().getActiveModelPresentation() instanceof ComponentPresentation || canvasPresentation.getController().getActiveModelPresentation() instanceof SystemPresentation)) {
                resetZoom();
                return;
            }

            double neededWidth = (model instanceof ComponentPresentation ?
                    (model.getMinWidth()
                            + ((ComponentController) model.getController()).inputSignatureContainer.getWidth()
                            + ((ComponentController) model.getController()).outputSignatureContainer.getWidth())
                    : model.getMinWidth());


            double newScale = Math.min(canvasPresentation.getWidth() / neededWidth, canvasPresentation.getHeight() / model.getMinHeight() - 0.2); // 0.2 subtracted for margin

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
        canvasPresentation.getController().modelPane.setTranslateX(0);

        if (canvasPresentation.getController().modelPane.getChildren().isEmpty() || canvasPresentation.getController().modelPane.getChildren().get(0) instanceof DeclarationsPresentation) {
            canvasPresentation.getController().modelPane.setTranslateY(0);
        } else {
            canvasPresentation.getController().modelPane.setTranslateY(-Ecdar.CANVAS_PADDING * 2); // 0 is slightly below center, this looks better
        }

        // Center the model within the modelPane to account for resized model
        model.setTranslateX(0);
        model.setTranslateY(0);
    }
}
