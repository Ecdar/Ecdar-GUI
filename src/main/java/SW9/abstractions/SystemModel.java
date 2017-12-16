package SW9.abstractions;

import SW9.Ecdar;
import SW9.presentations.Grid;
import SW9.utility.UndoRedoStack;
import SW9.utility.colors.Color;
import SW9.utility.colors.EnabledColor;
import SW9.utility.helpers.Boxed;
import com.google.gson.JsonObject;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * A model of a system.
 * The class is called SystemModel, since Java already has a System class.
 */
public class SystemModel extends EcdarModel implements Boxed { // TODO name EcdarSystem
    private static final String SYSTEM = "System";
    private static final String SYSTEM_ROOT_X = "systemRootX";

    // Verification properties
    private final StringProperty description = new SimpleStringProperty("");
    private final ObservableList<ComponentInstance> componentInstances = FXCollections.observableArrayList();
    private final ObservableList<ComponentOperator> componentOperators = FXCollections.observableArrayList();
    private final ObservableList<EcdarSystemEdge> edges = FXCollections.observableArrayList();
    private final SystemRoot systemRoot = new SystemRoot();

    // Styling properties
    private final Box box = new Box();

    public SystemModel() {
        setSystemName();
        setRandomColor();

        // Create system root in the middle, horizontally
        systemRoot.setX(Grid.snap(getBox().getWidth() / 2));
    }

    SystemModel(final JsonObject json) {
        deserialize(json);
    }

    public void addEdge(final EcdarSystemEdge edge) {
        edges.add(edge);
    }

    public void removeEdge(final EcdarSystemEdge edge) {
        edges.remove(edge);
    }

    @Override
    public Box getBox() {
        return box;
    }

    public String getDescription() {
        return description.get();
    }

    public StringProperty getDescriptionProperty() {
        return description;
    }

    public void setDescription(final String description) {
        this.description.setValue(description);
    }

    public ObservableList<ComponentInstance> getComponentInstances() {
        return componentInstances;
    }

    public ObservableList<ComponentOperator> getComponentOperators() {
        return componentOperators;
    }

    public SystemRoot getSystemRoot() {
        return systemRoot;
    }

    public void addComponentInstance(final ComponentInstance instance) {
        componentInstances.add(instance);
    }

    public void removeComponentInstance(final ComponentInstance instance) {
        componentInstances.remove(instance);
    }

    public void addComponentOperator(final ComponentOperator operator) {
        componentOperators.add(operator);
    }

    public void removeComponentOperator(final ComponentOperator operator) {
        componentOperators.remove(operator);
    }

    /**
     * Dyes the system.
     * @param color the color to dye with
     * @param intensity the intensity of the color
     */
    public void dye(final Color color, final Color.Intensity intensity) {
        final Color previousColor = colorProperty().get();
        final Color.Intensity previousColorIntensity = colorIntensityProperty().get();

        UndoRedoStack.pushAndPerform(() -> { // Perform
            // Color the component
            setColorIntensity(intensity);
            setColor(color);
        }, () -> { // Undo
            // Color the component
            setColorIntensity(previousColorIntensity);
            setColor(previousColor);
        }, String.format("Changed the color of %s to %s", this, color.name()), "color-lens");
    }

    @Override
    public JsonObject serialize() {
        final JsonObject result = super.serialize();

        result.addProperty(DESCRIPTION, getDescription());

        box.addProperties(result);

        result.addProperty(COLOR, EnabledColor.getIdentifier(getColor()));

        result.addProperty(SYSTEM_ROOT_X, systemRoot.getX());

        return result;
    }

    @Override
    public void deserialize(final JsonObject json) {
        super.deserialize(json);

        setDescription(json.getAsJsonPrimitive(DESCRIPTION).getAsString());

        box.setProperties(json);

        final EnabledColor enabledColor = EnabledColor.fromIdentifier(json.getAsJsonPrimitive(COLOR).getAsString());
        if (enabledColor != null) {
            setColorIntensity(enabledColor.intensity);
            setColor(enabledColor.color);
        }

        systemRoot.setX(json.getAsJsonPrimitive(SYSTEM_ROOT_X).getAsDouble());
    }

    /**
     * Generate and sets a unique id for this system
     */
    public void setSystemName() {
        for(int counter = 1; ; counter++) {
            if(!Ecdar.getProject().getSystemNames().contains(SYSTEM + counter)){
                setName((SYSTEM + counter));
                return;
            }
        }
    }
}
