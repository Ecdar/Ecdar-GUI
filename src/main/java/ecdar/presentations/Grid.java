package ecdar.presentations;

import ecdar.controllers.EcdarController;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import java.util.ArrayList;

public class Grid extends Parent {
    public static final int GRID_SIZE = 10;
    static final int CORNER_SIZE = 4 * Grid.GRID_SIZE;
    public static final double TOOL_BAR_HEIGHT = CORNER_SIZE * 0.5;
    private final ArrayList<Line> horizontalLines = new ArrayList<>();
    private final ArrayList<Line> verticalLines = new ArrayList<>();

    public Grid() {
        setTranslateX(GRID_SIZE * 0.5);
        setTranslateY(GRID_SIZE * 0.5);

        // When the scene changes (goes from null to something) set update the grid
        sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                updateGrid(1);
                Platform.runLater(() -> {
                    newValue.widthProperty().addListener((observable1 -> this.updateGrid(getScaleX())));
                    newValue.heightProperty().addListener((observable1 -> this.updateGrid(getScaleY())));
                });
            }
        });
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
        // The given size of the canvas divided by the given zoomLevel (multiplied by 6 to ensure that the grid covers the full canvas)
        double screenWidth = (int) Grid.snap(EcdarController.getActiveCanvasShellPresentation().getWidth() / zoomLevel); // ToDo Grid: Possibly add multiplier
        double screenHeight = (int) Grid.snap(EcdarController.getActiveCanvasShellPresentation().getHeight() / zoomLevel);
//
//        if (this.getTranslateX() != Grid.snap(this.getTranslateX())) {
//            this.setTranslateX(Grid.snap(this.getTranslateX()) + GRID_SIZE * 0.5);
//        }
//
//        if (this.getTranslateY() != Grid.snap(this.getTranslateY())) {
//            this.setTranslateY(Grid.snap(this.getTranslateY()) + GRID_SIZE * 0.5);
//        }

        Platform.runLater(() -> {
            // Remove old vertical lines
            while (!verticalLines.isEmpty()) {
                final Line removeLine = verticalLines.get(0);
                getChildren().remove(removeLine);
                verticalLines.remove(removeLine);
            }

            // Add new vertical lines to cover the screen at the current zoom level
            int i = (int) -screenHeight / GRID_SIZE;
            int numberOfLine = (int) screenWidth / GRID_SIZE;
            while (i < numberOfLine) {
                Line line = new Line(i * GRID_SIZE, -screenHeight, i * GRID_SIZE, screenHeight);
                line.getStyleClass().add("grid-line");

                verticalLines.add(line);
                getChildren().add(line);
                i++;
            }

            // Remove old horizontal lines
            while (!horizontalLines.isEmpty()) {
                final Line removeLine = horizontalLines.get(0);
                getChildren().remove(removeLine);
                horizontalLines.remove(removeLine);
            }

            // Add new horizontal lines to cover the screen at the current zoom level
            i = (int) -screenHeight / GRID_SIZE;
            numberOfLine = (int) screenHeight / GRID_SIZE;
            while (i < numberOfLine) {
                Line line = new Line(-screenWidth, i * GRID_SIZE, screenWidth, i * GRID_SIZE);
                line.getStyleClass().add("grid-line");

                horizontalLines.add(line);
                getChildren().add(line);
                i++;
            }
        });
    }
}
