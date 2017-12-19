package SW9.abstractions;

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
    private final ObjectProperty<SystemElement> source = new SimpleObjectProperty<>();
    private final ObjectProperty<SystemElement> target = new SimpleObjectProperty<>();
    private final ObservableList<SystemNail> nails = FXCollections.observableArrayList();

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

    public ObservableList<SystemNail> getNails() {
        return nails;
    }
}
