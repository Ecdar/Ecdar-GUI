package SW9.abstractions;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A model of a system
 */
public class System extends HighLevelModelObject {
    private static final double INITIAL_HEIGHT = 600d;
    private static final double INITIAL_WIDTH = 450d;

    private static final AtomicInteger hiddenId = new AtomicInteger(0); // Used to generate unique IDs

    // Verification properties
    private final StringProperty description = new SimpleStringProperty("");

    // Styling properties
    private final DoubleProperty width = new SimpleDoubleProperty(INITIAL_WIDTH);
    private final DoubleProperty height = new SimpleDoubleProperty(INITIAL_HEIGHT);

}
