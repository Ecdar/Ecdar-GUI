package ecdar.presentations;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.controllers.SimEdgeController;
import ecdar.controllers.SimulationController;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;

/**
 * The presentation class for the edges shown in the {@link SimulationPresentation}
 */
public class SimEdgePresentation extends Group {
    private final SimEdgeController controller;

    private final ObjectProperty<Edge> edge = new SimpleObjectProperty<>();
    private final ObjectProperty<Component> component = new SimpleObjectProperty<>();

    public SimEdgePresentation(final Edge edge, final Component component) {
        controller = new EcdarFXMLLoader().loadAndGetController("SimEdgePresentation.fxml", this);

        controller.setEdge(edge);
        this.edge.bind(controller.edgeProperty());

        controller.setComponent(component);
        this.component.bind(controller.componentProperty());

        // when hovering mouse the cursor should change to hand
        this.setOnMouseEntered(event -> {
            // ToDo NIELS: Handle change of functionality and utilization of ComponentInstances
            if (controller.getEdge().isHighlighted())
                this.getScene().setCursor(javafx.scene.Cursor.HAND);
        });
        this.setOnMouseExited(event -> this.getScene().setCursor(javafx.scene.Cursor.DEFAULT));
    }

    public SimEdgeController getController() {
        return controller;
    }

}
