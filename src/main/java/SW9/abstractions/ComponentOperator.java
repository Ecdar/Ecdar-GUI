package SW9.abstractions;

import SW9.Ecdar;
import SW9.presentations.Grid;
import com.google.gson.JsonObject;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;

/**
 * Model of a Component Operator
 */
public abstract class ComponentOperator implements SystemElement {
    public final static int WIDTH = 4 * Grid.GRID_SIZE;
    public final static int HEIGHT = 2 * Grid.GRID_SIZE;

    public static final String TYPE = "type";

    private static final String HIDDEN_ID = "id";
    private static final String X = "x";
    private static final String Y = "y";

    private final Box box = new Box();
    final StringProperty label = new SimpleStringProperty("");

    private int hiddenId;

    /**
     * Constructor, does nothing
     */
    ComponentOperator(final EcdarSystem system) {
        hiddenId = system.generateId();
    }

    public Box getBox() {
        return box;
    }

    public String getLabel() { return label.get();
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
        result.addProperty(TYPE, ComponentOperatorJsonFactory.getJsonType(this));
        result.addProperty(X, getBox().getX());
        result.addProperty(Y, getBox().getY());

        return result;
    }

    @Override
    public int getHiddenId() {
        return hiddenId;
    }

    private void setHiddenId(final int id) {
        hiddenId = id;
    }

    public void deserialize(final JsonObject json) {
        setHiddenId(json.getAsJsonPrimitive(HIDDEN_ID).getAsInt());
        getBox().setX(json.getAsJsonPrimitive(X).getAsDouble());
        getBox().setY(json.getAsJsonPrimitive(Y).getAsDouble());
    }
}
