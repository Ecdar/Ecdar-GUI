package ecdar.presentations;

import javafx.scene.shape.Polygon;

/**
 * Presentation for high level graphical models such as systems and components
 */
public abstract class ModelPresentation extends HighLevelModelPresentation {
    public static final int CORNER_SIZE = 40;
    public static final int TOOLBAR_HEIGHT = CORNER_SIZE / 2;
    public static final Polygon TOP_LEFT_CORNER = new Polygon(
            0, 0,
            CORNER_SIZE + 2, 0,
            0, CORNER_SIZE + 2
    );
}
