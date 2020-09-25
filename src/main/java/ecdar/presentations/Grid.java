package ecdar.presentations;

import ecdar.utility.helpers.ZoomHelper;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.scene.Parent;
import javafx.scene.shape.Line;

import javax.sound.midi.SysexMessage;
import java.util.ArrayList;

public class Grid extends Parent {
    public static final int GRID_SIZE = 10;
    static final int CORNER_SIZE = 4 * Grid.GRID_SIZE;
    public static final double TOOL_BAR_HEIGHT = CORNER_SIZE * 0.5;
    private final ArrayList<Line> horizontalLines = new ArrayList<>();
    private final ArrayList<Line> verticalLines = new ArrayList<>();

    public Grid(final int gridSize) {
        // When the scene changes (goes from null to something)
        parentProperty().addListener((observable, oldParent, newParent) -> {
            newParent.scaleXProperty().addListener((observableValue, oldScale, newScale) -> {
                double newWidth = getParent().getLayoutBounds().getWidth() / newScale.doubleValue();
                double newHeight = getParent().getLayoutBounds().getHeight() / newScale.doubleValue();

                // Remove old lines
                while (!verticalLines.isEmpty()) {
                    final Line removeLine = verticalLines.get(0);
                    getChildren().remove(removeLine);
                    verticalLines.remove(removeLine);
                }

                // Add new lines (to cover the screen, with 1 line in margin in both ends)
                int i = -1;
                while (i * gridSize - gridSize < newWidth) {
                    final Line line = new Line(i * gridSize, -1, i * gridSize, newHeight);
                    line.getStyleClass().add("grid-line");
                    verticalLines.add(line);
                    i++;
                }
                verticalLines.forEach(line -> getChildren().add(line));

                // Remove old lines
                while (!horizontalLines.isEmpty()) {
                    final Line removeLine = horizontalLines.get(0);
                    getChildren().remove(removeLine);
                    horizontalLines.remove(removeLine);
                }

                // Add new lines (to cover the screen, with 1 line in margin in both ends)
                i = -1;
                while (i * gridSize - gridSize < newHeight) {
                    final Line line = new Line(-1, i * gridSize, newWidth, i * gridSize);
                    line.getStyleClass().add("grid-line");
                    horizontalLines.add(line);
                    i++;
                }
                horizontalLines.forEach(line -> getChildren().add(line));
                layoutXProperty().bind(getParent().layoutXProperty());
                layoutYProperty().bind(getParent().layoutYProperty());
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
