package ecdar.presentations;

import javafx.scene.Parent;
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

                //The screen size in GridSlices multiplied by 3 to ensure that the screen is still covered when zoomed out
                int screenWidth = (int) ((Screen.getPrimary().getBounds().getWidth() - (Screen.getPrimary().getBounds().getWidth() % GRID_SIZE)) * 2);
                int screenHeight = (int) ((Screen.getPrimary().getBounds().getHeight() - (Screen.getPrimary().getBounds().getHeight() % GRID_SIZE)) * 2);

                // Add new lines to cover the screen, even when zoomed out
                int i = 0;
                while (i < screenWidth / GRID_SIZE) {
                    Line line = new Line(i * GRID_SIZE, 0, i * GRID_SIZE, screenHeight);
                    line.getStyleClass().add("grid-line");

                    line.startXProperty().bind(newScene.widthProperty().add(i * GRID_SIZE).subtract(newScene.widthProperty().multiply(1.75)));
                    line.endXProperty().bind(newScene.widthProperty().add(i * GRID_SIZE).subtract(newScene.widthProperty().multiply(1.75)));
                    line.startYProperty().bind(newScene.heightProperty().divide(getParent().scaleYProperty()).multiply(-1).add(newScene.heightProperty().divide(2)));
                    line.endYProperty().bind(newScene.heightProperty().divide(getParent().scaleYProperty()).add(newScene.heightProperty().divide(2)));

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

                //The screen size in GridSlices multiplied by 3 to ensure that the screen is still covered when zoomed out
                int screenWidth = (int) ((Screen.getPrimary().getBounds().getWidth() - (Screen.getPrimary().getBounds().getWidth() % GRID_SIZE)) * 2);
                int screenHeight = (int) ((Screen.getPrimary().getBounds().getHeight() - (Screen.getPrimary().getBounds().getHeight() % GRID_SIZE)) * 2);

                // Add new lines to cover the screen, even when zoomed out
                int i = 0;
                while (i < screenHeight / GRID_SIZE) {
                    Line line = new Line(0, i * GRID_SIZE, screenWidth, i * GRID_SIZE);
                    line.getStyleClass().add("grid-line");

                    line.startXProperty().bind(newScene.widthProperty().divide(getParent().scaleXProperty()).multiply(-1).add(newScene.widthProperty().divide(2)));
                    line.endXProperty().bind(newScene.widthProperty().divide(getParent().scaleXProperty()).add(newScene.widthProperty().divide(2)));
                    line.startYProperty().bind(newScene.heightProperty().add(i * GRID_SIZE).subtract(newScene.heightProperty().multiply(1.75)));
                    line.endYProperty().bind(newScene.heightProperty().add(i * GRID_SIZE).subtract(newScene.heightProperty().multiply(1.75)));

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
