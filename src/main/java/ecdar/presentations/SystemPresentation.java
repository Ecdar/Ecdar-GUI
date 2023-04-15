package ecdar.presentations;

import ecdar.abstractions.*;
import ecdar.controllers.SystemController;

/**
 * Presentation for a system.
 */
public class SystemPresentation extends ModelPresentation {
    private final SystemController controller;

    public SystemPresentation(final EcdarSystem system) {
        controller = new EcdarFXMLLoader().loadAndGetController("SystemPresentation.fxml", this);
        controller.setSystem(system);

        minHeightProperty().bindBidirectional(system.getBox().getHeightProperty());
        maxHeightProperty().bindBidirectional(system.getBox().getHeightProperty());
        minWidthProperty().bindBidirectional(system.getBox().getWidthProperty());
        maxWidthProperty().bindBidirectional(system.getBox().getWidthProperty());
    }

    @Override
    public SystemController getController() {
        return controller;
    }
}
