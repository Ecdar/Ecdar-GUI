package ecdar.abstractions;

import ecdar.Ecdar;
import ecdar.code_analysis.Nearable;
import ecdar.presentations.Grid;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.Circular;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

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
    private final ObjectProperty<Color> color = new SimpleObjectProperty<>(Color.GREY_BLUE);
    private final ObjectProperty<Color.Intensity> colorIntensity = new SimpleObjectProperty<>(Color.Intensity.I700);
    private final ObservableList<Nail> nails = FXCollections.observableArrayList();

    // Circulars
    private final ObjectProperty<Circular> sourceCircular = new SimpleObjectProperty<>();
    private final ObjectProperty<Circular> targetCircular = new SimpleObjectProperty<>();

    // Boolean for if this edge is locked or can be edited
    private final BooleanProperty isLocked = new SimpleBooleanProperty(false);

    private final BooleanProperty isHighlighted = new SimpleBooleanProperty(false);

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
        return guard.get();
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

    public Color getColor() {
        return color.get();
    }

    public void setColor(final Color color) {
        this.color.set(color);
    }

    public ObjectProperty<Color> colorProperty() {
        return color;
    }

    public Color.Intensity getColorIntensity() {
        return colorIntensity.get();
    }

    public void setColorIntensity(final Color.Intensity colorIntensity) {
        this.colorIntensity.set(colorIntensity);
    }

    public ObjectProperty<Color.Intensity> colorIntensityProperty() {
        return colorIntensity;
    }

    public void setIsHighlighted(final boolean highlight){ this.isHighlighted.set(highlight);}

    public boolean getIsHighlighted(){ return this.isHighlighted.get(); }

    public BooleanProperty isHighlightedProperty() { return this.isHighlighted; }

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
        final double x = Grid.snap((getSourceLocation().getX() + getTargetLocation().getX()) / 2.0);
        final double y = Grid.snap((getSourceLocation().getY() + getTargetLocation().getY()) / 2.0);

        final Nail nail = new Nail(x, y);
        nail.setPropertyType(Edge.PropertyType.SYNCHRONIZATION);
        addNail(nail);
    }

    public Circular getSourceCircular() {
        if(sourceCircular != null) {
            return sourceCircular.get();
        }
        return null;
    }

    public Circular getTargetCircular() {
        if(targetCircular != null) {
            return targetCircular.get();
        }
        return null;
    }

    protected void updateSourceCircular() {
        if(getSourceLocation() != null) {
            sourceCircular.set(getSourceLocation());
        }
    }

    protected void updateTargetCircular() {
        if(getTargetLocation() != null) {
            targetCircular.set(getTargetLocation());
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
        for(int counter = 0; ; counter++) {
            if(!Ecdar.getProject().getEdgeIds().contains(String.valueOf(counter))){
                id.set(Edge.EDGE + counter);
                return;
            }
        }
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
}
