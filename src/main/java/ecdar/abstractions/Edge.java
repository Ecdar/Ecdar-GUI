package ecdar.abstractions;

import com.google.gson.JsonPrimitive;
import ecdar.Ecdar;
import ecdar.controllers.EcdarController;
import ecdar.utility.serialize.Serializable;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.beans.property.*;

import java.util.ArrayList;
import java.util.List;

public class Edge extends DisplayableEdge implements Serializable {

    private static final String ID = "id";
    private static final String GROUP = "group";
    private static final String SOURCE_LOCATION = "sourceLocation";
    private static final String TARGET_LOCATION = "targetLocation";
    private static final String SELECT = "select";
    private static final String GUARD = "guard";
    private static final String UPDATE = "update";
    private static final String SYNC = "sync";
    private static final String NAILS = "nails";
    private static final String STATUS = "status";
    private static final String IS_LOCKED = "isLocked";
    public static final String EDGE = "E";
    static final int ID_LETTER_LENGTH = 1;

    private final StringProperty sync = new SimpleStringProperty("");
    private final StringProperty group = new SimpleStringProperty("");

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
     * Generates an id for this, and binds reachability analysis.
     */
    public void initialize() {
        setId();
        System.out.println(getId());
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

    public String getGroup(){
        return group.get();
    }

    public void setGroup(final String string){
        group.set(string);
    }

    /**
     * Creates a clone of an edge.
     * Clones objects used for verification.
     * Uses the ids of the source and target to find new source and target objects among the locations of a given component.
     * Be sure that the given component has locations with these ids.
     * @param component component to select a source and a target location within
     * @return the edge
     */
    Edge cloneForVerification(final Component component) {
        final Edge clone = new Edge(component.findLocation(getSourceLocation().getId()), getStatus());

        // Clone target location
        clone.setTargetLocation(component.findLocation(getTargetLocation().getId()));

        // Clone properties if they are non-empty
        getNails().stream().filter(nail -> nail.getPropertyType().equals(PropertyType.SELECTION)).findFirst().ifPresent(nail -> {
            clone.setSelect(getSelect());
            clone.addNail(nail.cloneForVerification());
        });
        getNails().stream().filter(nail -> nail.getPropertyType().equals(PropertyType.GUARD)).findFirst().ifPresent(nail -> {
            clone.setGuard(getGuard());
            clone.addNail(nail.cloneForVerification());
        });
        getNails().stream().filter(nail -> nail.getPropertyType().equals(PropertyType.SYNCHRONIZATION)).findFirst().ifPresent(nail -> {
            clone.setSync(getSync());
            clone.addNail(nail.cloneForVerification());
        });
        getNails().stream().filter(nail -> nail.getPropertyType().equals(PropertyType.UPDATE)).findFirst().ifPresent(nail -> {
            clone.setUpdate(getUpdate());
            clone.addNail(nail.cloneForVerification());
        });

        // Clone if edge is locked (e.g. the Inconsistent and Universal locations have locked edges)
        clone.setIsLocked(getIsLocked().get());

        return clone;
    }

    public List<String> getProperty(final PropertyType propertyType) {
        List<String> result = new ArrayList<>();
        if (propertyType.equals(PropertyType.SELECTION)) {
            result.add(getSelect());
            return result;
        } else if (propertyType.equals(PropertyType.GUARD)) {
            result.add(getGuard());
            return result;
        } else if (propertyType.equals(PropertyType.SYNCHRONIZATION)) {
            result.add(getSync());
            return result;
        } else if (propertyType.equals(PropertyType.UPDATE)) {
            result.add(getUpdate());
            return result;
        } else {
            return result;
        }
    }

    public void setProperty(final PropertyType propertyType, final List<String> newProperty) {
        if (propertyType.equals(PropertyType.SELECTION)) {
            selectProperty().unbind();
            setSelect(newProperty.get(0));
        } else if (propertyType.equals(PropertyType.GUARD)) {
            guardProperty().unbind();
            setGuard(newProperty.get(0));
        } else if (propertyType.equals(PropertyType.SYNCHRONIZATION)) {
            syncProperty().unbind();
            setSync(newProperty.get(0));
        } else if (propertyType.equals(PropertyType.UPDATE)) {
            updateProperty().unbind();
            setUpdate(newProperty.get(0));
        }
    }

    @Override
    public JsonObject serialize() {
        final JsonObject result = new JsonObject();

        result.addProperty(ID, getId());
        result.addProperty(GROUP, getGroup());

        result.addProperty(SOURCE_LOCATION, getSourceLocation().getId());
        result.addProperty(TARGET_LOCATION, getTargetLocation().getId());

        result.addProperty(STATUS, ioStatus.get().toString());
        result.addProperty(SELECT, getSelect());
        result.addProperty(GUARD, getGuard());
        result.addProperty(UPDATE, getUpdate());
        result.addProperty(SYNC, getSync());

        result.addProperty(IS_LOCKED, getIsLocked().get());

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

        final JsonPrimitive IDJson = json.getAsJsonPrimitive(ID);
        if (IDJson != null) setId(IDJson.getAsString());
        else setId();

        final JsonPrimitive groupJson = json.getAsJsonPrimitive(GROUP);
        if (groupJson != null) {
            setGroup(groupJson.getAsString());
        }
        else setGroup("");

        setSelect(json.getAsJsonPrimitive(SELECT).getAsString());
        setGuard(json.getAsJsonPrimitive(GUARD).getAsString());
        setUpdate(json.getAsJsonPrimitive(UPDATE).getAsString());
        setSync(json.getAsJsonPrimitive(SYNC).getAsString());

        // We need to check for null here in order to be backwards compatible (older models do not specify is locked)
        final JsonPrimitive isLockedJson = json.getAsJsonPrimitive(IS_LOCKED);
        if (isLockedJson != null) setIsLocked(isLockedJson.getAsBoolean());
        else setIsLocked(getSync().equals("*"));

        json.getAsJsonArray(NAILS).forEach(jsonElement -> {
            final Nail newNail = new Nail((JsonObject) jsonElement);
            addNail(newNail);
        });
    }

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

    /**
     * Adds an update nail at (0, 0).
     * Adds a specified update property the this edge.
     * @param update the specified update property
     */
    public void addUpdateNail(final String update) {
        final Nail nail = new Nail(0, 0);
        nail.setPropertyType(PropertyType.UPDATE);
        addNail(nail);
        setUpdate(update);
    }

}
