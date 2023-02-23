package ecdar.abstractions;

import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.helpers.Boxed;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A model of a system.
 * The class is called EcdarSystem, since Java already has a System class.
 */
public class EcdarSystem extends HighLevelModel implements Boxed {
    private static final String SYSTEM_ROOT_X = "systemRootX";
    private static final String INSTANCES = "componentInstances";
    private static final String OPERATORS = "operators";
    private static final String EDGES = "edges";

    // Verification properties
    private final StringProperty description = new SimpleStringProperty("");
    private final ObservableList<ComponentInstance> componentInstances = FXCollections.observableArrayList();
    private final ObservableList<ComponentOperator> componentOperators = FXCollections.observableArrayList();
    private final ObservableList<SystemEdge> edges = FXCollections.observableArrayList();
    private final SystemRoot systemRoot = new SystemRoot();

    // Styling properties
    private final Box box = new Box();

    public EcdarSystem(final EnabledColor color, final String name) {
        this.nameProperty().set(name);
        setColor(color);

        getBox().setWidth(600d);

        // Create system root in the middle, horizontally
        systemRoot.setX((getBox().getWidth() - SystemRoot.WIDTH) / 2);
    }

    EcdarSystem(final JsonObject json) {
        deserialize(json);
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


    /* Edges */

    public ObservableList<SystemEdge> getEdges() {
        return edges;
    }

    public void addEdge(final SystemEdge edge) {
        edges.add(edge);
    }

    public void removeEdge(final SystemEdge edge) {
        edges.remove(edge);
    }

    /**
     * Dyes the system.
     * @param color the color to dye with
     */
    public void dye(final EnabledColor color) {
        final EnabledColor previousColor = colorProperty().get();

        UndoRedoStack.pushAndPerform(() -> { // Perform
            // Color the component
            setColor(color);
        }, () -> { // Undo
            // Color the component
            setColor(previousColor);
        }, String.format("Changed the color of %s to %s", this, color.color.name()), "color-lens");
    }

    @Override
    public JsonObject serialize() {
        final JsonObject result = super.serialize();

        result.addProperty(DESCRIPTION, getDescription());

        box.addProperties(result);

        result.addProperty(COLOR, EnabledColor.getIdentifier(getColor().color));

        result.addProperty(SYSTEM_ROOT_X, systemRoot.getX());

        final JsonArray instances = new JsonArray();
        getComponentInstances().forEach(instance -> instances.add(instance.serialize()));
        result.add(INSTANCES, instances);

        final JsonArray operators = new JsonArray();
        getComponentOperators().forEach(operator -> operators.add(operator.serialize()));
        result.add(OPERATORS, operators);

        final JsonArray edges = new JsonArray();
        getEdges().forEach(edge -> edges.add(edge.serialize()));
        result.add(EDGES, edges);

        return result;
    }

    @Override
    public void deserialize(final JsonObject json) {
        super.deserialize(json);

        setDescription(json.getAsJsonPrimitive(DESCRIPTION).getAsString());

        box.setProperties(json);

        final EnabledColor enabledColor = EnabledColor.fromIdentifier(json.getAsJsonPrimitive(COLOR).getAsString());
        if (enabledColor != null) {
            setColor(enabledColor);
        }

        systemRoot.setX(json.getAsJsonPrimitive(SYSTEM_ROOT_X).getAsDouble());

        json.getAsJsonArray(INSTANCES).forEach(jsonInstance ->
                getComponentInstances().add(new ComponentInstance((JsonObject) jsonInstance)));

        json.getAsJsonArray(OPERATORS).forEach(jsonOperator -> {
            final String type = ((JsonObject) jsonOperator).getAsJsonPrimitive(ComponentOperator.TYPE).getAsString();
            final ComponentOperator operator = ComponentOperatorFactory.create(type, this);
            operator.deserialize((JsonObject) jsonOperator);
            getComponentOperators().add(operator);
        });

        json.getAsJsonArray(EDGES).forEach(jsonEdge ->
                getEdges().add(new SystemEdge((JsonObject) jsonEdge, this)));
    }

    /**
     * Gets the first found unfinished edge.
     * An edge is unfinished, if the has no target.
     * @return The unfinished edge, or null if none was found.
     */
    public SystemEdge getUnfinishedEdge() {
        for (final SystemEdge edge : edges) {
            if (!edge.isFinished()) return edge;
        }
        return null;
    }

    /**
     * Generator a hidden id for a system node.
     * The id is unique among the system nodes of this system.
     * @return the id
     */
    public int generateId() {
        final Set<Integer> ids = new HashSet<>();

        ids.add(getSystemRoot().getHiddenId());

        for (final ComponentInstance instance : getComponentInstances()) {
            ids.add(instance.getHiddenId());
        }

        for (final ComponentOperator operator : getComponentOperators()) {
            ids.add(operator.getHiddenId());
        }

        for (int counter = 0; ; counter++) {
            if(!ids.contains(counter)){
                return counter;
            }
        }
    }

    /**
     * Find a system node by its hidden id.
     * @param hiddenId the hidden id
     * @return the system node, or null if none was found
     */
    public SystemElement findSystemElement(final int hiddenId) {
        final List<SystemElement> nodes = new ArrayList<>();

        nodes.add(getSystemRoot());
        nodes.addAll(getComponentInstances());
        nodes.addAll(getComponentOperators());

        for (final SystemElement node : nodes) {
            if (node.getHiddenId() == hiddenId) return node;
        }

        return null;
    }
}
