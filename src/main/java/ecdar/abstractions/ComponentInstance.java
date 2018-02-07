package ecdar.abstractions;

import ecdar.Ecdar;
import ecdar.presentations.Grid;
import ecdar.utility.colors.Color;
import com.google.gson.JsonObject;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;

/**
 * Instance of a component.
 */
public class ComponentInstance implements SystemElement {
    public final static int WIDTH = Grid.GRID_SIZE * 22;
    public final static int HEIGHT = Grid.GRID_SIZE * 12;
    private static final String HIDDEN_ID = "id";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String COMPONENT_NAME = "componentName";

    private final ObjectProperty<Component> component = new SimpleObjectProperty<>();
    private final Box box = new Box();

    // Id of the instance used in the system declarations when verifying
    private final StringProperty instanceIdProperty = new SimpleStringProperty("");

    // Id of the instance used when writing to and reading from JSON
    // This id is unique withing its system
    private int hiddenId;

    /**
     * Constructor.
     * @param system system containing the component instance
     */
    public ComponentInstance(final EcdarSystem system) {
        hiddenId =  system.generateId();
    }

    /**
     * Constructs from a JSON object.
     * @param json the JSON object
     */
    public ComponentInstance(final JsonObject json) {
        deserialize(json);
    }

    public Component getComponent() {
        return component.get();
    }

    public void setComponent(final Component component) {
        this.component.set(component);
    }

    public Box getBox() {
        return box;
    }

    @Override
    public int getHiddenId() {
        return hiddenId;
    }

    @Override
    public ObservableValue<? extends Number> getEdgeX() {
        return box.getXProperty().add(WIDTH / 2);
    }

    @Override
    public ObservableValue<? extends Number> getEdgeY() {
        return box.getYProperty().add(HEIGHT / 2);
    }

    public JsonObject serialize() {
        final JsonObject result = new JsonObject();

        result.addProperty(HIDDEN_ID, getHiddenId());
        result.addProperty(COMPONENT_NAME, getComponent().getName());
        result.addProperty(X, getBox().getX());
        result.addProperty(Y, getBox().getY());

        return result;
    }

    private void deserialize(final JsonObject json) {
        setHiddenId(json.getAsJsonPrimitive(HIDDEN_ID).getAsInt());
        setComponent(Ecdar.getProject().findComponent(json.getAsJsonPrimitive(COMPONENT_NAME).getAsString()));
        getBox().setX(json.getAsJsonPrimitive(X).getAsDouble());
        getBox().setY(json.getAsJsonPrimitive(Y).getAsDouble());
    }

    public StringProperty getInstanceIdProperty() {
        return instanceIdProperty;
    }

    private void setHiddenId(final int hiddenId) {
        this.hiddenId = hiddenId;
    }
}
