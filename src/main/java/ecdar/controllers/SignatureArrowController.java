package ecdar.controllers;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.SelectHelper;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class SignatureArrowController {
    private final ObjectProperty<Component> component = new SimpleObjectProperty<>();
    private final ObjectProperty<EdgeStatus> edgeStatus = new SimpleObjectProperty<>();
    private final StringProperty syncText = new SimpleStringProperty();

    public Label signatureArrowLabel;
    public Path signatureArrowPath;
    public Path signatureArrowHeadPath;
    public Circle signatureArrowCircle;
    public VBox arrowBox;

    public void initialize(final URL location, final ResourceBundle resources) {

    }

    /***
     * Finds matching edges and highlights them
     */
    public void highlightEdges() {
        // Clear the currently selected elements, so we don't have multiple things highlighted/selected
        SelectHelper.clearSelectedElements();
        final List<Edge> edgesToHighlight = findEdgesForSync();
        edgesToHighlight.forEach(edge -> edge.setIsHighlighted(true));
    }

    /***
     * Finds matching edges and removes the highlight
     */
    public void unhighlightEdges() {
        final List<Edge> edgesToDeHighlight = findEdgesForSync();
        edgesToDeHighlight.forEach(edge -> edge.setIsHighlighted(false));
    }

    /***
     * Finds the edges of the Component that match this class's edgeStatus and syncText
     * @return The list of edges that match
     */
    private List<Edge> findEdgesForSync() {
        List<Edge> matchingEdges = new ArrayList<>();

        component.get().getEdges().forEach((edge) -> {
            final String edgeSync = edge.getSync();
            final EdgeStatus edgeStatus = edge.ioStatus.get();

            if(edgeSync.equals(this.getSyncText()) && edgeStatus == this.getEdgeStatus()) {
                matchingEdges.add(edge);
            }
        });

        return matchingEdges;
    }

    /***
     * Highlights edges when mouse enters the root
     */
    public void mouseEntered() {
        highlightEdges();
    }

    /***
     * Removes highlight from edges when mouse exits the root
     */
    public  void mouseExited() { unhighlightEdges(); }

    public void setEdgeStatus(final EdgeStatus edgeStatus) { this.edgeStatus.set(edgeStatus); }
    public EdgeStatus getEdgeStatus() { return edgeStatus.get(); }

    public void setSyncText(final String syncText) { this.syncText.set(syncText); }
    public String getSyncText() { return syncText.get(); }

    public void setComponent(final Component component) { this.component.set(component); }
    public Component getComponent() { return this.component.get(); }
}
