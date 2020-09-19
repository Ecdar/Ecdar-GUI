package ecdar.presentations;

import javafx.scene.Parent;
import javafx.scene.shape.Line;
import javafx.stage.Screen;

public class Grid extends Parent {
    public static final int GRID_SIZE = 10;
    static final int CORNER_SIZE = 4 * Grid.GRID_SIZE;
    public static final double TOOL_BAR_HEIGHT = CORNER_SIZE * 0.5;

    public Grid(final int gridSize) {
        //The screen size in GridSlices multiplied by 3 to ensure that the screen is still covered when zoomed out
        int screenWidthInGridSlices = (int) (Screen.getPrimary().getBounds().getWidth() - (Screen.getPrimary().getBounds().getWidth() % gridSize));
        int screenHeightInGridSlices = (int) ((Screen.getPrimary().getBounds().getHeight() - (Screen.getPrimary().getBounds().getHeight() % gridSize)) * 1.2);

        setTranslateX(gridSize * 0.5);
        setTranslateY(gridSize * 0.5);

        // Add vertical lines to cover the screen, even when zoomed out
        int i = -screenWidthInGridSlices;
        while (i * gridSize - gridSize < screenWidthInGridSlices) {
            Line line = new Line(i * gridSize, -screenHeightInGridSlices, i * gridSize, screenHeightInGridSlices);
            line.getStyleClass().add("grid-line");
            i++;
        }

        // Add horizontal lines to cover the screen, even when zoomed out
        i = -screenHeightInGridSlices;
        while (i * gridSize - gridSize < screenHeightInGridSlices) {
            Line line = new Line(-screenWidthInGridSlices, i * gridSize, screenWidthInGridSlices, i * gridSize);
            line.getStyleClass().add("grid-line");
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
