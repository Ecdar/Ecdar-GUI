package SW9.abstractions;

import SW9.utility.colors.Color;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 */
public class Declarations extends VerificationObject {
    private StringProperty declarations;

    public Declarations(final String name) {
        setName(name);
        setColor(Color.AMBER);
        declarations = new SimpleStringProperty("");
    }
}
