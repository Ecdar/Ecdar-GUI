package SW9.abstractions;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

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
}
