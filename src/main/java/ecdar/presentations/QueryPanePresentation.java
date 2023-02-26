package ecdar.presentations;

import ecdar.backend.QueryHandler;
import ecdar.controllers.QueryPaneController;
import javafx.scene.layout.*;

public class QueryPanePresentation extends StackPane {
    private final QueryPaneController controller;

    public QueryPanePresentation(QueryHandler queryHandler) {
        controller = new EcdarFXMLLoader().loadAndGetController("QueryPanePresentation.fxml", this);
        controller.setQueryHandler(queryHandler);
    }

    public QueryPaneController getController() {
        return controller;
    }
}