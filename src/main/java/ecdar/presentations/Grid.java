package ecdar.presentations;

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
        setTranslateX(GRID_SIZE * 0.5);
        setTranslateY(GRID_SIZE * 0.5);

        // When the scene changes (goes from null to something) set update the grid
        sceneProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue != null) {
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
     * @param raw the raw value
     * @return the value after being snapped
     */
    public static double snap(final double raw) {
        return Math.round(raw / GRID_SIZE) * GRID_SIZE;
    }

    /**
     * Moved the grid in the opposite direction of the oldValue - newValue change in relation to the scale
     * @param oldValue the start position of the translation
     * @param newValue the end position of the translation
     * @param scale the scale in which to move the grid
     */
    public void handleTranslateX(double oldValue, double newValue, double scale) {
        //Move the grid in the opposite direction of the canvas drag, to keep its location on screen
        this.setTranslateX(this.getTranslateX() - (newValue - oldValue) / scale);
    }

    /**
     * Moved the grid in the opposite direction of the oldValue - newValue change in relation to the scale
     * @param oldValue the start position of the translation
     * @param newValue the end position of the translation
     * @param scale the scale in which to move the grid
     */
    public void handleTranslateY(double oldValue, double newValue, double scale) {
        //Move the grid in the opposite direction of the canvas drag, to keep its location on screen
        this.setTranslateY(this.getTranslateY() - (newValue - oldValue) / scale);
    }

    /**
     * Redraw the grid in center of the screen
     * @param scale the scale in which to draw the grid
     */
    public void updateGrid(double scale) {
        // The given size of the canvas divided by the given scale
        double screenWidth = (int) Grid.snap(((CanvasShellPresentation) getParent().getParent()).getWidth() / scale * 4);
        double screenHeight = (int) Grid.snap(((CanvasShellPresentation) getParent().getParent()).getHeight() / scale * 4);

        if (this.getTranslateX() != Grid.snap(this.getTranslateX())) {
            this.setTranslateX(Grid.snap(this.getTranslateX()) + GRID_SIZE * 0.5);
        }

        if (this.getTranslateY() != Grid.snap(this.getTranslateY())) {
            this.setTranslateY(Grid.snap(this.getTranslateY()) + GRID_SIZE * 0.5);
        }

        Platform.runLater(() -> {
            // Remove old vertical lines
            while (!verticalLines.isEmpty()) {
                final Line removeLine = verticalLines.get(0);
                getChildren().remove(removeLine);
                verticalLines.remove(removeLine);
            }

            // Add new vertical lines to cover the screen at the current zoom level
            int i = (int) -screenHeight / (GRID_SIZE * 4);
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
            i = (int) -screenHeight / (GRID_SIZE * 4);
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
