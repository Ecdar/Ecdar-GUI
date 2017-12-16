package SW9.abstractions;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * This is an edge in an Ecdar system.
 * The class is called EcdarSystemEdge, since there is already an SystemEdge in com.uppaal.model.system.
 */
public class EcdarSystemEdge {
    // Verification properties
    private final ObjectProperty<SystemElement> source = new SimpleObjectProperty<>();
    private final ObjectProperty<SystemElement> target = new SimpleObjectProperty<>();

    public EcdarSystemEdge(final SystemElement source) {
        setSource(source);
    }

    public SystemElement getSource() {
        return source.get();
    }

    public ObjectProperty<SystemElement> getSourceProperty() {
        return source;
    }

    public void setSource(final SystemElement source) {
        getSourceProperty().set(source);
    }

    public SystemElement getTarget() {
        return target.get();
    }

    public ObjectProperty<SystemElement> getTargetProperty() {
        return target;
    }

    public void setTarget(final SystemElement target) {
        getTargetProperty().set(target);
    }
}
