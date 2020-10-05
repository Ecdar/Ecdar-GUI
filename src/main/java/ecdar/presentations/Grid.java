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
        this.setTranslateX(this.getTranslateX() + (newValue - oldValue) / -scale);
    }

    public void handleTranslateY(double oldValue, double newValue, double scale) {
        //Move the grid in the opposite direction of the canvas drag, to keep its location on screen
        this.setTranslateY(this.getTranslateY() + (newValue - oldValue) / -scale);
    }

    public void updateGrid(double scale, double width, double height) {
        Platform.runLater(() -> {
            //The screen size in GridSlices multiplied by 3 to ensure that the screen is still covered when zoomed out
            int screenWidth = (int) ((Screen.getPrimary().getBounds().getWidth() / scale - ((Screen.getPrimary().getBounds().getWidth() / scale) % GRID_SIZE) + GRID_SIZE / 2));
            int screenHeight = (int) ((Screen.getPrimary().getBounds().getHeight() / scale - ((Screen.getPrimary().getBounds().getHeight() / scale) % GRID_SIZE) + GRID_SIZE / 2));

            // Remove old lines
            while (!verticalLines.isEmpty()) {
                final Line removeLine = verticalLines.get(0);
                getChildren().remove(removeLine);
                verticalLines.remove(removeLine);
            }
            // Add new lines to cover the screen, even when zoomed out
            int i = (int) (-GRID_SIZE * 10 * scale);
            int numberOfLine = screenWidth / GRID_SIZE;
            while (i < numberOfLine) {
                Line line = new Line(i * GRID_SIZE, -numberOfLine, i * GRID_SIZE, screenHeight);
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
            i = (int) (-GRID_SIZE * 10 * scale);
            numberOfLine = screenHeight / GRID_SIZE;
            while (i < numberOfLine) {
                Line line = new Line(-numberOfLine, i * GRID_SIZE, screenWidth, i * GRID_SIZE);
                line.getStyleClass().add("grid-line");

                horizontalLines.add(line);
                i++;
            }
            horizontalLines.forEach(line -> getChildren().add(line));
        });

        //Center the grid on the screen
        double newHeight = (height / scale - (height / scale)) * 1.2;
        double newWidth = (height / scale - (width / scale) / 1.1);

        setTranslateX(newWidth - (newWidth % Grid.GRID_SIZE) + Grid.GRID_SIZE);
        setTranslateY(newHeight - (newHeight % Grid.GRID_SIZE) + Grid.GRID_SIZE);
    }
}
