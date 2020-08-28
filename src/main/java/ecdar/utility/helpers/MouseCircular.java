package ecdar.utility.helpers;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.Location;
import ecdar.presentations.CanvasPresentation;
import ecdar.utility.mouse.MouseTracker;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;

import java.util.List;

public class MouseCircular implements Circular {
    private final DoubleProperty x = new SimpleDoubleProperty(0d);
    private final DoubleProperty y = new SimpleDoubleProperty(0d);
    private final DoubleProperty radius = new SimpleDoubleProperty(10);
    private final SimpleDoubleProperty scale = new SimpleDoubleProperty(1d);
    private final MouseTracker mouseTracker = CanvasPresentation.mouseTracker;

    public MouseCircular(Edge edge, Component component){
        //Set the initial x and y coordinates of the circular
        x.set(mouseTracker.getGridX());
        y.set(mouseTracker.getGridY());

        //Make sure that the circular follows the mouse after the mouse button is released
        mouseTracker.registerOnMouseMovedEventHandler(event -> {
            x.set(mouseTracker.getGridX());
            y.set(mouseTracker.getGridY());
        });

        //Make sure that the circular follows the mouse if the mouse button is not released
        mouseTracker.registerOnMouseDraggedEventHandler(event -> {
            x.set(mouseTracker.getGridX());
            y.set(mouseTracker.getGridY());
        });

        //Set the new source to the clicked circular
        EventHandler<MouseEvent> eventHandler = event -> {
            //Go through all locations and set the source of the edge to the first location within the radius of the mouse
            List<Location> locations = component.getLocations();
            Location closestLoc = locations.get(0);
            for (Location loc : locations) {
                if(Math.abs(loc.getY() - getY()) < radius.get() * 2 && Math.abs(loc.getX() - getX()) < radius.get() * 2 && Math.abs(loc.getY() - getY()) + Math.abs(loc.getX() - getX()) < Math.abs(closestLoc.getY() - getY()) + Math.abs(closestLoc.getX() - getX())){
                    closestLoc = loc;
                }
            }

            edge.setSourceLocation(closestLoc);
        };

        //Set register the eventHandler
        mouseTracker.registerOnMousePressedEventHandler(eventHandler);

        //Unregister the eventHandler when a new source is found
        mouseTracker.registerOnMousePressedEventHandler(event -> {
            if(edge.getSourceCircular() != this){
                mouseTracker.unregisterOnMousePressedEventHandler(eventHandler);
            } else {
                mouseTracker.registerOnMousePressedEventHandler(eventHandler);
            }
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
