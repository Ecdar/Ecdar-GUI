package ecdar.utility.helpers;

import ecdar.controllers.EcdarController;
import ecdar.utility.mouse.MouseTracker;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class MouseCircular implements Circular {
    private final DoubleProperty x = new SimpleDoubleProperty(0d);
    private final DoubleProperty y = new SimpleDoubleProperty(0d);
    private final DoubleProperty radius = new SimpleDoubleProperty(10);
    private final SimpleDoubleProperty scale = new SimpleDoubleProperty(1d);
    private final MouseTracker mouseTracker = EcdarController.getActiveCanvasPresentation().mouseTracker;

    public MouseCircular(){
        //Set the initial x and y coordinates of the circular
        x.bind(mouseTracker.xProperty());
        y.bind(mouseTracker.yProperty());
    }

    @Override
    public DoubleProperty radiusProperty() {
        return radius;
    }

    @Override
    public DoubleProperty scaleProperty() {
        return scale;
    }

    @Override
    public DoubleProperty xProperty() {
        return x;
    }

    @Override
    public DoubleProperty yProperty() {
        return y;
    }

    @Override
    public double getX() {
        return x.get();
    }

    @Override
    public double getY() {
        return y.get();
    }
}
