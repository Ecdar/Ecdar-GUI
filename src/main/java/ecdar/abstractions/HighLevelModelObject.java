package ecdar.abstractions;

import ecdar.Ecdar;
import ecdar.presentations.DropDownMenu;
import ecdar.utility.colors.Color;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.serialize.Serializable;
import com.google.gson.JsonObject;
import javafx.beans.property.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * An object used for verifications.
 * This could be a component, a global declarations object, or a system.
 */
public abstract class HighLevelModelObject implements Serializable, DropDownMenu.HasColor {
    private static final String NAME = "name";

    static final String DECLARATIONS = "declarations";
    public static final String DESCRIPTION = "description";
    static final String COLOR = "color";

    private final StringProperty name;
    private final ObjectProperty<Color> color;
    private final ObjectProperty<Color.Intensity> colorIntensity;
    private boolean temporary = false;

    public HighLevelModelObject() {
        name = new SimpleStringProperty("");
        color = new SimpleObjectProperty<>(Color.GREY_BLUE);
        colorIntensity = new SimpleObjectProperty<>(Color.Intensity.I700);
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

    public boolean isTemporary() {
        return temporary;
    }

    public void setTemporary(final boolean newValue) {
        this.temporary = newValue;
    }

    /**
     * Sets a random color.
     * If some colors are not currently in use, choose among those.
     * Otherwise choose a between all available colors.
     */
    void setRandomColor() {
        // Color the new component in such a way that we avoid clashing with other components if possible
        final List<EnabledColor> availableColors = new ArrayList<>(EnabledColor.enabledColors);
        Ecdar.getProject().getComponents().forEach(component -> {
            availableColors.removeIf(enabledColor -> enabledColor.color.equals(component.getColor()));
        });
        if (availableColors.size() == 0) {
            availableColors.addAll(EnabledColor.enabledColors);
        }
        final int randomIndex = (new Random()).nextInt(availableColors.size());
        final EnabledColor selectedColor = availableColors.get(randomIndex);
        setColorIntensity(selectedColor.intensity);
        setColor(selectedColor.color);
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
