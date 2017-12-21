package SW9.abstractions;

import javafx.beans.value.ObservableValue;

/**
 * An element in a system that can be connected to an edge.
 * An observable value can be a property or a binding.
 */
public interface SystemElement {
    /**
     * Observable value for x coordinate of where you want the edge to start from.
     * @return observable value
     */
    ObservableValue<? extends Number> getEdgeX();

    /**
     * X coordinate of where you want the edge to start from.
     * @return the x coordinate
     */
    default int getEdgeXInt() {
        return getEdgeX().getValue().intValue();
    }

    /**
     * Observable value for y coordinate of where you want the edge to start from.
     * @return the observable value
     */
    ObservableValue<? extends Number> getEdgeY();

    /**
     * Y coordinate of where you want the edge the start from.
     * @return the y coordinate
     */
    default int getEdgeYInt() {
        return getEdgeY().getValue().intValue();
    }
}
