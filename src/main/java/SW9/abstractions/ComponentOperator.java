package SW9.abstractions;

import SW9.presentations.Grid;
import SW9.utility.colors.Color;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;

/**
 * Model of a Component Operator
 */
public abstract class ComponentOperator implements SystemElement {
    public final static int WIDTH = 4 * Grid.GRID_SIZE;
    public final static int HEIGHT = 2 * Grid.GRID_SIZE;

    private final Box box = new Box();
    final StringProperty label = new SimpleStringProperty("");

    //Styling properties
    private final ObjectProperty<Color> color = new SimpleObjectProperty<>();
    private final ObjectProperty<Color.Intensity> colorIntensity = new SimpleObjectProperty<>();

    /**
     * Constructor, does nothing
     */
    ComponentOperator() {

    }

    public ObjectProperty<Color> getColorProperty() {
        return color;
    }

    public ObjectProperty<Color.Intensity> getColorIntensityProperty() {
        return colorIntensity;
    }

    public Box getBox() {
        return box;
    }

    public String getLabel() { return label.get();
    }

    @Override
    public ObservableValue<? extends Number> getEdgeX() {
        return box.getXProperty().add(WIDTH / 2);
    }

    @Override
    public ObservableValue<? extends Number> getEdgeY() {
        return box.getYProperty().add(HEIGHT / 2);
    }
}
