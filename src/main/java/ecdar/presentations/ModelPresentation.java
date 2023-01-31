package ecdar.presentations;

import ecdar.Ecdar;
import ecdar.abstractions.Box;
import ecdar.abstractions.HighLevelModelObject;
import ecdar.controllers.EcdarController;
import ecdar.controllers.ModelController;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.StringValidator;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

import static ecdar.presentations.Grid.GRID_SIZE;

/**
 * Presentation for high level graphical models such as systems and components
 */
public abstract class ModelPresentation extends HighLevelModelPresentation {
    public static final Polygon TOP_LEFT_CORNER = new Polygon(
            0, 0,
            Grid.CORNER_SIZE + 2, 0,
            0, Grid.CORNER_SIZE + 2
    );
}
