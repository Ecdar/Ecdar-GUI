package ecdar.presentations;

import ecdar.backend.BackendDriver;
import ecdar.controllers.QueryPaneController;
import javafx.scene.layout.*;

public class QueryPanePresentation extends StackPane {
    private final QueryPaneController controller;

    public QueryPanePresentation(final BackendDriver backendDriver) {
        controller = new EcdarFXMLLoader().loadAndGetController("QueryPanePresentation.fxml", this);
        controller.setBackendDriver(backendDriver);
    }

    public QueryPaneController getController() {
        return controller;
    }
}