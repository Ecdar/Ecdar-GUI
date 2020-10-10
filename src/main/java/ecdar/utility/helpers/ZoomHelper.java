package ecdar.utility.helpers;

import ecdar.controllers.CanvasController;
import ecdar.presentations.CanvasPresentation;
import ecdar.presentations.Grid;

public class ZoomHelper {
    private static CanvasPresentation canvasPresentation;
    private static Grid grid;
    public static double minZoomFactor = 0.4;
    public static double maxZoomFactor = 4;

    public static void setCanvas(CanvasPresentation newCanvasPresentation) {
        canvasPresentation = newCanvasPresentation;

        canvasPresentation.heightProperty().addListener((observable -> ZoomHelper.centerComponentAndUpdateGrid(canvasPresentation.scaleXProperty().doubleValue())));
        canvasPresentation.widthProperty().addListener((observable -> ZoomHelper.centerComponentAndUpdateGrid(canvasPresentation.scaleXProperty().doubleValue())));
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

        centerComponentAndUpdateGrid(newScale);
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

        centerComponentAndUpdateGrid(newScale);
    }

    public static void resetZoom() {
        canvasPresentation.setScaleX(1);
        canvasPresentation.setScaleY(1);

        //Center component
        centerComponentAndUpdateGrid(1);
    }

    public static void zoomToFit() {
        double newScale = Math.min(canvasPresentation.getWidth() / canvasPresentation.getController().getActiveComponentPresentation().getWidth() - 0.1, canvasPresentation.getHeight() / canvasPresentation.getController().getActiveComponentPresentation().getHeight() - 0.2); //0.1 for width and 0.2 for height added for margin

        //Scale canvas
        canvasPresentation.setScaleX(newScale);
        canvasPresentation.setScaleY(newScale);

        centerComponentAndUpdateGrid(newScale);
    }

    public static void centerComponentAndUpdateGrid(double newScale){
        //Center component
        double xOffset = newScale * canvasPresentation.getWidth() * 1.0f / 2 - newScale * CanvasController.activeComponentPresentation.getWidth() * 1.0f / 2;
        double yOffset = newScale * canvasPresentation.getHeight() * 1.0f / 3 - newScale * CanvasController.activeComponentPresentation.getHeight() * 1.0f / 3 + Grid.TOOL_BAR_HEIGHT / (1.0f / 3);

        canvasPresentation.setTranslateX(xOffset - (xOffset % Grid.GRID_SIZE));
        canvasPresentation.setTranslateY(yOffset - (yOffset % Grid.GRID_SIZE));

        grid.updateGrid(newScale, canvasPresentation.getWidth(), canvasPresentation.getHeight());
    }
}
