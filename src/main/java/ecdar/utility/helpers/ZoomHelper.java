package ecdar.utility.helpers;

import ecdar.controllers.CanvasController;
import ecdar.presentations.CanvasPresentation;
import ecdar.presentations.Grid;

import static ecdar.presentations.Grid.GRID_SIZE;

public class ZoomHelper {
    private static CanvasPresentation canvasPresentation;
    private static Grid grid;

    public static void setCanvas(CanvasPresentation newCanvasPresentation) {
        canvasPresentation = newCanvasPresentation;
    }

    public static void setGrid(Grid newGrid) {
        grid = newGrid;
    }

    public static void zoomIn() {
        double newScale = canvasPresentation.getScaleX();
        double delta = 1.2;

        newScale *= delta;

        //Limit for zooming in
        if(newScale > 8){
            return;
        }

        canvasPresentation.setScaleX(newScale);
        canvasPresentation.setScaleY(newScale);
    }

    public static void zoomOut() {
        double newScale = canvasPresentation.getScaleX();
        double delta = 1.2;

        newScale /= delta;

        //Limit for zooming out
        if(newScale < 0.4){
            return;
        }

        canvasPresentation.setScaleX(newScale);
        canvasPresentation.setScaleY(newScale);
    }

    public static void resetZoom() {
        canvasPresentation.setScaleX(1);
        canvasPresentation.setScaleY(1);
    }

    public static void zoomToFit() {
        double newScale = Math.min(canvasPresentation.getWidth() / 100/*CanvasController.getActiveComponent().getWidth()*/ - 0.1, canvasPresentation.getHeight() / 100 /*CanvasController.getActiveComponent().getHeight()*/ - 0.2); //0.1 for width and 0.2 for height added for margin
        final double gridSize = GRID_SIZE * newScale;
        double xOffset = newScale * canvasPresentation.getWidth() * 1.0f / 2 - newScale * 100 /*CanvasController.getActiveComponent().getWidth()*/ * 1.0f / 2;
        double yOffset = newScale * canvasPresentation.getHeight() * 1.0f / 3 - newScale * (/*CanvasController.getActiveComponent().getHeight() */ newScale * 100) * 1.0f / 3; //The offset places the component a bit too high, so '-newScale * 100' is used to lower it a but

        canvasPresentation.setScaleX(newScale);
        canvasPresentation.setScaleY(newScale);

        canvasPresentation.setTranslateX(xOffset - (xOffset % gridSize) + gridSize * 0.5);
        canvasPresentation.setTranslateY(yOffset - (yOffset % gridSize) + gridSize * 0.5);

        grid.setTranslateX(gridSize * 0.5);
        grid.setTranslateY(gridSize * 0.5);
    }
}
