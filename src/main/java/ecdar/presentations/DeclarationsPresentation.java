package ecdar.presentations;

import ecdar.abstractions.Declarations;
import ecdar.controllers.DeclarationsController;
import ecdar.controllers.HighLevelModelController;

/**
 * Presentation for overall declarations.
 */
public class DeclarationsPresentation extends HighLevelModelPresentation {
    private final DeclarationsController controller;

    public DeclarationsPresentation(final Declarations declarations) {
        controller = new EcdarFXMLLoader().loadAndGetController("DeclarationsPresentation.fxml", this);
        controller.setDeclarations(declarations);

        // Listen to changes and
        controller.textArea.textProperty().addListener((obs, oldText, newText) ->
                controller.updateHighlighting());
    }

    @Override
    public HighLevelModelController getController() {
        return controller;
    }
}
