package SW9.presentations;

import SW9.abstractions.Declarations;
import SW9.controllers.DeclarationsController;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;

/**
 * Presentation for overall declarations.
 */
public class DeclarationPresentation extends HighLevelModelPresentation {
    private final DeclarationsController controller;

    public DeclarationPresentation(final Declarations declarations) {
        controller = new EcdarFXMLLoader().loadAndGetController("DeclarationPresentation.fxml", this);
        controller.setDeclarations(declarations);

        // Listen to changes and
        controller.textArea.textProperty().addListener((obs, oldText, newText) ->
                controller.updateHighlighting());
    }
}
