package ecdar.controllers;

import ecdar.Ecdar;
import ecdar.abstractions.Declarations;
import ecdar.presentations.ComponentPresentation;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.Initializable;
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

    private double offSet;
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
        root.minWidthProperty().bind(Ecdar.getPresentation().getController().getEditorPresentation().getController().canvasPane.minWidthProperty());
        root.maxWidthProperty().bind(Ecdar.getPresentation().getController().getEditorPresentation().getController().canvasPane.maxWidthProperty());
        root.minHeightProperty().bind(Ecdar.getPresentation().getController().getEditorPresentation().getController().canvasPane.minHeightProperty());
        root.maxHeightProperty().bind(Ecdar.getPresentation().getController().getEditorPresentation().getController().canvasPane.maxHeightProperty());

        updateOffset(EcdarController.getActiveCanvasPresentation().getController().getInsetShouldShow().get());
        EcdarController.getActiveCanvasPresentation().getController().getInsetShouldShow().addListener((observable, oldValue, newValue) -> {
            updateOffset(newValue);
        });
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

    /**
     * Updates if height of the view should have an offset at the bottom.
     * Whether the view should have an offset is based on the configuration of the error view.
     * @param shouldHave true iff views should have an offset
     */
    private void updateOffset(final boolean shouldHave) {
        if (shouldHave) {
            offSet = 20;
        } else {
            offSet = 0;
        }
    }
}
