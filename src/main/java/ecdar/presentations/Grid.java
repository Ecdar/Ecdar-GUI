package ecdar.presentations;

import ecdar.abstractions.Component;
import ecdar.controllers.CanvasController;
import ecdar.controllers.EcdarController;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.shape.Line;
import javafx.stage.Screen;
import org.apache.batik.gvt.CanvasGraphicsNode;

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
            updateGrid(1, Screen.getPrimary().getBounds().getWidth(), Screen.getPrimary().getBounds().getHeight());
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

    public void updateGrid(double scale, double canvasWidth, double canvasHeight) {
        // The given size of the canvas divided by the given scale
        // Divided by 1.8 as the grid is drawn from the middle and to draw it slightly outside the screen
        double screenWidth = (int) Grid.snap(canvasWidth / scale / 1.8);
        double screenHeight = (int) Grid.snap(canvasHeight / scale / 1.8);

        Platform.runLater(() -> {
            // Remove old lines
            while (!verticalLines.isEmpty()) {
                final Line removeLine = verticalLines.get(0);
                getChildren().remove(removeLine);
                verticalLines.remove(removeLine);
            }
            // Add new lines to cover the screen, even when zoomed out
            int i = (int) -screenHeight / (GRID_SIZE / 2);
            int numberOfLine = (int) screenWidth / GRID_SIZE;
            while (i < numberOfLine) {
                Line line = new Line(i * GRID_SIZE, -screenHeight, i * GRID_SIZE, screenHeight);
                line.getStyleClass().add("grid-line");

                verticalLines.add(line);
                getChildren().add(line);
                i++;
            }

            // Remove old lines
            while (!horizontalLines.isEmpty()) {
                final Line removeLine = horizontalLines.get(0);
                getChildren().remove(removeLine);
                horizontalLines.remove(removeLine);
            }

            // Add new lines to cover the screen, even when zoomed out
            i = (int) -screenHeight / (GRID_SIZE / 2);
            numberOfLine = (int) screenHeight / GRID_SIZE;
            while (i < numberOfLine) {
                Line line = new Line(-screenWidth, i * GRID_SIZE, screenWidth, i * GRID_SIZE);
                line.getStyleClass().add("grid-line");

                horizontalLines.add(line);
                getChildren().add(line);
                i++;
            }
        });

        System.out.println("v: " + this.verticalLines.size() + " h: " + this.horizontalLines.size());

        // Center the grid on the screen
        // Using GRID_SIZE for small corrections as the grid is not directly centered on the screen
        setTranslateX(Grid.snap(CanvasController.activeComponentPresentation.getWidth() / 2 - GRID_SIZE) + GRID_SIZE * 0.5);
        setTranslateY(Grid.snap(CanvasController.activeComponentPresentation.getHeight() / 2 + TOOL_BAR_HEIGHT * 4) + GRID_SIZE * 0.5);
    }
}
