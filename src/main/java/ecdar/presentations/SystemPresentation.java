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
    }
}
