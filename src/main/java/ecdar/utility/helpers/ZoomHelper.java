package ecdar.utility.helpers;

import ecdar.controllers.CanvasController;
import ecdar.presentations.CanvasPresentation;
import ecdar.presentations.Grid;

public class ZoomHelper {
    private static CanvasPresentation canvasPresentation;
    private static Grid grid;
    public static double minZoomFactor = 0.4;
    public static double maxZoomFactor = 4;

    /**
     * Set the CanvasPresentation of the grid and add listeners for both the width and height of the new CanvasPresentation
     * @param newCanvasPresentation the new CanvasPresentation
     */
    public static void setCanvas(CanvasPresentation newCanvasPresentation) {
        canvasPresentation = newCanvasPresentation;

        canvasPresentation.heightProperty().addListener((observable -> ZoomHelper.centerComponentAndUpdateGrid(canvasPresentation.scaleXProperty().doubleValue())));
        canvasPresentation.widthProperty().addListener((observable -> ZoomHelper.centerComponentAndUpdateGrid(canvasPresentation.scaleXProperty().doubleValue())));
    }

    public static void setGrid(Grid newGrid) {
        grid = newGrid;
    }
    /**
     * Zoom in with a delta of 1.2
     */
    public static void zoomIn() {
        double delta = 1.2;
        double newScale = canvasPresentation.getScaleX() * delta;

        //Limit for zooming in
        if(newScale > maxZoomFactor){
            return;
        }

        //Scale canvas
        canvasPresentation.setScaleX(newScale);
        canvasPresentation.setScaleY(newScale);

        centerComponentAndUpdateGrid(newScale);
    }

    /**
     * Zoom out with a delta of 1.2
     */
    public static void zoomOut() {
        double delta = 1.2;
        double newScale = canvasPresentation.getScaleX() / delta;

        //Limit for zooming out
        if(newScale < minZoomFactor){
            return;
        }

        //Scale canvas
        canvasPresentation.setScaleX(newScale);
        canvasPresentation.setScaleY(newScale);

        centerComponentAndUpdateGrid(newScale);
    }

    /**
     * Set the zoom multiplier to 1
     */
    public static void resetZoom() {
        canvasPresentation.setScaleX(1);
        canvasPresentation.setScaleY(1);

        //Center component
        centerComponentAndUpdateGrid(1);
    }

    /**
     * Zoom in to fit the component on screen
     */
    public static void zoomToFit() {
        double newScale = Math.min(canvasPresentation.getWidth() / CanvasController.activeComponentPresentation.getWidth() - 0.1, canvasPresentation.getHeight() / CanvasController.activeComponentPresentation.getHeight() - 0.2); //0.1 for width and 0.2 for height added for margin

        //Scale canvas
        canvasPresentation.setScaleX(newScale);
        canvasPresentation.setScaleY(newScale);

        centerComponentAndUpdateGrid(newScale);
    }

    /**
     * Method for centering the active component on screen and redrawing the grid to fill the screen
     * @param newScale the scale in which to redraw the grid and place the component based on
     */
    public static void centerComponentAndUpdateGrid(double newScale){
        // Check added to avoid NullPointerException
        if(CanvasController.activeComponentPresentation != null){
            // Calculate the new x and y offsets needed to center the component
            double xOffset = newScale * canvasPresentation.getWidth() * 1.0f / 2 - newScale * CanvasController.activeComponentPresentation.getWidth() * 1.0f / 2;
            double yOffset = newScale * canvasPresentation.getHeight() * 1.0f / 3 - newScale * CanvasController.activeComponentPresentation.getHeight() * 1.0f / 3 + newScale * Grid.TOOL_BAR_HEIGHT * 1.0f / 3;

            // Center the component based on the offsets
            canvasPresentation.setTranslateX(Grid.snap(xOffset));
            canvasPresentation.setTranslateY(Grid.snap(yOffset));

            // Redraw the grid based on the new scale and canvas size
            grid.updateGrid(newScale, canvasPresentation.getWidth(), canvasPresentation.getHeight());
        }
    }
}
