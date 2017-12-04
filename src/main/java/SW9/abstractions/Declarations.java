package SW9.abstractions;

import SW9.utility.colors.Color;
import SW9.utility.colors.EnabledColor;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Overall declarations of a model.
 * This could be global declarations or system declarations.
 */
public class Declarations extends HighLevelModelObject {

    /**
     * Constructor with a name.
     * @param name name of the declarations
     */
    public Declarations(final String name) {
        setName(name);
        setColor(Color.AMBER);
    }


    public Declarations(final JsonObject object) {
        deserialize(object);
        setColor(Color.AMBER);
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
}
