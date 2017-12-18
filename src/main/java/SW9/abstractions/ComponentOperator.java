package SW9.abstractions;

import SW9.utility.colors.Color;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model of a Component Operator
 */
public abstract class ComponentOperator {
    private final Box box = new Box();
    final StringProperty label = new SimpleStringProperty("");

    //Styling properties
    private final ObjectProperty<Color> color = new SimpleObjectProperty<>(Color.GREY_BLUE);
    private final ObjectProperty<Color.Intensity> colorIntensity = new SimpleObjectProperty<>(Color.Intensity.I700);

    /**
     * Constructor, does nothing
     */
    ComponentOperator() {

    }

    public ObjectProperty<Color> getColor() {
        return color;
    }

    public ObjectProperty<Color.Intensity> getColorIntensity() {
        return colorIntensity;
    }

    public Box getBox() {
        return box;
    }

    public String getLabel() { return label.get();
    }
}
