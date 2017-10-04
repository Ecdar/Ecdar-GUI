package SW9.abstractions;

import SW9.utility.colors.Color;
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
    Declarations(final String name) {
        setName(name);
        setColor(Color.AMBER);
    }
}
