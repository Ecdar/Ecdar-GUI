package ecdar.presentations;

import ecdar.controllers.EcdarController;
import javafx.application.Platform;
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
        setTranslateX(snap(this.getTranslateX()));
        setTranslateY(snap(this.getTranslateY()));

        // When the scene changes (goes from null to something) set update the grid
        sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Platform.runLater(() -> {
                    newValue.widthProperty().addListener((observable1 -> this.updateGrid(getScaleX())));
                    newValue.heightProperty().addListener((observable1 -> this.updateGrid(getScaleY())));
                });
            }
        });
        updateGrid(getScaleX());
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
     *
     * @param zoomLevel the scale in which to draw the grid
     */
    public void updateGrid(double zoomLevel) {
        setScaleX(zoomLevel);
        setScaleY(zoomLevel);

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
            double screenWidth = (int) snap(EcdarController.getActiveCanvasPresentation().getWidth() / zoomLevel);
            double screenHeight = (int) snap(EcdarController.getActiveCanvasPresentation().getHeight() / zoomLevel);

            // Add new vertical lines to cover the screen at the current zoom level
            int i = 0;
            int numberOfLine = (int) screenWidth / GRID_SIZE;
            while (i < numberOfLine) {
                Line line = new Line(i * GRID_SIZE, 0, i * GRID_SIZE, screenHeight);
                line.getStyleClass().add("grid-line");

                verticalLines.add(line);
                getChildren().add(line);
                i++;
            }

            // Add new horizontal lines to cover the screen at the current zoom level
            i = 0;
            numberOfLine = (int) screenHeight / GRID_SIZE;
            while (i < numberOfLine) {
                Line line = new Line(0, i * GRID_SIZE, screenWidth, i * GRID_SIZE);
                line.getStyleClass().add("grid-line");

                horizontalLines.add(line);
                getChildren().add(line);
                i++;
            }
        });
    }
}
