package SW9.abstractions;

import SW9.Ecdar;
import SW9.controllers.SystemRootController;
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
    private final ObjectProperty<SystemElement> tempNode = new SimpleObjectProperty<>();
    private final ObservableList<SystemNail> nails = FXCollections.observableArrayList();


    /**
     * Constructor
     * @param node A node of this edge. This can either be a child or a parent
     */
    public EcdarSystemEdge(final SystemElement node) {
        tempNode.set(node);
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
        return tempNode.get();
    }

    public void setTempNode(final SystemElement node) {
        tempNode.set(node);
    }

    public ObjectProperty<SystemElement> getTempNodeProperty() {
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
     * @return true iff succeeded
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
        setParent(getTempNode());
        setTempNode(null);
        return true;
    }

    /**
     * If allowed, finishes edge with the temporary node and a given system root.
     * @param controller controller for the given system root
     * @return true iff succeeded
     */
    public boolean tryFinishWithRoot(final SystemRootController controller) {
        // If already has edge, give error
        if (controller.hasEdge()) {
            Ecdar.showToast("This system root already has an edge.");
            return false;
        }

        // Set root as parent node
        setChild(getTempNode());
        setParent(controller.getSystemRoot());
        setTempNode(null);
        return true;
    }

    public boolean tryFinishWithOperator(final ComponentOperator operator) {
        if(getTempNode().getEdgeY() == operator.getEdgeY()){
            Ecdar.showToast("This operator is at same level as the source component");
            return false;
        }
        if(getTempNode().getEdgeY().getValue().doubleValue() < operator.getEdgeY().getValue().doubleValue()){
            if(getTempNode() instanceof ComponentInstance){
                Ecdar.showToast("Operators must be higher than component instances");
                return false;
            } else {
                setChild(operator);
                setParent(getTempNode());
                setTempNode(null);
                return true;
            }
        } else {
            if(getTempNode() instanceof SystemRoot){
                Ecdar.showToast("Operators must be lower than the system root");
                return false;
            }
            setChild(getTempNode());
            setParent(operator);
            setTempNode(null);
            return true;
        }
    }
}
