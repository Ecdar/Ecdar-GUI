package ecdar.presentations;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
        setTranslateX(GRID_SIZE * 0.5);
        setTranslateY(GRID_SIZE * 0.5);

        // When the scene changes (goes from null to something) set update the grid
        sceneProperty().addListener((observable, oldValue, newValue) -> {
            updateGrid(1, getScene().getWidth(), getScene().getHeight());
            newValue.widthProperty().addListener((observable1 -> this.updateGrid(getParent().getScaleX(), getParent().getLayoutBounds().getWidth(), getParent().getLayoutBounds().getHeight())));
            newValue.heightProperty().addListener((observable1 -> this.updateGrid(getParent().getScaleY(), getParent().getLayoutBounds().getWidth(), getParent().getLayoutBounds().getHeight())));
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
        this.setTranslateX(this.getTranslateX() - (newValue - oldValue) / scale);
    }

    public void handleTranslateY(double oldValue, double newValue, double scale) {
        //Move the grid in the opposite direction of the canvas drag, to keep its location on screen
        this.setTranslateY(this.getTranslateY() - (newValue - oldValue) / scale);
    }

    public void updateGrid(double scale, double width, double height) {
        //The screen size in GridSlices multiplied by 3 to ensure that the screen is still covered when zoomed out
        int screenWidth = (int) (width / scale - ((width / scale) % GRID_SIZE) + GRID_SIZE / 2);
        int screenHeight = (int) (height / scale - ((height / scale) % GRID_SIZE) + GRID_SIZE / 2);

        Platform.runLater(() -> {
            // Remove old lines
            while (!verticalLines.isEmpty()) {
                final Line removeLine = verticalLines.get(0);
                getChildren().remove(removeLine);
                verticalLines.remove(removeLine);
            }
            // Add new lines to cover the screen, even when zoomed out
            int i = -GRID_SIZE * 10;
            int numberOfLine = screenWidth / GRID_SIZE;
            while (i < numberOfLine) {
                Line line = new Line(i * GRID_SIZE, 0, i * GRID_SIZE, screenHeight);
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

            // Add new lines to cover the screen, even when zoomed out
            i = -GRID_SIZE * 10;
            numberOfLine = screenHeight / GRID_SIZE;
            while (i < numberOfLine) {
                Line line = new Line(0, i * GRID_SIZE, screenWidth, i * GRID_SIZE);
                line.getStyleClass().add("grid-line");

                horizontalLines.add(line);
                i++;
            }
            horizontalLines.forEach(line -> getChildren().add(line));
        });

        System.out.println("v: " + this.verticalLines.size() + " h: " + this.horizontalLines.size());

        //Center the grid on the screen
        setTranslateX(((width - screenWidth) / 2));
        setTranslateY(((height - screenHeight) / 2.4));
    }
}
