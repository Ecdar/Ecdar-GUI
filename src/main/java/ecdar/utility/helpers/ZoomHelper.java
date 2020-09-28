package ecdar.utility.helpers;

import ecdar.presentations.CanvasPresentation;
import ecdar.presentations.Grid;

public class ZoomHelper {
    private static CanvasPresentation canvasPresentation;
    private static Grid grid;
    public static double minZoomFactor = 0.4;
    public static double maxZoomFactor = 8;

    public static void setCanvas(CanvasPresentation newCanvasPresentation) {
        canvasPresentation = newCanvasPresentation;
    }

    public static void setGrid(Grid newGrid) {
        grid = newGrid;
    }

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

        centerComponent(newScale);
    }

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
        final double actualHeight = canvasPresentation.getHeight() - Grid.TOOL_BAR_HEIGHT;
        double xOffset = newScale * canvasPresentation.getWidth() * 1.0f / 2 - newScale * canvasPresentation.getController().getActiveComponentPresentation().getWidth() * 1.0f / 2;
        double yOffset = newScale * actualHeight * 1.0f / 3 - newScale * canvasPresentation.getController().getActiveComponentPresentation().getHeight() * 1.0f / 3 + Grid.TOOL_BAR_HEIGHT / (1.0f / 3); //The offset places the component a bit too high, so 'canvasPresentation.getHeight() / 4' is used to lower it a but

        canvasPresentation.setTranslateX(xOffset - (xOffset % Grid.GRID_SIZE));
        canvasPresentation.setTranslateY(yOffset - (yOffset % Grid.GRID_SIZE));

        grid.setTranslateX(Grid.GRID_SIZE * 0.5);
        grid.setTranslateY(Grid.GRID_SIZE * 0.5);
    }
}
