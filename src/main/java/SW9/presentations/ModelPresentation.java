package SW9.presentations;

import javafx.scene.shape.Polygon;

import static SW9.presentations.Grid.GRID_SIZE;

/**
 *
 */
public class ModelPresentation extends HighLevelModelPresentation {
    static final double CORNER_SIZE = 4 * GRID_SIZE;
    public static final double TOOL_BAR_HEIGHT = CORNER_SIZE / 2;

    static final Polygon TOP_LEFT_CORNER = new Polygon(
            0, 0,
            CORNER_SIZE + 2, 0,
            0, CORNER_SIZE + 2
    );
}
