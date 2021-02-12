package ecdar.controllers;

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

    private double offSet, canvasHeight;
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
        root.setMinWidth(EcdarController.activeCanvasPresentation.getController().getWidthProperty().doubleValue());
        canvasHeight = EcdarController.activeCanvasPresentation.getController().getHeightProperty().doubleValue();
        updateOffset(EcdarController.activeCanvasPresentation.getController().getInsetShouldShow().get());
        updateHeight();

        EcdarController.activeCanvasPresentation.getController().getWidthProperty().addListener((observable, oldValue, newValue) -> {
            root.setMinWidth(newValue.doubleValue());
            root.setMaxWidth(newValue.doubleValue());
        });
        EcdarController.activeCanvasPresentation.getController().getHeightProperty().addListener((observable, oldValue, newValue) -> {
            canvasHeight = newValue.doubleValue();
            updateHeight();
        });
        EcdarController.activeCanvasPresentation.getController().getInsetShouldShow().addListener((observable, oldValue, newValue) -> {
            updateOffset(newValue);
            updateHeight();
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

    /**
     * Updates the height of the view.
     */
    private void updateHeight() {
        final double value = canvasHeight - EcdarController.activeCanvasPresentation.getController().DECLARATION_Y_MARGIN - offSet;

        root.setMinHeight(value);
        root.setMaxHeight(value);
    }
}
