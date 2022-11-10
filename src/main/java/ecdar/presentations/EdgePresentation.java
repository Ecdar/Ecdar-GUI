package ecdar.presentations;

import ecdar.abstractions.Component;
import ecdar.abstractions.DisplayableEdge;
import ecdar.abstractions.Nail;
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
        initializeFailingEdgeListener();
    }

    private void initializeFailingEdgeListener() {
        controller.getEdge().failingProperty().addListener((observable, oldFailing, newFailing) -> onFailingUpdate(controller.getEdge(), newFailing));
    }

    private void onFailingUpdate(DisplayableEdge edge, Boolean isFailing) {
        for (Nail nail : edge.getNails()) {
            if (nail.getPropertyType().equals(DisplayableEdge.PropertyType.SYNCHRONIZATION)) {
                controller.getNailNailPresentationMap().get(nail).onFailingUpdate(isFailing);
            }
        }
    }

    public EdgeController getController() {
        return controller;
    }
}