package ecdar.utility.helpers;

import ecdar.controllers.EcdarController;
import ecdar.presentations.CanvasPresentation;
import ecdar.presentations.Grid;
import ecdar.presentations.ModelPresentation;
import ecdar.presentations.SystemPresentation;
import javafx.scene.Node;

public class ZoomHelper {
    private CanvasPresentation canvasPresentation;
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
        // ToDo grid: Currently results in the canvas moving out of view and push everything away
//        canvasPresentation.widthProperty().addListener((observable -> centerComponentAndUpdateGrid(canvasPresentation.scaleXProperty().doubleValue())));
//        canvasPresentation.heightProperty().addListener((observable -> centerComponentAndUpdateGrid(canvasPresentation.scaleYProperty().doubleValue())));
    }

    public Double getZoomLevel() {
        return canvasPresentation.getScaleX();
    }

    public void setZoomLevel(Double zoomLevel) {
        if (active) {
            canvasPresentation.setScaleX(zoomLevel);
            canvasPresentation.setScaleY(zoomLevel);
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
            double newScale = canvasPresentation.getScaleX() * delta;

            //Limit for zooming in
            if (newScale > maxZoomFactor) {
                return;
            }

            //Scale canvas
            canvasPresentation.setScaleX(newScale);
            canvasPresentation.setScaleY(newScale);

            centerComponentAndUpdateGrid(newScale);
        }
    }

    /**
     * Zoom out with a delta of 1.2
     */
    public void zoomOut() {
        if (active) {
            double delta = 1.2;
            double newScale = canvasPresentation.getScaleX() / delta;

            //Limit for zooming out
            if (newScale < minZoomFactor) {
                return;
            }

            //Scale canvas
            canvasPresentation.setScaleX(newScale);
            canvasPresentation.setScaleY(newScale);

            centerComponentAndUpdateGrid(newScale);
        }
    }

    /**
     * Set the zoom multiplier to 1
     */
    public void resetZoom() {
        if (active) {
            canvasPresentation.setScaleX(1);
            canvasPresentation.setScaleY(1);

            //Center component
            centerComponentAndUpdateGrid(1);
        }
    }

    /**
     * Zoom in to fit the component on screen
     */
    public void zoomToFit() {
        if (active) {
            if (EcdarController.getActiveCanvasShellPresentation().getCanvasController().activeComponentPresentation == null) {
                resetZoom();
                return;
            }
            double newScale = Math.min(canvasPresentation.getWidth() / EcdarController.getActiveCanvasShellPresentation().getCanvasController().activeComponentPresentation.getWidth() - 0.1, canvasPresentation.getHeight() / EcdarController.getActiveCanvasShellPresentation().getCanvasController().activeComponentPresentation.getHeight() - 0.2); //0.1 for width and 0.2 for height added for margin

            //Scale canvas
            canvasPresentation.setScaleX(newScale);
            canvasPresentation.setScaleY(newScale);

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
        if(EcdarController.getActiveCanvasShellPresentation().getCanvasController().activeComponentPresentation != null){
            updateGrid(newScale, EcdarController.getActiveCanvasShellPresentation().getCanvasController().activeComponentPresentation);
        } else if (EcdarController.getActiveCanvasShellPresentation().getCanvasController().getActiveModel() != null) {
            // The canvas is currently showing an EcdarSystem object, so wee need to find the SystemPresentation in order to center it on screen
            SystemPresentation systemPresentation = null;

            for (Node node : EcdarController.getActiveCanvasShellPresentation().getCanvasController().root.getChildren()) {
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

    private void updateGrid(double newScale, ModelPresentation modelPresentation) {
        // Calculate the new x and y offsets needed to center the component
        double xOffset = newScale * canvasPresentation.getWidth() * 1.0f / 2 - newScale * modelPresentation.getWidth() * 1.0f / 2;
        double yOffset = newScale * canvasPresentation.getHeight() * 1.0f / 2 - newScale * modelPresentation.getHeight() * 1.0f / 2;

        modelPresentation.setTranslateX(xOffset);
        modelPresentation.setTranslateY(yOffset);

        // Redraw the grid based on the new scale and canvas size
        grid.updateGrid(newScale);
    }
}
