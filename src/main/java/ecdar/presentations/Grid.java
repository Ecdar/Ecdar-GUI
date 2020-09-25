package ecdar.presentations;

import ecdar.utility.helpers.ZoomHelper;
import javafx.scene.Parent;
import javafx.scene.shape.Line;
import javafx.stage.Screen;

public class Grid extends Parent {
    public static final int GRID_SIZE = 10;
    static final int CORNER_SIZE = 4 * Grid.GRID_SIZE;
    public static final double TOOL_BAR_HEIGHT = CORNER_SIZE * 0.5;

    public Grid(final int gridSize) {
        //Get the screen size in GridSlices (height multiplied by 1.1 to ensure that the screen is still covered when zoomed out)
        int screenWidth = (int) (Screen.getPrimary().getBounds().getWidth() / ZoomHelper.minZoomFactor);
        int screenHeight = (int) (Screen.getPrimary().getBounds().getHeight() / ZoomHelper.minZoomFactor);

        setTranslateX(gridSize * 0.5);
        setTranslateY(gridSize * 0.5);

        // Add vertical lines to cover the screen, even when zoomed out
        int i = 0;
        while (i * gridSize - gridSize < screenWidth * 0.5) {
            Line line = new Line(i * gridSize, -screenHeight * 0.5, i * gridSize, screenHeight * 0.6);
            line.getStyleClass().add("grid-line");
            getChildren().add(line);
            i++;
        }

        // Add horizontal lines to cover the screen, even when zoomed out
        i = 0;
        while (i * gridSize - gridSize < screenHeight * 0.5) {
            Line line = new Line(-screenWidth * 0.5, i * gridSize, screenWidth * 0.5, i * gridSize);
            line.getStyleClass().add("grid-line");
            getChildren().add(line);
            i++;
        }
    }

    /**
     * Snap to the grid.
     * @param raw the raw value
     * @return the value after being snapped
     */
    public static double snap(final double raw) {
        return Math.round(raw / GRID_SIZE) * GRID_SIZE;
    }

    public void handleTranslateX(double oldValue, double newValue, double scale) {
        //Move the grid in the opposite direction of the canvas drag, to keep its location on screen
        this.setTranslateX(this.getTranslateX() + (newValue - oldValue) / -scale);
    }

    public void handleTranslateY(double oldValue, double newValue, double scale) {
        //Move the grid in the opposite direction of the canvas drag, to keep its location on screen
        this.setTranslateY(this.getTranslateY() + (newValue - oldValue) / -scale);
    }
}
