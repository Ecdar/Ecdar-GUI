package ecdar.abstractions;

import ecdar.Ecdar;
import ecdar.utility.UndoRedoStack;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.function.BiConsumer;

public class GroupedEdge extends DisplayableEdge {
    private static final String ID = "id";
    private static final String EDGE_GROUP = "edge_group";

    private final ObservableList<Edge> edges = FXCollections.observableList(new ArrayList<>());
    private final StringProperty id = new SimpleStringProperty("");

    public GroupedEdge(Edge edge) {
        edges.add(edge);
        ioStatus = new SimpleObjectProperty<>(edge.ioStatus.get());
        initializeFromEdge(edge);

        edge.getNails().forEach(this::addNail);


        // ToDo NIELS: REMOVE!!!
        UndoRedoStack.setDebugRunnable(new BiConsumer<Stack<UndoRedoStack.Command>, Stack<UndoRedoStack.Command>>() {
            @Override
            public void accept(Stack<UndoRedoStack.Command> commands, Stack<UndoRedoStack.Command> commands2) {

            }
        });
    }

    private void initializeFromEdge(Edge edge) {
        setSourceLocation(edge.getSourceLocation());
        setTargetLocation(edge.getTargetLocation());
        setSelect(edge.getSelect());
        setGuard(edge.getGuard());
        setUpdate(edge.getUpdate());
        setColor(edge.getColor());
        setColorIntensity(edge.getColorIntensity());
        setIsHighlighted(edge.getIsHighlighted());
        setIsLocked(edge.getIsLocked().getValue());
        setStatus(edge.getStatus());
    }

    public boolean addEdgeToGroup(Edge newEdge) {
        return edges.add(newEdge);
    }

    /**
     * Remove the latest non-empty edge from the edge list. This is necessary, as the latest edge will always have an empty sync.
     */
    public void removeEdgeAddedBeforeEmptySync() {
        edges.remove(edges.size() - 2);
    }

    public ObservableList<Edge> getEdges() {
        return edges;
    }

    public ArrayList<String> getSyncs() {
        ArrayList<String> syncs = new ArrayList<>();

        for (Edge edge : edges) {
            syncs.add(edge.getSync());
        }

        return syncs;
    }

    public String getId() {
        return id.get();
    }

    /**
     * Generate and sets a unique id for this location
     */
    protected void setId() {
        for(int counter = 0; ; counter++) {
            if(!Ecdar.getProject().getLocationIds().contains(String.valueOf(counter))){
                id.set(EDGE_GROUP + counter);
                return;
            }
        }
    }

    /**
     * Sets a specific id for this location
     * @param string id to set
     */
    public void setId(final String string){
        id.set(string);
    }

    public StringProperty idProperty() {
        return id;
    }

    public Edge getBaseSubEdge() {
        Edge edge = new Edge(getSourceLocation(), getStatus());

        // Initialize edge with new sync
        edge.setTargetLocation(this.getTargetLocation());
        edge.setGuard(this.getGuard());
        edge.setUpdate(this.getGuard());
        edge.setProperty(Edge.PropertyType.SELECTION, Collections.singletonList(this.getSelect()));
        edge.setIsLocked(this.getIsLocked().getValue());
        edge.setIsHighlighted(this.getIsHighlighted());
        edge.setGroup(this.getId());

        return edge;
    }

    public List<String> getProperty(final Edge.PropertyType propertyType) {
        List<String> result = new ArrayList<>();
        if (propertyType.equals(PropertyType.SELECTION)) {
            result.add(getSelect());
            return result;
        } else if (propertyType.equals(PropertyType.GUARD)) {
            result.add(getGuard());
            return result;
        } else if (propertyType.equals(PropertyType.SYNCHRONIZATION)) {
            result.addAll(getSyncs());
            return result;
        } else if (propertyType.equals(PropertyType.UPDATE)) {
            result.add(getUpdate());
            return result;
        } else {
            return result;
        }
    }

    public void setProperty(final PropertyType propertyType, final List<String> newProperty) {
        if (propertyType.equals(PropertyType.SELECTION)) {
            selectProperty().unbind();
            setSelect(newProperty.get(0));
        } else if (propertyType.equals(PropertyType.GUARD)) {
            guardProperty().unbind();
            setGuard(newProperty.get(0));
        } else if (propertyType.equals(PropertyType.SYNCHRONIZATION)) {
            // ToDo NIELS: Handle synchronization property
        } else if (propertyType.equals(PropertyType.UPDATE)) {
            updateProperty().unbind();
            setUpdate(newProperty.get(0));
        }
    }

    public List<StringProperty> getSyncProperties() {
        List<StringProperty> syncProperties = new ArrayList<>();

        for (Edge edge : edges) {
            syncProperties.add(edge.syncProperty());
        }

        return syncProperties;
    }
}
