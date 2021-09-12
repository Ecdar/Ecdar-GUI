package ecdar.simulation;

import ecdar.Debug;
import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.Nail;
import ecdar.presentations.SimNailPresentation;
import ecdar.presentations.SimTagPresentation;
import ecdar.utility.colors.Color;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;

import java.net.URL;
import java.util.ResourceBundle;

public class SimNailController implements Initializable {
    private final ObjectProperty<Component> component = new SimpleObjectProperty<>();
    private final ObjectProperty<Edge> edge = new SimpleObjectProperty<>();
    private final ObjectProperty<Nail> nail = new SimpleObjectProperty<>();

    private SimEdgeController edgeController;
    public SimNailPresentation root;
    public Circle nailCircle;
    public Circle dragCircle;
    public Line propertyTagLine;
    public SimTagPresentation propertyTag;
    public Group dragGroup;
    public Label propertyLabel;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        nail.addListener((obsNail, oldNail, newNail) -> {

            // The radius from the abstraction is the master and the view simply reflects what is in the model
            nailCircle.radiusProperty().bind(newNail.radiusProperty());

            // Draw the presentation based on the initial value from the abstraction
            root.setLayoutX(newNail.getX());
            root.setLayoutY(newNail.getY());

            // Reflect future updates from the presentation into the abstraction
            newNail.xProperty().bindBidirectional(root.layoutXProperty());
            newNail.yProperty().bindBidirectional(root.layoutYProperty());

        });

        // Debug visuals
        dragCircle.opacityProperty().bind(Debug.draggableAreaOpacity);
        dragCircle.setFill(Debug.draggableAreaColor.getColor(Debug.draggableAreaColorIntensity));
    }

    /**
     * Sets an edge controller.
     * This should be called when adding a nail.
     * @param controller the edge controller
     */
    public void setEdgeController(final SimEdgeController controller) {
        this.edgeController = controller;
    }

    public Nail getNail() {
        return nail.get();
    }

    public void setNail(final Nail nail) {
        this.nail.set(nail);
    }

    public ObjectProperty<Nail> nailProperty() {
        return nail;
    }

    public Component getComponent() {
        return component.get();
    }

    public void setComponent(final Component component) {
        this.component.set(component);
    }

    public ObjectProperty<Component> componentProperty() {
        return component;
    }

    public Edge getEdge() {
        return edge.get();
    }

    public void setEdge(final Edge edge) {
        this.edge.set(edge);
    }

    public ObjectProperty<Edge> edgeProperty() {
        return edge;
    }

    public Color getColor() {
        return getComponent().getColor();
    }

    public Color.Intensity getColorIntensity() {
        return getComponent().getColorIntensity();
    }

    public DoubleProperty xProperty() {
        return root.layoutXProperty();
    }

    public DoubleProperty yProperty() {
        return root.layoutYProperty();
    }

    public double getX() {
        return xProperty().get();
    }

    public double getY() {
        return yProperty().get();
    }
}
