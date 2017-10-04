package SW9.abstractions;

import SW9.utility.colors.Color;
import SW9.utility.serialize.Serializable;
import com.google.gson.JsonObject;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Overall declarations of a model.
 * This could be global declarations or system declarations.
 */
public class Declarations extends VerificationObject {

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
}
