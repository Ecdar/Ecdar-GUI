package SW9.abstractions;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 *
 */
public class Declarations extends VerificationObject {
    private StringProperty declarations;

    public Declarations() {
        declarations = new SimpleStringProperty("");
    }
}
