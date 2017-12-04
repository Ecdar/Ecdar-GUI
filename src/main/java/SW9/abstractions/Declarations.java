package SW9.abstractions;

import SW9.utility.colors.Color;
import SW9.utility.colors.EnabledColor;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Overall declarations of a model.
 * This could be global declarations or system declarations.
 */
public class Declarations extends HighLevelModelObject {
    private final StringProperty declarationsText;

    /**
     * Constructor with a name.
     * @param name name of the declarations
     */
    public Declarations(final String name) {
        setName(name);
        setColor(Color.AMBER);
        declarationsText = new SimpleStringProperty("");
    }


    public Declarations(final JsonObject object) {
        deserialize(object);
        setColor(Color.AMBER);
        declarationsText = new SimpleStringProperty("");
    }

    public String getDeclarationsText() {
        return declarationsText.get();
    }

    public void setDeclarationsText(final String declarationsText) {
        this.declarationsText.set(declarationsText);
    }

    @Override
    public JsonObject serialize() {
        final JsonObject result = super.serialize();

        result.addProperty(DECLARATIONS, getDeclarationsText());

        return result;
    }

    @Override
    public void deserialize(final JsonObject json) {
        super.deserialize(json);

        setDeclarationsText(json.getAsJsonPrimitive(DECLARATIONS).getAsString());
    }

    public void clearDeclarationsText() {
        setDeclarationsText("");
    }
}
