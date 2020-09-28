package ecdar.presentations;

import javafx.scene.Parent;
import javafx.scene.shape.Line;

import java.util.ArrayList;

public class Grid extends Parent {
    public static final int GRID_SIZE = 10;
    static final int CORNER_SIZE = 4 * Grid.GRID_SIZE;
    public static final double TOOL_BAR_HEIGHT = CORNER_SIZE * 0.5;
    private final ArrayList<Line> horizontalLines = new ArrayList<>();
    private final ArrayList<Line> verticalLines = new ArrayList<>();

    public Grid() {
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

                // Add new lines (to cover the screen, with 1 line in margin in both ends)
                int i = -1;
                while (i * GRID_SIZE - GRID_SIZE < newWidth.doubleValue()) {
                    final Line line = new Line(i * GRID_SIZE, 200, i * GRID_SIZE, 300);
                    line.getStyleClass().add("grid-line");

                    line.startYProperty().bind(getParent().layoutYProperty().subtract(getParent().translateYProperty()).subtract(50)); // the 50 is a fix
                    line.endYProperty().bind(getParent().layoutYProperty().subtract(getParent().translateYProperty()).add(getScene().heightProperty()));

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

                // Add new lines (to cover the screen, with 1 line in margin in both ends)
                int i = -1;
                while (i * GRID_SIZE - GRID_SIZE < newHeight.doubleValue()) {
                    final Line line = new Line(200, i * GRID_SIZE, 300, i * GRID_SIZE);
                    line.getStyleClass().add("grid-line");

                    line.startXProperty().bind(getParent().layoutXProperty().subtract(getParent().translateXProperty()));
                    line.endXProperty().bind(getParent().layoutXProperty().subtract(getParent().translateXProperty()).add(getScene().widthProperty()));

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
