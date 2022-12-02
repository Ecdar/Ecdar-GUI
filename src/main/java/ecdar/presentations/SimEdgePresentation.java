package ecdar.presentations;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.controllers.SimEdgeController;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.util.Pair;

/**
 * The presentation class for the edges shown in the {@link SimulatorOverviewPresentation}
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

        // when hovering mouse the curser should change to hand
        this.setOnMouseEntered(event -> {
            if (Ecdar.getSimulationHandler().currentState.get().getEdges().contains(new Pair<>(component.getName(), edge.getId())))
                this.getScene().setCursor(javafx.scene.Cursor.HAND);
        });
        this.setOnMouseExited(event -> this.getScene().setCursor(javafx.scene.Cursor.DEFAULT));

        // when clicking the edge the edge should be selected and the simulation should take next step (if the edge is enabled)
        this.setOnMouseClicked(event -> {
            if (Ecdar.getSimulationHandler().currentState.get().getEdges().contains(new Pair<>(component.getName(), edge.getId()))) {
                Ecdar.getSimulationHandler().selectedEdge.set(edge);
                Ecdar.getSimulationHandler().nextStep();
            }
        });
    }

    public SimEdgeController getController() {
        return controller;
    }

}
