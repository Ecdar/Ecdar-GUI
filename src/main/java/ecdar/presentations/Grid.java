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
        // Update grid when scene size changes
        sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Platform.runLater(() -> {
                    newValue.widthProperty().addListener(observable1 -> updateGrid());
                    newValue.heightProperty().addListener(observable1 -> updateGrid());
                });
            }
        });

        // Update grid when scaling/zoom is changed
        this.scaleXProperty().addListener(observable -> updateGrid());

        setTranslateX(GRID_SIZE * 0.5);
        updateGrid();
    }

    /**
     * Snap to the grid.
     *
     * @param raw the raw value
     * @return the value after being snapped
     */
    public static double snap(final double raw) {
        return raw - raw % GRID_SIZE;
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
            int scaledWidth = (int) snap(width.get() / getScaleX());
            int scaledHeight = (int) snap(height.get() / getScaleY());

            // Add new vertical lines to cover the screen at the current zoom level
            int i = -scaledWidth / GRID_SIZE;
            int numberOfLine = scaledWidth / GRID_SIZE;
            while (i < numberOfLine) {
                Line line = new Line(i * GRID_SIZE, -scaledHeight, i * GRID_SIZE, scaledHeight);
                line.getStyleClass().add("grid-line");

                verticalLines.add(line);
                getChildren().add(line);
                i++;
            }

            // Add new horizontal lines to cover the screen at the current zoom level
            i = -scaledHeight / GRID_SIZE;
            numberOfLine = scaledHeight / GRID_SIZE;
            while (i < numberOfLine) {
                Line line = new Line(-scaledWidth, i * GRID_SIZE, scaledWidth, i * GRID_SIZE);
                line.getStyleClass().add("grid-line");

                horizontalLines.add(line);
                getChildren().add(line);
                i++;
            }
        });
    }

    public void bindSize(DoubleProperty width, DoubleProperty height) {
        this.width.bind(width.multiply(4));
        this.height.bind(height.multiply(4));
    }
}
