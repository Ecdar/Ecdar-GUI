package SW9.abstractions;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * A system root is the highest entity of a system.
 * It consists of components and other systems combined with
 * conjunction, composition, and quotient.
 * It has an x coordinate that enables the root to be dragged horizontally.
 */
public class SystemRoot {
    private final DoubleProperty x = new SimpleDoubleProperty(50d);

    public double getX() {
        return x.get();
    }

    public void setX(final double x) {
        this.x.set(x);
    }

    public DoubleProperty getXProperty() {
        return x;
    }
}
