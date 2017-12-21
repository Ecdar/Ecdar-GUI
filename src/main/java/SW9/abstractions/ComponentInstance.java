package SW9.abstractions;

import SW9.presentations.Grid;
import SW9.utility.colors.Color;
import com.google.gson.JsonObject;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;

/**
 * Instance of a component.
 */
public class ComponentInstance implements SystemElement {
    public final static int WIDTH = Grid.GRID_SIZE * 22;
    public final static int HEIGHT = Grid.GRID_SIZE * 12;
    private static final String ID = "id";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String COMPONENT_NAME = "componentName";

    private final ObjectProperty<Component> component = new SimpleObjectProperty<>();
    private final ObjectProperty<Color> color = new SimpleObjectProperty<>(); // TODO remove color and intensity from here
    private final ObjectProperty<Color.Intensity> colorIntensity = new SimpleObjectProperty<>();
    private final Box box = new Box();
    private StringProperty instanceIdProperty = new SimpleStringProperty(""); ;

    private final int hiddenId;

    public ComponentInstance(final EcdarSystem system) {
        hiddenId =  system.generateId();
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

    @Override
    public int getHiddenId() {
        return hiddenId;
    }

    @Override
    public ObservableValue<? extends Number> getEdgeX() {
        return box.getXProperty().add(WIDTH / 2);
    }

    @Override
    public ObservableValue<? extends Number> getEdgeY() {
        return box.getYProperty().add(HEIGHT / 2);
    }

    public JsonObject serialize() {
        final JsonObject result = new JsonObject();

        result.addProperty(ID, getHiddenId());
        result.addProperty(COMPONENT_NAME, getComponent().getName());
        result.addProperty(X, getBox().getX());
        result.addProperty(Y, getBox().getY());

        return result;
    }

    public StringProperty getInstanceIdProperty() {
        return instanceIdProperty;
    }
}
