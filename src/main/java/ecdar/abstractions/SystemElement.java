package ecdar.abstractions;

import javafx.beans.value.ObservableValue;

/**
 * An element in a system that can be connected to an edge.
 * An observable value can be a property or a binding.
 */
public interface SystemElement { // TODO rename to SystemNode
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

    /**
     * Gets an id that is unique within its systems.
     * This is used to write and read to and from JSON.
     * @return the id
     */
    int getHiddenId();
}
