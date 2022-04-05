package ecdar.utility.helpers;

import ecdar.controllers.EcdarController;
import ecdar.presentations.CanvasPresentation;
import ecdar.presentations.Grid;
import ecdar.presentations.ModelPresentation;
import ecdar.presentations.SystemPresentation;
import javafx.scene.Node;

public class ZoomHelper {
    private CanvasPresentation canvasPresentation;
    private ModelPresentation model;
    private Grid grid;
    private boolean active = true;

    public double minZoomFactor = 0.4;
    public double maxZoomFactor = 4;

    /**
     * Set the CanvasPresentation of the grid and add listeners for both the width and height of the new CanvasPresentation
     * @param newCanvasPresentation the new CanvasPresentation
     */
    public void setCanvas(CanvasPresentation newCanvasPresentation) {
        canvasPresentation = newCanvasPresentation;
        model = canvasPresentation.getController().getActiveComponentPresentation();
        // ToDo grid: Currently results in crash
//        canvasPresentation.widthProperty().addListener((observable -> centerComponentAndUpdateGrid(canvasPresentation.scaleXProperty().doubleValue())));
//        canvasPresentation.heightProperty().addListener((observable -> centerComponentAndUpdateGrid(canvasPresentation.scaleYProperty().doubleValue())));
    }

    public Double getZoomLevel() {
        return model.getScaleX();
    }

    public void setZoomLevel(Double zoomLevel) {
        if (active && model != null) {
            model.setScaleX(zoomLevel);
            model.setScaleY(zoomLevel);
            centerComponentAndUpdateGrid(zoomLevel);
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
            double newScale = model.getScaleX() * delta;

            //Limit for zooming in
            if (newScale > maxZoomFactor) {
                return;
            }

            scaleModel(newScale);
            centerComponentAndUpdateGrid(newScale);
        }
    }

    /**
     * Zoom out with a delta of 1.2
     */
    public void zoomOut() {
        if (active) {
            double delta = 1.2;
            double newScale = model.getScaleX() / delta;

            //Limit for zooming out
            if (newScale < minZoomFactor) {
                return;
            }

            scaleModel(newScale);
            centerComponentAndUpdateGrid(newScale);
        }
    }

    /**
     * Set the zoom multiplier to 1
     */
    public void resetZoom() {
        if (active) {
            scaleModel(1);
            centerComponentAndUpdateGrid(1);
        }
    }

    /**
     * Zoom in to fit the component on screen
     */
    public void zoomToFit() {
        if (active) {
            if (EcdarController.getActiveCanvasPresentation().getController().activeComponentPresentation == null) {
                resetZoom();
                return;
            }
            double newScale = Math.min(canvasPresentation.getWidth() / EcdarController.getActiveCanvasPresentation().getController().activeComponentPresentation.getWidth() - 0.1, canvasPresentation.getHeight() / EcdarController.getActiveCanvasPresentation().getController().activeComponentPresentation.getHeight() - 0.2); //0.1 for width and 0.2 for height added for margin

            scaleModel(newScale);
            centerComponentAndUpdateGrid(newScale);
        }
    }

    /**
     * Set zoom as active/disabled
     */
    public void setActive(boolean activeState) {
        this.active = activeState;
    }

    /**
     * Method for centering the active component on screen and redrawing the grid to fill the screen
     * @param newScale the scale in which to redraw the grid and place the component based on
     */
    private void centerComponentAndUpdateGrid(double newScale){
        // Check added to avoid NullPointerException
        if(EcdarController.getActiveCanvasPresentation().getController().activeComponentPresentation != null){
            updateGrid(newScale, EcdarController.getActiveCanvasPresentation().getController().activeComponentPresentation);
        } else if (EcdarController.getActiveCanvasPresentation().getController().getActiveModel() != null) {
            // The canvas is currently showing an EcdarSystem object, so wee need to find the SystemPresentation in order to center it on screen
            SystemPresentation systemPresentation = null;

            for (Node node : EcdarController.getActiveCanvasPresentation().getController().modelPane.getChildren()) {
                if (node instanceof SystemPresentation) {
                    systemPresentation = (SystemPresentation) node;
                    break;
                }
            }

            if (systemPresentation == null) {
                return;
            }

            updateGrid(newScale, systemPresentation);
        }
    }

    private void scaleModel(double newScale) {
        model.setScaleX(newScale);
        model.setScaleY(newScale);
    }

    private void updateGrid(double newScale, ModelPresentation modelPresentation) {
        // Calculate the new x and y offsets needed to center the model
        double xOffset = newScale * canvasPresentation.getWidth() * 1.0f / 2 - newScale * modelPresentation.getWidth() * 1.0f / 2;
        double yOffset = newScale * canvasPresentation.getHeight() * 1.0f / 2 - newScale * modelPresentation.getHeight() * 1.0f / 2;

        modelPresentation.setTranslateX(xOffset);
        modelPresentation.setTranslateY(yOffset);

        // Redraw the grid based on the new scale and canvas size
        grid.updateGrid(newScale);
    }
}
