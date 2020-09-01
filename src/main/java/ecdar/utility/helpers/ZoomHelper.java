package ecdar.utility.helpers;

import ecdar.presentations.CanvasPresentation;
import ecdar.presentations.Grid;

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

        //Scale canvas
        canvasPresentation.setScaleX(newScale);
        canvasPresentation.setScaleY(newScale);

        centerComponent(newScale);
    }

    public static void zoomOut() {
        double newScale = canvasPresentation.getScaleX();
        double delta = 1.2;

        newScale /= delta;

        //Limit for zooming out
        if(newScale < 0.4){
            return;
        }

        //Scale canvas
        canvasPresentation.setScaleX(newScale);
        canvasPresentation.setScaleY(newScale);

        centerComponent(newScale);
    }

    public static void resetZoom() {
        canvasPresentation.setScaleX(1);
        canvasPresentation.setScaleY(1);

        //Center component
        centerComponent(1);
    }

    public static void zoomToFit() {
        double newScale = Math.min(canvasPresentation.getWidth() / canvasPresentation.getController().getActiveComponentPresentation().getWidth() - 0.1, canvasPresentation.getHeight() / canvasPresentation.getController().getActiveComponentPresentation().getHeight() - 0.2); //0.1 for width and 0.2 for height added for margin

        //Scale canvas
        canvasPresentation.setScaleX(newScale);
        canvasPresentation.setScaleY(newScale);

        centerComponent(newScale);
    }

    private static void centerComponent(double newScale){
        //Center component
        final double gridSize = Grid.GRID_SIZE * newScale;
        double xOffset = newScale * canvasPresentation.getWidth() * 1.0f / 2 - newScale * canvasPresentation.getController().getActiveComponentPresentation().getWidth() * 1.0f / 2;
        double yOffset = newScale * canvasPresentation.getHeight() * 1.0f / 3 - newScale * (canvasPresentation.getController().getActiveComponentPresentation().getHeight() - newScale * 100) * 1.0f / 3; //The offset places the component a bit too high, so '-newScale * 100' is used to lower it a but

        canvasPresentation.setTranslateX(xOffset - (xOffset % gridSize) + gridSize * 0.5);
        canvasPresentation.setTranslateY(yOffset - (yOffset % gridSize) + gridSize * 0.5);

        //Make sure the grid stays on screen
        grid.setTranslateX(gridSize * 0.5);
        grid.setTranslateY(gridSize * 0.5);
    }
}
