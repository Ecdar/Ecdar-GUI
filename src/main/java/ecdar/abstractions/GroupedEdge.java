package ecdar.abstractions;

import ecdar.Ecdar;
import ecdar.controllers.EcdarController;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroupedEdge extends DisplayableEdge {
    private static final String ID = "id";
    private static final String GROUPED_EDGE = "GE";

    private final ArrayList<Edge> edges = new ArrayList<>();
    private final StringProperty id = new SimpleStringProperty("");

    public GroupedEdge(Edge edge) {
        edges.add(edge);
        initializeFromEdge(edge);

        //Add guard nail
        Nail nail = new Nail(-10, -10);
        nail.setPropertyType(Edge.PropertyType.GUARD);
        addNail(nail);

        // Add update nail
        nail = new Nail(10, 10);
        nail.setPropertyType(Edge.PropertyType.UPDATE);
        addNail(nail);
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
        if(newEdge.getGuard().equals(this.edges.get(0).getGuard()) && newEdge.getUpdate().equals(this.edges.get(0).getUpdate())) {
            return edges.add(newEdge);
        } else {
            return false;
        }
    }

    public ArrayList<Edge> getEdges() {
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
    private void setId() {
        for(int counter = 0; ; counter++) {
            if(!Ecdar.getProject().getLocationIds().contains(String.valueOf(counter))){
                id.set(GROUPED_EDGE + counter);
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

    public void addSync(final String sync) {
        // Initialize edge with new sync
        Edge edge = new Edge(getSourceLocation(), getStatus());
        edge.setTargetLocation(this.getTargetLocation());
        edge.setGuard(this.getGuard());
        edge.setUpdate(this.getGuard());
        edge.setColor(this.getColor());
        edge.setColorIntensity(this.getColorIntensity());
        edge.setProperty(Edge.PropertyType.SELECTION, Collections.singletonList(this.getSelect()));
        edge.setIsLocked(this.getIsLocked().getValue());
        edge.setIsHighlighted(this.getIsHighlighted());
        edge.setSync(sync);
        edge.setGroup(this.getId());

        this.edges.add(edge);
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
            // ToDo: Handle synchronization property
        } else if (propertyType.equals(PropertyType.UPDATE)) {
            updateProperty().unbind();
            setUpdate(newProperty.get(0));
        }
    }
}
