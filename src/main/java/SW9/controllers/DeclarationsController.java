package SW9.controllers;

import SW9.abstractions.Declarations;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.Initializable;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.net.URL;
import java.util.ResourceBundle;

/**
 *
 */
public class DeclarationsController implements Initializable {
    public StyleClassedTextArea declaration;
    public BorderPane frame;
    public StackPane root;
    private double offSet = 200, canvasHeight;

    public void setDeclarations(Declarations declarations) {

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        root.setMinWidth(CanvasController.getWidthProperty().doubleValue());
        canvasHeight = CanvasController.getHeightProperty().doubleValue();
        if (CanvasController.getInsetShouldShow().get()) {
            offSet = 20;
        } else {
            offSet = 0;
        }
        updateHeight();

        CanvasController.getWidthProperty().addListener((observable, oldValue, newValue) -> {
            root.setMinWidth(newValue.doubleValue());
            root.setMaxWidth(newValue.doubleValue());
            frame.setMinWidth(newValue.doubleValue());
            frame.setMaxWidth(newValue.doubleValue());
        });
        CanvasController.getHeightProperty().addListener((observable, oldValue, newValue) -> {
            canvasHeight = newValue.doubleValue();
            updateHeight();
        });

        CanvasController.getInsetShouldShow().addListener((observable, oldValue, newValue) -> {
            if (newValue)
                offSet = 20;
            else
                offSet = 0;

            updateHeight();
        });


        declaration.setParagraphGraphicFactory(LineNumberFactory.get(declaration));

    }

    private void updateHeight() {
        final double value = canvasHeight - CanvasController.DECLARATION_X_MARGIN - offSet;

        root.setMinHeight(value);
        root.setMaxHeight(value);
        frame.setMinHeight(value);
        frame.setMaxHeight(value);
    }
}
