package ecdar.presentations;

import javafx.scene.Parent;
import javafx.scene.shape.Line;
import javafx.stage.Screen;

import java.util.ArrayList;

public class Grid extends Parent {
    public static final int GRID_SIZE = 10;
    static final int CORNER_SIZE = 4 * Grid.GRID_SIZE;
    public static final double TOOL_BAR_HEIGHT = CORNER_SIZE * 0.5;
    private final ArrayList<Line> horizontalLines = new ArrayList<>();
    private final ArrayList<Line> verticalLines = new ArrayList<>();

    public Grid() {
        //The screen size in GridSlices multiplied by 3 to ensure that the screen is still covered when zoomed out
        int screenWidthInGridSlices = (int) (Screen.getPrimary().getBounds().getWidth() - (Screen.getPrimary().getBounds().getWidth() % GRID_SIZE)) * 3;
        int screenHeightInGridSlices = (int) (Screen.getPrimary().getBounds().getHeight() - (Screen.getPrimary().getBounds().getHeight() % GRID_SIZE)) * 3;

        setTranslateX(GRID_SIZE * 0.5);
        setTranslateY(GRID_SIZE * 0.5);

        // When the scene changes (goes from null to something)
        sceneProperty().addListener((observable, oldScene, newScene) -> {
            // When the width of this scene is being updated
            newScene.widthProperty().addListener((observable1, oldWidth, newWidth) -> {
                // Remove old lines
                while (!verticalLines.isEmpty()) {
                    final Line removeLine = verticalLines.get(0);
                    getChildren().remove(removeLine);
                    verticalLines.remove(removeLine);
                }

                // Add new lines to cover the screen, even when zoomed out
                int i = -screenWidthInGridSlices;
                while (i * GRID_SIZE - GRID_SIZE < screenWidthInGridSlices) {
                    Line line = new Line(i * GRID_SIZE, -screenHeightInGridSlices, i * GRID_SIZE, screenHeightInGridSlices);
                    line.getStyleClass().add("grid-line");

                    verticalLines.add(line);
                    i++;
                }
                verticalLines.forEach(line -> getChildren().add(line));
            });

            // When the height of this scene is being updated
            newScene.heightProperty().addListener((observable1, oldHeight, newHeight) -> {
                // Remove old lines
                while (!horizontalLines.isEmpty()) {
                    final Line removeLine = horizontalLines.get(0);
                    getChildren().remove(removeLine);
                    horizontalLines.remove(removeLine);
                }

                // Add new lines to cover the screen, even when zoomed out
                int i = -screenHeightInGridSlices;
                while (i * GRID_SIZE - GRID_SIZE < screenHeightInGridSlices) {
                    Line line = new Line(-screenWidthInGridSlices, i * GRID_SIZE, screenWidthInGridSlices, i * GRID_SIZE);
                    line.getStyleClass().add("grid-line");

                    horizontalLines.add(line);
                    i++;
                }
                horizontalLines.forEach(line -> getChildren().add(line));
            });
        });
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
