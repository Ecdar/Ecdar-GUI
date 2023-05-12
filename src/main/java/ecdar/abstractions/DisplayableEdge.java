package ecdar.abstractions;

import ecdar.code_analysis.Nearable;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.helpers.Circular;
import ecdar.utility.helpers.MouseCircular;
import ecdar.utility.helpers.StringHelper;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.UUID;

public abstract class DisplayableEdge implements Nearable {
    private final StringProperty id = new SimpleStringProperty("");
    // Defines if this is an input or an output edge
    public ObjectProperty<EdgeStatus> ioStatus;

    // Verification properties
    protected final ObjectProperty<Location> sourceLocation = new SimpleObjectProperty<>();
    protected final ObjectProperty<Location> targetLocation = new SimpleObjectProperty<>();

    private final StringProperty select = new SimpleStringProperty("");
    private final StringProperty guard = new SimpleStringProperty("");
    private final StringProperty update = new SimpleStringProperty("");

    // Styling properties
    private final ObjectProperty<EnabledColor> color = new SimpleObjectProperty<>(EnabledColor.getDefault());
    private final ObservableList<Nail> nails = FXCollections.observableArrayList();

    // Circulars
    private final ObjectProperty<Circular> sourceCircular = new SimpleObjectProperty<>();
    private final ObjectProperty<Circular> targetCircular = new SimpleObjectProperty<>();

    // Boolean for if this edge is locked or can be edited
    private final BooleanProperty isLocked = new SimpleBooleanProperty(false);

    private final BooleanProperty isHighlighted = new SimpleBooleanProperty(false);

    protected final BooleanProperty failing = new SimpleBooleanProperty(false);
    private final BooleanProperty isHighlightedForReachability = new SimpleBooleanProperty(false);

    public Location getSourceLocation() {
        return sourceLocation.get();
    }

    public void setSourceLocation(final Location sourceLocation) {
        this.sourceLocation.set(sourceLocation);
        updateSourceCircular();
    }

    public ObjectProperty<Circular> sourceCircularProperty() {
        return sourceCircular;
    }

    public ObjectProperty<Location> sourceLocationProperty() {
        return sourceLocation;
    }

    public Location getTargetLocation() {
        return targetLocation.get();
    }

    public void setTargetLocation(final Location targetLocation) {
        this.targetLocation.set(targetLocation);
        updateTargetCircular();
    }

    public ObjectProperty<Location> targetLocationProperty() {
        return targetLocation;
    }

    public String getSelect() {
        return select.get();
    }

    public void setSelect(final String select) {
        this.select.set(select);
    }

    public StringProperty selectProperty() {
        return select;
    }

    public String getGuard() {
        return StringHelper.ConvertUnicodeToSymbols(guard.get());
    }

    public void setGuard(final String guard) {
        this.guard.set(guard);
    }

    public StringProperty guardProperty() {
        return guard;
    }

    public String getUpdate() {
        return update.get();
    }

    public void setUpdate(final String update) {
        this.update.set(update);
    }

    public StringProperty updateProperty() {
        return update;
    }

    public EnabledColor getColor() {
        return color.get();
    }

    public void setColor(final EnabledColor color) {
        this.color.set(color);
    }

    public ObjectProperty<EnabledColor> colorProperty() {
        return color;
    }

    public void setIsHighlighted(final boolean highlight){ this.isHighlighted.set(highlight);}

    public boolean isHighlighted(){ return this.isHighlighted.get(); }
    public boolean getIsHighlightedForReachability(){ return this.isHighlightedForReachability.get(); }

    public BooleanProperty isHighlightedProperty() { return this.isHighlighted; }
    public BooleanProperty isHighlightedForReachabilityProperty() { return this.isHighlightedForReachability; }


    public ObservableList<Nail> getNails() {
        return nails;
    }

    public boolean addNail(final Nail nail) {
        return nails.add(nail);
    }

    public void insertNailAt(final Nail nail, final int index) {
        nails.add(index, nail);
    }

    public boolean removeNail(final Nail nail) {
        return nails.remove(nail);
    }

