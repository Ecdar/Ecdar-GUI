package ecdar.presentations;

import ecdar.abstractions.Declarations;
import ecdar.controllers.DeclarationsController;

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
