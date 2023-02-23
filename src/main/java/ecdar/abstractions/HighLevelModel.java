package ecdar.abstractions;

import ecdar.presentations.DropDownMenu;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.serialize.Serializable;
import com.google.gson.JsonObject;
import javafx.beans.property.*;

/**
 * An object used for verifications.
 * This could be a component, a global declarations object, or a system.
 */
public abstract class HighLevelModel implements Serializable, DropDownMenu.HasColor {
    private static final String NAME = "name";

    static final String DECLARATIONS = "declarations";
    public static final String DESCRIPTION = "description";
    static final String COLOR = "color";

    private final StringProperty name;
    private final ObjectProperty<EnabledColor> color;
    private boolean temporary = false;

    public HighLevelModel() {
        name = new SimpleStringProperty("");
        color = new SimpleObjectProperty<>(EnabledColor.getDefault());
    }

    public String getName() {
        return name.get();
    }

    public void setName(final String name) {
        this.name.unbind();
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public EnabledColor getColor() {
        return color.get();
    }

    /**
     * Sets the color of the model
     */
    void setColor(final EnabledColor color) {
        this.color.set(color);
    }

    public ObjectProperty<EnabledColor> colorProperty() {
        return color;
    }

    public boolean isTemporary() {
        return temporary;
    }

    public void setTemporary(final boolean newValue) {
        this.temporary = newValue;
    }

    @Override
    public JsonObject serialize() {
        final JsonObject result = new JsonObject();

        result.addProperty(NAME, getName());

        return result;
    }

    @Override
    public void deserialize(final JsonObject json) {
        setName(json.getAsJsonPrimitive(NAME).getAsString());
    }
}
