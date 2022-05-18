package ecdar.utility.helpers;

import ecdar.controllers.EcdarController;
import ecdar.presentations.CanvasPresentation;
import ecdar.presentations.Grid;
import ecdar.presentations.ModelPresentation;
import ecdar.presentations.SystemPresentation;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Node;

public class ZoomHelper {
    public DoubleProperty currentZoomFactor = new SimpleDoubleProperty();
    public double minZoomFactor = 0.4;
    public double maxZoomFactor = 4;

    private CanvasPresentation canvasPresentation;
    private ModelPresentation model;
    private Grid grid;
    private boolean active = true;

    /**
     * Set the CanvasPresentation of the grid and add listeners for both the width and height of the new CanvasPresentation
     *
     * @param newCanvasPresentation the new CanvasPresentation
     */
    public void setCanvas(CanvasPresentation newCanvasPresentation) {
        canvasPresentation = newCanvasPresentation;
        model = canvasPresentation.getController().getActiveComponentPresentation();
        Platform.runLater(this::resetZoom);
    }

    public Double getZoomLevel() {
        return currentZoomFactor.get();
    }

    public void setZoomLevel(Double zoomLevel) {
        if (active && model != null) {
            currentZoomFactor.set(zoomLevel);
            grid.updateGrid();
        }
    }

    public void setGrid(Grid newGrid) {
        grid = newGrid;
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
            double newScale = Math.min(canvasPresentation.getWidth() / model.getWidth() - 0.1, canvasPresentation.getHeight() / model.getHeight() - 0.2); //0.1 for width and 0.2 for height subtracted for margin

            currentZoomFactor.set(newScale);
            centerComponentAndUpdateGrid();
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

    /**
     * Method for centering the active component on screen and redrawing the grid to fill the screen
     */
    private void centerComponentAndUpdateGrid() {
        // Check added to avoid NullPointerException
        grid.updateGrid();
        centerComponent(EcdarController.getActiveCanvasPresentation().getController().modelPane);
    }

    private void centerComponent(Node presentation) {
        presentation.setTranslateX(0);
        presentation.setTranslateY(-Grid.GRID_SIZE * 2); // 0 is slightly below center, this looks better
    }
}
