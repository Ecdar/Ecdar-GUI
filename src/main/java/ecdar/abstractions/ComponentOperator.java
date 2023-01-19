package ecdar.abstractions;

import com.google.gson.JsonObject;
import ecdar.Ecdar;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;

/**
 * Model of a Component Operator
 */
public abstract class ComponentOperator implements SystemElement {
    public final static int WIDTH = Ecdar.CANVAS_PADDING * 4;
    public final static int HEIGHT = Ecdar.CANVAS_PADDING * 2;

    public static final String TYPE = "type";

    private static final String HIDDEN_ID = "id";
    private static final String X = "x";
    private static final String Y = "y";

    private final Box box = new Box();
    final StringProperty label = new SimpleStringProperty("");

    private int hiddenId;

    /**
     * Constructor.
     * @param system system containing the operator
     */
    ComponentOperator(final EcdarSystem system) {
        hiddenId = system.generateId();
    }

    public Box getBox() {
        return box;
    }

    public String getLabel() { return label.get();
    }


    // Edge coordinates

    @Override
    public ObservableValue<? extends Number> getEdgeX() {
        return box.getXProperty().add(WIDTH / 2);
    }

    @Override
    public ObservableValue<? extends Number> getEdgeY() {
        return box.getYProperty().add(HEIGHT / 2);
    }


    // Hidden id

    @Override
    public int getHiddenId() {
        return hiddenId;
    }

    private void setHiddenId(final int id) {
        hiddenId = id;
    }


    /**
     * Serializes to a JSON object.
     * @return the result
     */
    public JsonObject serialize() {
        final JsonObject result = new JsonObject();

        result.addProperty(HIDDEN_ID, getHiddenId());
        result.addProperty(TYPE, ComponentOperatorFactory.getTypeAsString(this));
        result.addProperty(X, getBox().getX());
        result.addProperty(Y, getBox().getY());

        return result;
    }

    /**
     * Deserializes from a JSON object.
     * @param json the JSON object
     */
    public void deserialize(final JsonObject json) {
        setHiddenId(json.getAsJsonPrimitive(HIDDEN_ID).getAsInt());
        getBox().setX(json.getAsJsonPrimitive(X).getAsDouble());
        getBox().setY(json.getAsJsonPrimitive(Y).getAsDouble());
    }
}
