package ecdar.backend;

import com.google.gson.JsonObject;
import ecdar.utility.serialize.Serializable;
import javafx.beans.property.SimpleBooleanProperty;

public class BackendInstance implements Serializable {
    private static final String NAME = "name";
    private static final String IS_LOCAL = "isLocal";
    private static final String IS_DEFAULT = "isDefault";
    private static final String LOCATION = "location";
    private static final String PORT_RANGE_START = "portRangeStart";
    private static final String PORT_RANGE_END = "portRangeEnd";
    private static final String LOCKED = "locked";
    private static final String IS_THREAD_SAFE = "isThreadSafe";

    private String name;
    private boolean isLocal;
    private boolean isDefault;
    private boolean isThreadSafe;
    private String backendLocation;
    private int portStart;
    private int portEnd;
    private SimpleBooleanProperty locked = new SimpleBooleanProperty(false);

    public BackendInstance() {};

    public BackendInstance(final JsonObject jsonObject) {
        deserialize(jsonObject);
    };

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public void setLocal(boolean local) {
        isLocal = local;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public boolean isThreadSafe() {
        return isThreadSafe;
    }

    public void setIsThreadSafe(boolean threadSafe) {
        isThreadSafe = threadSafe;
    }

    public String getBackendLocation() {
        return backendLocation;
    }

    public void setBackendLocation(String backendLocation) {
        this.backendLocation = backendLocation;
    }

    public int getPortStart() {
        return portStart;
    }

    public void setPortStart(int portStart) {
        this.portStart = portStart;
    }

    public int getPortEnd() {
        return portEnd;
    }

    public void setPortEnd(int portEnd) {
        this.portEnd = portEnd;
    }

    public int getNumberOfInstances() {
        return this.portEnd - this.portStart;
    }

    public void lockInstance() {
        locked.set(true);
    }

    public SimpleBooleanProperty getLockedProperty() {
        return locked;
    }

    @Override
    public JsonObject serialize() {
        final JsonObject result = new JsonObject();
        result.addProperty(NAME, getName());
        result.addProperty(IS_LOCAL, isLocal());
        result.addProperty(IS_DEFAULT, isDefault());
        result.addProperty(IS_THREAD_SAFE, isThreadSafe());
        result.addProperty(LOCATION, getBackendLocation());
        result.addProperty(PORT_RANGE_START, getPortStart());
        result.addProperty(PORT_RANGE_END, getPortEnd());
        result.addProperty(LOCKED, getLockedProperty().get());

        return result;
    }

    @Override
    public void deserialize(final JsonObject json) {
        setName(json.getAsJsonPrimitive(NAME).getAsString());
        setLocal(json.getAsJsonPrimitive(IS_LOCAL).getAsBoolean());
        setDefault(json.getAsJsonPrimitive(IS_DEFAULT).getAsBoolean());

        try { // ToDo NIELS: Decide to either do this or simply reload defaults
            setIsThreadSafe(json.getAsJsonPrimitive(IS_THREAD_SAFE).getAsBoolean());
        } catch (NullPointerException e) {
            setIsThreadSafe(false);
        }

        setBackendLocation(json.getAsJsonPrimitive(LOCATION).getAsString());
        setPortStart(json.getAsJsonPrimitive(PORT_RANGE_START).getAsInt());
        setPortEnd(json.getAsJsonPrimitive(PORT_RANGE_END).getAsInt());
        if (json.getAsJsonPrimitive(LOCKED).getAsBoolean()) lockInstance();
    }

    @Override
    public String toString() {
        return name;
    }
}
