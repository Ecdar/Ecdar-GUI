package ecdar.abstractions;

import ecdar.Ecdar;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.List;

public class GroupedEdge extends DisplayableEdge {
    private final ObservableList<Edge> edges = FXCollections.observableList(new ArrayList<>());
    private final StringProperty id = new SimpleStringProperty("");

    public GroupedEdge(Edge initialEdge) {
        if (!initialEdge.getSync().equals("")) {
            edges.add(initialEdge);
        }

        ioStatus = new SimpleObjectProperty<>(initialEdge.ioStatus.get());
        initializeFromEdge(initialEdge);
        setTargetLocation(initialEdge.getTargetLocation());

        initialEdge.getNails().forEach(this::addNail);

        setId();
    }

    public GroupedEdge(ObservableList<Edge> initialEdges) {
        Edge edge = initialEdges.get(0);

        if (!edge.getSync().equals("")) {
            this.edges.add(edge);
        }

        ioStatus = new SimpleObjectProperty<>(edge.ioStatus.get());
        initializeFromEdge(edge);

        edge.getNails().forEach(nail -> {
            // For some reason the synchronization nails are added when dragging, so this prevents that
            if(!nail.getPropertyType().equals(PropertyType.SYNCHRONIZATION)) {
                this.addNail(nail);
            }
        });
        initialEdges.stream().skip(1).forEach(this::addEdgeToGroup);

        setId();
    }

    private void initializeFromEdge(Edge edge) {
        setSourceLocation(edge.getSourceLocation());
        setSelect(edge.getSelect());
        setGuard(edge.getGuard());
        setUpdate(edge.getUpdate());
        setColor(edge.getColor());
        setIsHighlighted(edge.getIsHighlighted());
        setIsLocked(edge.getIsLockedProperty().getValue());
        setStatus(edge.getStatus());
    }

    public boolean addEdgeToGroup(Edge newEdge) {
        if (!newEdge.getGuard().equals(this.getGuard()) || !newEdge.getUpdate().equals(this.getUpdate())) {
            return false;
        }

        return edges.add(newEdge);
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
            if(!Ecdar.getProject().getEdgeIds().contains(String.valueOf(counter))){
                id.set(Edge.EDGE_GROUP + counter);
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
        edge.sourceLocation.set(this.getSourceLocation());
        edge.targetLocation.set(this.getTargetLocation());
        edge.ioStatus.bind(this.ioStatus);
        edge.selectProperty().bind(this.selectProperty());
        edge.guardProperty().bind(this.guardProperty());
        edge.updateProperty().bind(this.updateProperty());
        edge.colorProperty().bind(this.colorProperty());
        edge.setIsHighlighted(this.getIsHighlighted());
        edge.getIsLockedProperty().bind(this.getIsLockedProperty());
        edge.setGroup(this.getId());
        edge.makeSyncNailBetweenLocations();

        edge.getNails().addAll(getNails());
        getNails().addListener((ListChangeListener<Nail>) c -> {
            edge.getNails().clear();
            edge.getNails().addAll(getNails());
        });

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

    @Override
    public void setTargetLocation(final Location targetLocation) {
        this.targetLocation.set(targetLocation);
        updateTargetCircular();

        this.edges.forEach(e -> {
            e.setTargetLocation(targetLocation);
        });
    }

    @Override
    public void setSourceLocation(final Location sourceLocation) {
        this.sourceLocation.set(sourceLocation);
        updateSourceCircular();

        this.edges.forEach(e -> {
            e.setSourceLocation(sourceLocation);
        });
    }
}
