package ecdar.presentations;

import ecdar.controllers.RightSimPaneController;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.DropShadowHelper;
import javafx.geometry.Insets;
import javafx.scene.layout.*;

/**
 * Presentation class for the right pane in the simulator
 */
public class RightSimPanePresentation extends StackPane {
    private RightSimPaneController controller;

    public RightSimPanePresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("RightSimPanePresentation.fxml", this);

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
        controller.queryPaneElement.getController().toolbar.setBorder(new Border(new BorderStroke(
                Color.GREY_BLUE.getColor(Color.Intensity.I900),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(0, 0, 0, 1)
        )));
    }

}
