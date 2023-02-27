package ecdar.presentations;

import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.backend.BackendDriver;
import ecdar.controllers.QueryController;
import javafx.application.Platform;
import javafx.scene.layout.*;

public class QueryPresentation extends HBox {
    private final QueryController controller;

    public QueryPresentation(final Query query, final BackendDriver backendDriver) {
        controller = new EcdarFXMLLoader().loadAndGetController("QueryPresentation.fxml", this);
        controller.setQuery(query, backendDriver);

        // Ensure that the icons are scaled to current font scale
        Platform.runLater(() -> Ecdar.getPresentation().getController().scaleIcons(this));
    }

    public QueryController getController() {
        return controller;
    }
}
