package SW9.abstractions;

import SW9.utility.colors.Color;
import javafx.beans.property.*;

/**
 * Instance of a component.
 */
public class ComponentInstance implements SystemElement {
    private final ObjectProperty<Component> component = new SimpleObjectProperty<>();
    private final ObjectProperty<Color> color = new SimpleObjectProperty<>();
    private final ObjectProperty<Color.Intensity> colorIntensity = new SimpleObjectProperty<>();
    private final Box box = new Box();
    private final StringProperty id = new SimpleStringProperty("");

    public ComponentInstance() {

    }

    public Component getComponent() {
        return component.get();
    }

    public void setComponent(final Component component) {
        this.component.set(component);
    }

    public Color getColor() {
        return color.get();
    }

    public void setColor(final Color color) {
        this.color.set(color);
    }

    public ObjectProperty<Color> getColorProperty() {
        return color;
    }

    public Color.Intensity getColorIntensity() {
        return colorIntensity.get();
    }

    public void setColorIntensity(final Color.Intensity intensity) {
        this.colorIntensity.set(intensity);
    }

    public ObjectProperty<Color.Intensity> getColorIntensityProperty() {
        return colorIntensity;
    }

    public Box getBox() {
        return box;
    }

    public String getId() {
        return id.get();
    }

    public StringProperty getIdProperty() {
        return id;
    }

    @Override
    public DoubleProperty getXProperty() {
        return box.getXProperty();
    }

    @Override
    public DoubleProperty getYProperty() {
        return box.getYProperty();
    }
}
