package ecdar.presentations;

import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Parent;
import javafx.scene.shape.Line;

import java.util.ArrayList;

public class Grid extends Parent {
    public static final int GRID_SIZE = 10;
    static final int CORNER_SIZE = 4 * Grid.GRID_SIZE;
    public static final double TOOL_BAR_HEIGHT = CORNER_SIZE * 0.5;
    private final ArrayList<Line> horizontalLines = new ArrayList<>();
    private final ArrayList<Line> verticalLines = new ArrayList<>();
    private final DoubleProperty width = new SimpleDoubleProperty();
    private final DoubleProperty height = new SimpleDoubleProperty();

    public Grid() {
        // When the scene changes (goes from null to something) set update the grid
        sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Platform.runLater(() -> {
                    newValue.widthProperty().addListener((observable1, oldValue1, newValue1) -> {
                        updateGrid();
                        width.set(newValue1.doubleValue());
                    });
                    newValue.heightProperty().addListener((observable1, oldValue1, newValue1) -> {
                        updateGrid();
                        height.set(newValue1.doubleValue());
                    });
                });
            }
        });

        updateGrid();
    }

    /**
     * Snap to the grid.
     *
     * @param raw the raw value
     * @return the value after being snapped
     */
    public static double snap(final double raw) {
        return Math.round(raw / GRID_SIZE) * GRID_SIZE;
    }

    /**
     * Redraw the grid in center of the screen
     */
    public void updateGrid() {
        // Remove old vertical lines
        while (!verticalLines.isEmpty()) {
            final Line removeLine = verticalLines.get(0);
            getChildren().remove(removeLine);
            verticalLines.remove(removeLine);
        }

        // Remove old horizontal lines
        while (!horizontalLines.isEmpty()) {
            final Line removeLine = horizontalLines.get(0);
            getChildren().remove(removeLine);
            horizontalLines.remove(removeLine);
        }

        Platform.runLater(() -> {
            double scaledWidth = (int) snap(width.get() / getScaleX());
            double scaledHeight = (int) snap(height.get() / getScaleX());

            // Add new vertical lines to cover the screen at the current zoom level
            int i = 0;
            int numberOfLine = (int) scaledWidth / GRID_SIZE;
            while (i < numberOfLine) {
                Line line = new Line(i * GRID_SIZE, 0, i * GRID_SIZE, scaledHeight);
                line.getStyleClass().add("grid-line");

                verticalLines.add(line);
                getChildren().add(line);
                i++;
            }

            // Add new horizontal lines to cover the screen at the current zoom level
            i = 0;
            numberOfLine = (int) scaledHeight / GRID_SIZE;
            while (i < numberOfLine) {
                Line line = new Line(0, i * GRID_SIZE, scaledWidth, i * GRID_SIZE);
                line.getStyleClass().add("grid-line");

                horizontalLines.add(line);
                getChildren().add(line);
                i++;
            }
        });
    }
}
