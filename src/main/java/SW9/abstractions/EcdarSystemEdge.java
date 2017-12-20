package SW9.abstractions;

import SW9.Ecdar;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * This is an edge in an Ecdar system.
 * The class is called EcdarSystemEdge, since there is already an SystemEdge in com.uppaal.model.system.
 */
public class EcdarSystemEdge {
    // Verification properties
    private final ObjectProperty<SystemElement> child = new SimpleObjectProperty<>();
    private final ObjectProperty<SystemElement> parent = new SimpleObjectProperty<>();
    private SystemElement tempNode;
    private final ObservableList<SystemNail> nails = FXCollections.observableArrayList();


    /**
     * Constructor
     * @param node A node of this edge. This can either be a child or a parent
     */
    public EcdarSystemEdge(final SystemElement node) {
        tempNode = node;
    }

    public ObservableList<SystemNail> getNails() {
        return nails;
    }

    public SystemElement getChild() {
        return child.get();
    }

    public ObjectProperty<SystemElement> getChildProperty() {
        return child;
    }

    public SystemElement getParent() {
        return parent.get();
    }

    public ObjectProperty<SystemElement> getParentProperty() {
        return parent;
    }

    public SystemElement getTempNode() {
        return tempNode;
    }

    public void setChild(final SystemElement child) {
        this.child.set(child);
    }

    public void setParent(final SystemElement parent) {
        this.parent.set(parent);
    }

    public boolean isFinished() {
        return getChild() != null && getParent() != null;
    }

    /**
     * Gets if a node is included in this edge.
     * It is included, iff it is the temp node, the child, or the parent.
     * @param node the node to check
     * @return true iff the node is included in this edge
     */
    public boolean isInEdge(final SystemElement node) {
        return node == getTempNode() || node == getChild() || node == getParent();
    }

    /**
     * If allowed, finishes edge with the temporary node and a given instance.
     * @param instance the instance to finish with
     * @return true iff allowed to finish
     */
    public boolean tryFinishWithComponentInstance(final ComponentInstance instance) {
        // other node must be operator or root
        if (!(getTempNode() instanceof ComponentOperator || getTempNode() instanceof SystemRoot)) {
            Ecdar.showToast("A component instance can only be connected to operators or system roots.");
            return false;
        }

        // Other node must be higher (lower Y) than the instance
        if (!(getTempNode().getEdgeYInt() < instance.getEdgeYInt())) {
            Ecdar.showToast("A component instance must be lower than the connected node");
            return false;
        }

        // Set instance as child node
        setChild(instance);
        setParent(tempNode);
        tempNode = null;
        return true;
    }

    public boolean tryFinishWithRoot(final SystemRoot systemRoot) {
        // TODO implement
        return false;
    }

    public boolean tryFinishWithOperator(final ComponentOperator operator) {
        // TODO implement
        /*
        // If already has edge, give error
        if (hasParent.get()) {
            Ecdar.showToast("This component operator already has a parent.");
            return;
        }*/
        return false;
    }
}
