package SW9.presentations;

import SW9.abstractions.Declarations;
import SW9.controllers.DeclarationsController;
import javafx.fxml.FXMLLoader;
import javafx.fxml.JavaFXBuilderFactory;
import javafx.scene.layout.StackPane;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.io.IOException;
import java.net.URL;

import static SW9.presentations.ComponentPresentation.computeHighlighting;

/**
 * Presentation for overall declarations.
 */
public class DeclarationPresentation extends StackPane {
    private final DeclarationsController controller;

    public DeclarationPresentation(final Declarations declarations) {
        final URL location = this.getClass().getResource("DeclarationPresentation.fxml");

        final FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(location);
        fxmlLoader.setBuilderFactory(new JavaFXBuilderFactory());

        try {
            fxmlLoader.setRoot(this);
            fxmlLoader.load(location.openStream());

            controller = fxmlLoader.getController();
            controller.setDeclarations(declarations);

            // Listen to changes and
            controller.textArea.textProperty().addListener((obs, oldText, newText) ->
                    controller.updateHighlighting());
        } catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }
}
