package ecdar.abstractions;

import ecdar.code_analysis.Nearable;
import ecdar.controllers.EcdarController;
import ecdar.presentations.Grid;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.Circular;
import ecdar.utility.serialize.Serializable;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class Edge implements Serializable, Nearable {

    private static final String SOURCE_LOCATION = "sourceLocation";
    private static final String TARGET_LOCATION = "targetLocation";
    private static final String SELECT = "select";
    private static final String GUARD = "guard";
    private static final String UPDATE = "update";
    private static final String SYNC = "sync";
    private static final String NAILS = "nails";
    private static final String STATUS = "status";

    // Defines if this is an input or an output edge
    public ObjectProperty<EdgeStatus> ioStatus;

    // Verification properties
    private final ObjectProperty<Location> sourceLocation = new SimpleObjectProperty<>();
    private final ObjectProperty<Location> targetLocation = new SimpleObjectProperty<>();

    private final StringProperty select = new SimpleStringProperty("");
    private final StringProperty guard = new SimpleStringProperty("");
    private final StringProperty update = new SimpleStringProperty("");
    private final StringProperty sync = new SimpleStringProperty("");

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

    public Edge(final Location sourceLocation, final EdgeStatus status) {
        setSourceLocation(sourceLocation);
        ioStatus = new SimpleObjectProperty<>(status);

        bindReachabilityAnalysis();
    }

    public Edge(final JsonObject jsonObject, final Component component) {
        deserialize(jsonObject, component);
        bindReachabilityAnalysis();
    }

    /**
     * Creates a clone of an edge.
     * Clones objects used for verification.
     * Uses the ids of the source and target to find new source and target objects among the locations of a given component.
     * Be sure that the given component has locations with these ids.
     * @param component component to select a source and a target location within
     */
    Edge cloneForVerification(final Component component) {
        final Edge clone = new Edge(component.findLocation(getSourceLocation().getId()), getStatus());

        // Clone target location
        clone.setTargetLocation(component.findLocation(getTargetLocation().getId()));

        // Clone properties if they are non-empty
        if (!getSelect().isEmpty()) {
            clone.setSelect(getSelect());
            final Nail nail = new Nail(0, 0);
            nail.setPropertyType(PropertyType.SELECTION);
            clone.addNail(nail);
        }
        if (!getGuard().isEmpty()) {
            clone.setGuard(getGuard());
            final Nail nail = new Nail(0, 0);
            nail.setPropertyType(PropertyType.GUARD);
            clone.addNail(nail);
        }
        if (!getUpdate().isEmpty()) {
            clone.setUpdate(getUpdate());
            final Nail nail = new Nail(0, 0);
            nail.setPropertyType(PropertyType.UPDATE);
            clone.addNail(nail);
        }
        if (!getSync().isEmpty()) {
            clone.setSync(getSync());
            final Nail nail = new Nail(0, 0);
            nail.setPropertyType(PropertyType.SYNCHRONIZATION);
            clone.addNail(nail);
        }

        return clone;
    }

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

    private void setSelect(final String select) {
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

    private void setUpdate(final String update) {
        this.update.set(update);
    }

    public StringProperty updateProperty() {
        return update;
    }

    public String getSync() {
        return sync.get();
    }

    public void setSync(final String sync) {
        this.sync.set(sync);
    }

    public StringProperty syncProperty() {
        return sync;
    }

    /**
     * Gets the synchronization string concatenated with the synchronization symbol (? or !).
     * @return the synchronization string concatenated with the synchronization symbol.
     */
    public String getSyncWithSymbol() {
        return sync.get() + (ioStatus.get().equals(EdgeStatus.INPUT) ? "?" : "!");
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
            if (nail.getPropertyType().equals(PropertyType.SYNCHRONIZATION)) return true;
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

    private void updateSourceCircular() {
        if(getSourceLocation() != null) {
            sourceCircular.set(getSourceLocation());
        }
    }

    private void updateTargetCircular() {
        if(getTargetLocation() != null) {
            targetCircular.set(getTargetLocation());
        }
    }

    public String getProperty(final PropertyType propertyType) {
        if (propertyType.equals(PropertyType.SELECTION)) {
            return getSelect();
        } else if (propertyType.equals(PropertyType.GUARD)) {
            return getGuard();
        } else if (propertyType.equals(PropertyType.SYNCHRONIZATION)) {
            return getSync();
        } else if (propertyType.equals(PropertyType.UPDATE)) {
            return getUpdate();
        } else {
            return "";
        }
    }

    public void setProperty(final PropertyType propertyType, final String newProperty) {
        if (propertyType.equals(PropertyType.SELECTION)) {
            selectProperty().unbind();
            setSelect(newProperty);
        } else if (propertyType.equals(PropertyType.GUARD)) {
            guardProperty().unbind();
            setGuard(newProperty);
        } else if (propertyType.equals(PropertyType.SYNCHRONIZATION)) {
            syncProperty().unbind();
            setSync(newProperty);
        } else if (propertyType.equals(PropertyType.UPDATE)) {
            updateProperty().unbind();
            setUpdate(newProperty);
        }
    }

    @Override
    public JsonObject serialize() {
        final JsonObject result = new JsonObject();

        result.addProperty(SOURCE_LOCATION, getSourceLocation().getId());
        result.addProperty(TARGET_LOCATION, getTargetLocation().getId());

        result.addProperty(STATUS, ioStatus.get().toString());
        result.addProperty(SELECT, getSelect());
        result.addProperty(GUARD, getGuard());
        result.addProperty(UPDATE, getUpdate());
        result.addProperty(SYNC, getSync());

        final JsonArray nails = new JsonArray();
        getNails().forEach(nail -> nails.add(nail.serialize()));
        result.add(NAILS, nails);

        return result;
    }

    @Override
    @Deprecated
    public void deserialize(final JsonObject json) {
        throw new UnsupportedOperationException("Use deserialize(JsonObject, Component) instead");
    }

    public void deserialize(final JsonObject json, final Component component) {
        for (final Location loc : component.getLocations()) {
            // Sets a location to be either source or target location if the location matches the json content
            if (loc.getId().equals(json.getAsJsonPrimitive(SOURCE_LOCATION).getAsString())) {
                setSourceLocation(loc);
            }
            if (loc.getId().equals(json.getAsJsonPrimitive(TARGET_LOCATION).getAsString())) {
                setTargetLocation(loc);
            }
        }

        ioStatus = new SimpleObjectProperty<>(EdgeStatus.valueOf(json.getAsJsonPrimitive(STATUS).getAsString()));

        setSelect(json.getAsJsonPrimitive(SELECT).getAsString());
        setGuard(json.getAsJsonPrimitive(GUARD).getAsString());
        setUpdate(json.getAsJsonPrimitive(UPDATE).getAsString());
        setSync(json.getAsJsonPrimitive(SYNC).getAsString());

        json.getAsJsonArray(NAILS).forEach(jsonElement -> {
            final Nail newNail = new Nail((JsonObject) jsonElement);
            nails.add(newNail);
        });
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

    public BooleanProperty getIsLocked(){return isLocked; }

    public void setIsLocked(boolean bool){isLocked.setValue(bool); }

    private void bindReachabilityAnalysis() {
        selectProperty().addListener((observable, oldValue, newValue) -> EcdarController.runReachabilityAnalysis());
        guardProperty().addListener((observable, oldValue, newValue) -> EcdarController.runReachabilityAnalysis());
        syncProperty().addListener((observable, oldValue, newValue) -> EcdarController.runReachabilityAnalysis());
        updateProperty().addListener((observable, oldValue, newValue) -> EcdarController.runReachabilityAnalysis());
    }

    /**
     * Adds a synchronization nail at (0, 0).
     * Adds a specified synchronization property to this edge.
     * @param sync the specified synchronization property
     */
    public void addSyncNail(final String sync) {
        final Nail nail = new Nail(0, 0);
        nail.setPropertyType(PropertyType.SYNCHRONIZATION);
        addNail(nail);
        setSync(sync);
    }

    /**
     * Adds a guard nail at (0, 0).
     * Adds a specified guard property the this edge.
     * @param guard the specified guard property
     */
    public void addGuardNail(final String guard) {
        final Nail nail = new Nail(0, 0);
        nail.setPropertyType(PropertyType.GUARD);
        addNail(nail);
        setGuard(guard);
    }

}
