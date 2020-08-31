package ecdar.presentations;

import javafx.beans.binding.DoubleBinding;
import javafx.scene.Parent;
import javafx.scene.shape.Line;
import javafx.stage.Screen;

import java.util.ArrayList;

public class Grid extends Parent {
    public static final int GRID_SIZE = 10;
    static final int CORNER_SIZE = 4 * Grid.GRID_SIZE;
    public static final double TOOL_BAR_HEIGHT = CORNER_SIZE / 2;

    private final ArrayList<Line> horizontalLines = new ArrayList<>();
    private final ArrayList<Line> verticalLines = new ArrayList<>();

    public Grid(final int gridSize) {
        //The screen size in GridSlices multiplied by 3 to ensure that the screen is still covered when zoomed out
        int screenWidthInGridSlices = (int) (Screen.getPrimary().getBounds().getWidth() - (Screen.getPrimary().getBounds().getWidth() % gridSize)) * 3;
        int screenHeightInGridSlices = (int) (Screen.getPrimary().getBounds().getHeight() - (Screen.getPrimary().getBounds().getHeight() % gridSize)) * 3;

        setTranslateX(gridSize * 0.5);
        setTranslateY(gridSize * 0.5);

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

                // Add new lines (to cover the screen, with 1 line in margin in both ends)
                int i = -screenWidthInGridSlices;
                while (i * gridSize - gridSize < screenWidthInGridSlices) {
                    final Line line = new Line(i * gridSize, -screenHeightInGridSlices, i * gridSize, -screenHeightInGridSlices);
                    line.getStyleClass().add("grid-line");

                    final DoubleBinding parentXBinding = new DoubleBinding() {
                        {
                            super.bind(getParent().translateXProperty());
                        }

                        @Override
                        protected double computeValue() {
                            final int moveFactor = (int) (getParent().getTranslateX() / gridSize);
                            return -1 * moveFactor * gridSize + 0.5 * gridSize;
                        }
                    };

                    line.layoutXProperty().bind(parentXBinding);
                    line.startYProperty().bind(getParent().layoutYProperty().subtract(getParent().translateYProperty()).subtract(50)); // the 50 is a fix
                    line.endYProperty().bind(getParent().layoutYProperty().subtract(getParent().translateYProperty()).add(getScene().heightProperty()));

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

                // Add new lines (to cover the screen, with 1 line in margin in both ends)
                int i = -screenHeightInGridSlices;
                while (i * gridSize - gridSize < screenHeightInGridSlices) {
                    final Line line = new Line(-screenWidthInGridSlices, i * gridSize, screenWidthInGridSlices, i * gridSize);
                    line.getStyleClass().add("grid-line");

                    final DoubleBinding parentYBinding = new DoubleBinding() {
                        {
                            super.bind(getParent().translateYProperty());
                        }

                        @Override
                        protected double computeValue() {
                            final int moveFactor = (int) (getParent().getTranslateY() / gridSize);
                            return -1 * moveFactor * gridSize + 0.5 * gridSize;
                        }
                    };

                    line.layoutYProperty().bind(parentYBinding);
                    line.startXProperty().bind(getParent().layoutXProperty().subtract(getParent().translateXProperty()));
                    line.endXProperty().bind(getParent().layoutXProperty().subtract(getParent().translateXProperty()).add(getScene().widthProperty()));

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
