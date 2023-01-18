package ecdar.abstractions;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;

/**
 * A system root is the highest entity of a system.
 * It consists of components and other systems combined with
 * conjunction, composition, and quotient.
 * It has an x coordinate that enables the root to be dragged horizontally.
 * Its hidden id is 0.
 */
public class SystemRoot implements SystemElement {
    public static final int WIDTH = 60;

    private static final int HIDDEN_ID = 0;

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

    @Override
    public ObservableValue<? extends Number> getEdgeX() {
        return getXProperty().add(30);
    }

    @Override
    public ObservableValue<? extends Number> getEdgeY() {
        return new SimpleDoubleProperty(40 - 4d);
    }

    @Override
    public int getHiddenId() {
        return HIDDEN_ID;
    }
}
