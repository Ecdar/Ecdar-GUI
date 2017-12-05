package SW9.abstractions;

import SW9.utility.UndoRedoStack;
import SW9.utility.colors.Color;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.util.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A model of a system
 */
public class SystemModel extends HighLevelModelObject {
    private static final AtomicInteger hiddenId = new AtomicInteger(0); // Used to generate unique IDs

    // Verification properties
    private final StringProperty description = new SimpleStringProperty("");

    public SystemModel() {
        setRandomColor();
    }

    // Styling properties
    private final Box box = new Box();

    public Box getBox() {
        return box;
    }

    public StringProperty getDescriptionProperty() {
        return description;
    }

    /**
     * Dyes the system.
     * @param color the color to dye with
     * @param intensity the intensity of the color
     */
    public void dye(final Color color, final Color.Intensity intensity) {
        final Color previousColor = colorProperty().get();
        final Color.Intensity previousColorIntensity = colorIntensityProperty().get();

        UndoRedoStack.pushAndPerform(() -> { // Perform
            // Color the component
            setColorIntensity(intensity);
            setColor(color);
        }, () -> { // Undo
            // Color the component
            setColorIntensity(previousColorIntensity);
            setColor(previousColor);
        }, String.format("Changed the color of %s to %s", this, color.name()), "color-lens");
    }
}
