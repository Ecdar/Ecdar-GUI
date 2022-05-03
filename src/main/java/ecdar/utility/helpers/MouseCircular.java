package ecdar.utility.helpers;

import ecdar.controllers.EcdarController;
import ecdar.presentations.Grid;
import ecdar.utility.mouse.MouseTracker;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import static ecdar.presentations.Grid.GRID_SIZE;

public class MouseCircular implements Circular {
    private final DoubleProperty x = new SimpleDoubleProperty(0d);
    private final DoubleProperty y = new SimpleDoubleProperty(0d);
    private final DoubleProperty originalX = new SimpleDoubleProperty(0d);
    private final DoubleProperty originalY = new SimpleDoubleProperty(0d);
    private final DoubleProperty originalMouseX = new SimpleDoubleProperty(0d);
    private final DoubleProperty originalMouseY = new SimpleDoubleProperty(0d);
    private final DoubleProperty radius = new SimpleDoubleProperty(10);
    private final SimpleDoubleProperty scale = new SimpleDoubleProperty(1d);
    private final MouseTracker mouseTracker = EcdarController.getActiveCanvasPresentation().mouseTracker;

    public MouseCircular(Circular initLocation){
        //Set the initial x and y coordinates of the circular
        originalX.set(initLocation.getX());
        originalY.set(initLocation.getY());
        originalMouseX.set(mouseTracker.xProperty().get());
        originalMouseY.set(mouseTracker.yProperty().get());

        mouseTracker.registerOnMouseMovedEventHandler(event -> {
            final double dragDistanceX = mouseTracker.xProperty().get() - originalMouseX.get();
            final double dragDistanceY = mouseTracker.yProperty().get() - originalMouseY.get();

            x.set(originalX.get() + dragDistanceX - EcdarController.getActiveCanvasPresentation().getController().modelPane.getTranslateX());
            y.set(originalY.get() + dragDistanceY - EcdarController.getActiveCanvasPresentation().getController().modelPane.getTranslateY());
        });

        mouseTracker.registerOnMouseDraggedEventHandler(event -> {
            final double dragDistanceX = mouseTracker.xProperty().get() - originalMouseX.get();
            final double dragDistanceY = mouseTracker.yProperty().get() - originalMouseY.get();

            x.set(originalX.get() + dragDistanceX - EcdarController.getActiveCanvasPresentation().getController().modelPane.getTranslateX());
            y.set(originalY.get() + dragDistanceY - EcdarController.getActiveCanvasPresentation().getController().modelPane.getTranslateY());
        });
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
