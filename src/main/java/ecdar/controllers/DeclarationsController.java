package ecdar.controllers;

import ecdar.Ecdar;
import ecdar.abstractions.Declarations;
import ecdar.presentations.ComponentPresentation;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for overall declarations.
 */
public class DeclarationsController implements Initializable {
    public StyleClassedTextArea textArea;
    public StackPane root;
    public BorderPane frame;

    private final ObjectProperty<Declarations> declarations;

    public DeclarationsController() {
        declarations = new SimpleObjectProperty<>(null);
    }

    public void setDeclarations(final Declarations declarations) {
        this.declarations.set(declarations);
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        initializeWidthAndHeight();
        initializeText();
    }

    /**
     * Initializes width and height of the text editor field, such that it fills up the whole canvas
     */
    private void initializeWidthAndHeight() {
        // Fetch width and height of canvas and update
        root.minWidthProperty().bind(Ecdar.getPresentation().getController().canvasPane.minWidthProperty());
        root.maxWidthProperty().bind(Ecdar.getPresentation().getController().canvasPane.maxWidthProperty());
        root.minHeightProperty().bind(Ecdar.getPresentation().getController().canvasPane.minHeightProperty());
        root.maxHeightProperty().bind(Ecdar.getPresentation().getController().canvasPane.maxHeightProperty());
        textArea.setTranslateY(20);
    }

    /**
     * Sets up the linenumbers and binds the text in the text area to the declaration object
     */
    private void initializeText() {
        textArea.setParagraphGraphicFactory(LineNumberFactory.get(textArea));

        // Bind the declarations of the abstraction the the view
        declarations.addListener((observable, oldValue, newValue) -> {
            textArea.replaceText(0, textArea.getLength(), declarations.get().getDeclarationsText());

            // Initially style the declarations
            updateHighlighting();
        });

        textArea.textProperty().addListener((observable, oldDeclaration, newDeclaration) ->
                declarations.get().setDeclarationsText(newDeclaration));
    }

    /**
     * Updates highlighting of the text in the text area.
     */
    public void updateHighlighting() {
        textArea.setStyleSpans(0, ComponentPresentation.computeHighlighting(declarations.get().getDeclarationsText()));
    }
}
