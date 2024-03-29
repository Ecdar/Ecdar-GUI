package ecdar.presentations;

import ecdar.abstractions.Component;
import ecdar.abstractions.DisplayableEdge;
import ecdar.controllers.EdgeController;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;

public class EdgePresentation extends Group {
    private final EdgeController controller;

    private final ObjectProperty<DisplayableEdge> edge = new SimpleObjectProperty<>();
    private final ObjectProperty<Component> component = new SimpleObjectProperty<>();

    public EdgePresentation(final DisplayableEdge edge, final Component component) {
        controller = new EcdarFXMLLoader().loadAndGetController("EdgePresentation.fxml", this);

        controller.setEdge(edge);
        this.edge.bind(controller.edgeProperty());

        controller.setComponent(component);
        this.component.bind(controller.componentProperty());
    }

    public EdgeController getController() {
        return controller;
    }
}