package ecdar.utility.helpers;

import ecdar.controllers.EcdarController;
import ecdar.utility.mouse.MouseTracker;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

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

    public MouseCircular(Circular initLocation) {
        //Set the initial x and y coordinates of the circular
        originalX.set(initLocation.getX());
        originalY.set(initLocation.getY());
        x.set(initLocation.getX());
        y.set(initLocation.getY());
        originalMouseX.set(mouseTracker.getGridX());
        originalMouseY.set(mouseTracker.getGridY());

        mouseTracker.registerOnMouseMovedEventHandler(event -> updatePosition());
        mouseTracker.registerOnMouseDraggedEventHandler(event -> updatePosition());

        // If the component is dragged while we are drawing an edge, update the coordinates accordingly
        EcdarController.getActiveCanvasPresentation().getController().modelPane.translateXProperty().addListener((observable, oldValue, newValue) -> originalX.set(
                originalX.get() - (newValue.doubleValue() - oldValue.doubleValue()) / EcdarController.getActiveCanvasZoomFactor().get()));
        EcdarController.getActiveCanvasPresentation().getController().modelPane.translateYProperty().addListener((observable, oldValue, newValue) -> originalY.set(
                originalY.get() - (newValue.doubleValue() - oldValue.doubleValue()) / EcdarController.getActiveCanvasZoomFactor().get()));

        // ToDo NIELS: When the width or height of the scene is changed, the coordinates should be updated
    }

    private void updatePosition() {
        final double dragDistanceX = mouseTracker.getGridX() - originalMouseX.get();
        final double dragDistanceY = mouseTracker.getGridY() - originalMouseY.get();

        x.set(originalX.get() + dragDistanceX / EcdarController.getActiveCanvasZoomFactor().get());
        y.set(originalY.get() + dragDistanceY / EcdarController.getActiveCanvasZoomFactor().get());
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
