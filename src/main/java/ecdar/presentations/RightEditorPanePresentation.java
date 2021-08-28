package ecdar.presentations;

import ecdar.controllers.RightEditorPaneController;
import ecdar.utility.colors.Color;
import javafx.geometry.Insets;
import javafx.scene.layout.*;

/**
 * Presentation class for the right pane in the editor
 */
public class RightEditorPanePresentation extends StackPane {
    private RightEditorPaneController controller;

    public RightEditorPanePresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("RightEditorPanePresentation.fxml", this);

        initializeBackground();
        initializeLeftBorder();
    }

    /**
     * Sets the background color of the ScrollPane Vbox
     */
    private void initializeBackground() {
        controller.scrollPaneVbox.setBackground(new Background(new BackgroundFill(
                Color.GREY.getColor(Color.Intensity.I200),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));
    }

    /**
     * Initializes the thin border on the left side of the querypane toolbar
     */
    private void initializeLeftBorder() {
        controller.queryPane.getController().toolbar.setBorder(new Border(new BorderStroke(
                Color.GREY_BLUE.getColor(Color.Intensity.I900),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(0, 0, 0, 1)
        )));
    }

}
