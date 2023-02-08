package ecdar.presentations;

import ecdar.abstractions.Component;
import ecdar.code_analysis.CodeAnalysis;
import ecdar.controllers.MessageCollectionController;
import javafx.collections.ObservableList;
import javafx.scene.layout.VBox;

public class MessageCollectionPresentation extends VBox {
    private final MessageCollectionController controller;

    public MessageCollectionPresentation(final Component component, final ObservableList<CodeAnalysis.Message> messages) {
        controller = new EcdarFXMLLoader().loadAndGetController("MessageCollectionPresentation.fxml", this);
        controller.setComponent(component);
        controller.setMessages(messages);
    }

    public MessageCollectionController getController() {
        return controller;
    }
}
