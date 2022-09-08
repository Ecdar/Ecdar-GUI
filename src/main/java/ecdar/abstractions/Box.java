package ecdar.abstractions;

import com.google.gson.JsonObject;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * A box with a two dimensional coordinate (top left corner) and width and height.
 */
public class Box {
    private static final double INITIAL_HEIGHT = 600d;
    private static final double INITIAL_WIDTH = 450d;
    private static final String X = "x";
    private static final String Y = "y";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";

    private final DoubleProperty x = new SimpleDoubleProperty(0d);
    private final DoubleProperty y = new SimpleDoubleProperty(0d);
    private final DoubleProperty width = new SimpleDoubleProperty(INITIAL_WIDTH);
    private final DoubleProperty height = new SimpleDoubleProperty(INITIAL_HEIGHT);

    public double getX() {
        return x.get();
    }

    public void setX(final double x) {
        this.x.set(x);
    }

    public DoubleProperty getXProperty() {
        return x;
    }

    public double getY() {
        return y.get();
    }

    public void setY(final double y) {
        this.y.set(y);
    }

    public DoubleProperty getYProperty() {
        return y;
    }

    public double getWidth() {
        return width.get();
    }

    public void setWidth(final double width) {
        this.width.set(width);
    }

    public DoubleProperty getWidthProperty() {
        return width;
    }

    public double getHeight() {
        return height.get();
    }

    public void setHeight(final double height) {
        this.height.set(height);
    }

    public DoubleProperty getHeightProperty() {
        return height;
    }

    /**
     * Add properties of this is a JSON object.
     * @param json the JSON object
     */
    public void addProperties(final JsonObject json) {
        json.addProperty(X, getX());
        json.addProperty(Y, getY());
        json.addProperty(WIDTH, getWidth());
        json.addProperty(HEIGHT, getHeight());
    }

    /**
     * Sets properties of this based on a JSON object.
     * @param json the JSON object
     */
    public void setProperties(final JsonObject json) {
        if(json.has(X) && json.has(Y)) {
            setX(json.getAsJsonPrimitive(X).getAsDouble());
            setY(json.getAsJsonPrimitive(Y).getAsDouble());
        } else {
            setX(5);
            setY(5);
        }

        if(json.has(WIDTH) && json.has(HEIGHT)) {
            setWidth(json.getAsJsonPrimitive(WIDTH).getAsDouble());
            setHeight(json.getAsJsonPrimitive(HEIGHT).getAsDouble());
        } else {
            setWidth(0);
            setHeight(0);
        }
    }
}
