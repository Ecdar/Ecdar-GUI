package ecdar.presentations;

import ecdar.controllers.QueryPaneController;
import javafx.scene.layout.*;

public class QueryPanePresentation extends StackPane {
    private final QueryPaneController controller;

    public QueryPanePresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("QueryPanePresentation.fxml", this);
    }

    public QueryPaneController getController() {
        return controller;
    }
}