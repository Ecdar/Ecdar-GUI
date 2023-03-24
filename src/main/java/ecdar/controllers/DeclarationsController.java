package ecdar.controllers;

import ecdar.abstractions.Declarations;
import ecdar.abstractions.HighLevelModel;
import ecdar.utility.helpers.UPPAALSyntaxHighlighter;
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
public class DeclarationsController extends HighLevelModelController implements Initializable {
    public StackPane root;
    public BorderPane frame;
    public StyleClassedTextArea textArea;

    private final ObjectProperty<Declarations> declarations;

    public DeclarationsController() {
        declarations = new SimpleObjectProperty<>(null);
    }

    public void setDeclarations(final Declarations declarations) {
        this.declarations.set(declarations);
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        initializeText();
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
     * Bind width and height of the text editor field, such that it fills up the provided canvas
     */
    public void bindWidthAndHeightToPane(StackPane pane) {
        // Fetch width and height of canvas and update
        root.minWidthProperty().bind(pane.minWidthProperty());
        root.maxWidthProperty().bind(pane.maxWidthProperty());
        root.minHeightProperty().bind(pane.minHeightProperty());
        root.maxHeightProperty().bind(pane.maxHeightProperty());
        textArea.setTranslateY(20);
    }

    /**
     * Updates highlighting of the text in the text area.
     */
    public void updateHighlighting() {
        textArea.setStyleSpans(0, UPPAALSyntaxHighlighter.computeHighlighting(declarations.get().getDeclarationsText()));
    }

    @Override
    public HighLevelModel getModel() {
        return declarations.get();
    }
}