    public ObjectProperty<Circular> targetCircularProperty() {
        return targetCircular;
    }

    /**
     * Gets whether this has a synchronization nail.
     * @return true iff this has a synchronization nail
     */
    public boolean hasSyncNail() {
        for (final Nail nail : getNails()) {
            if (nail.getPropertyType().equals(Edge.PropertyType.SYNCHRONIZATION)) return true;
        }

        return false;
    }

    /**
     * Makes a synchronization nail between the source and target locations.
     */
    public void makeSyncNailBetweenLocations() {
        final double x = (getSourceLocation().getX() + getTargetLocation().getX()) / 2.0;
        final double y = (getSourceLocation().getY() + getTargetLocation().getY()) / 2.;

        final Nail nail = new Nail(x, y);
        nail.setPropertyType(Edge.PropertyType.SYNCHRONIZATION);
        addNail(nail);
    }

    public Circular getSourceCircular() {
        return sourceCircular.get();
    }

    public Circular getTargetCircular() {
        return targetCircular.get();
    }

    protected void updateSourceCircular() {
        if(getSourceLocation() != null) {
            sourceCircular.set(getSourceLocation());
        } else {
            sourceCircular.set(new MouseCircular(sourceCircular.get()));
        }
    }

    protected void updateTargetCircular() {
        if(getTargetLocation() != null) {
            targetCircular.set(getTargetLocation());
        } else {
            targetCircular.set(new MouseCircular(targetCircular.get()));
        }
    }

    @Override
    public String generateNearString() {
        String result = "Edge";

        if (getSourceLocation() != null) {
            result += " from " + getSourceLocation().generateNearString();
        } else {
            result += " from " + getSourceCircular();
        }

        if (getTargetLocation() != null) {
            result += " to " + getTargetLocation().generateNearString();
        } else {
            result += " to " + getTargetCircular();
        }

        return result;
    }

    /**
     * Gets if this is an input or output edge.
     * @return the status
     */
    public EdgeStatus getStatus() {
        return ioStatus.get();
    }

    public void setStatus(final EdgeStatus status) {
        ioStatus.set(status);
    }

    /**
     * Switches if the status of this is input or output
     */
    public void switchStatus() {
        if (ioStatus.get() == EdgeStatus.INPUT) {
            ioStatus.set(EdgeStatus.OUTPUT);
        } else {
            ioStatus.set(EdgeStatus.INPUT);
        }
    }

    public void setIsHighlightedForReachability(final boolean highlightedForReachability){ this.isHighlightedForReachability.set(highlightedForReachability);}

    public enum PropertyType {
        NONE(-1),
        SELECTION(0),
        GUARD(1),
        SYNCHRONIZATION(2),
        UPDATE(3);

        private final int i;

        PropertyType(final int i) {
            this.i = i;
        }

        public int getI() {
            return i;
        }
    }

    public boolean isSelfLoop() {
        return (getSourceLocation() != null && getSourceLocation().equals(getTargetLocation()));
    }

    public BooleanProperty getIsLockedProperty(){return isLocked; }

    public void setIsLocked(boolean bool){isLocked.setValue(bool); }

    public String getId() {
        return id.get();
    }

    /**
     * Generate and sets a unique id for this location
     */
    protected void setId() {
        id.set(UUID.randomUUID().toString());
    }

    /**
     * Sets a specific id for this location
     * @param string id to set
     */
    public void setId(final String string){
        id.set(string);
    }

    public StringProperty idProperty() {
        return id;
    }

    public abstract List<String> getProperty(final PropertyType propertyType);

    public abstract void setProperty(final PropertyType propertyType, final List<String> newProperty);

    /**
     * Sets the 'failing' property
     * @param bool true if the edge is failing.
     */
    public abstract void setFailing(final boolean bool);

    /**
     * Getter for the 'failing' boolean
     * @return Whether edge is failing in last query response.
     */
    public abstract boolean getFailing();

    /**
     * The observable boolean property for 'failing' of this.
     * @return The observable boolean property for 'failing' of this.
     */
    public abstract BooleanProperty failingProperty();
}
