package ecdar.presentations;

import ecdar.controllers.LeftSimPaneController;
import ecdar.utility.colors.Color;
import javafx.geometry.Insets;
import javafx.scene.layout.*;

public class LeftSimPanePresentation extends StackPane {
    private LeftSimPaneController controller;

    public LeftSimPanePresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("LeftSimPanePresentation.fxml", this);

        initializeBackground();
        initializeRightBorder();
    }

    private void initializeBackground() {
        controller.scrollPaneVbox.setBackground(new Background(new BackgroundFill(
                Color.GREY.getColor(Color.Intensity.I200),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));
    }

    /**
     * Initializes the thin border on the right side of the transition toolbar
     */
    private void initializeRightBorder() {
        controller.transitionPanePresentation.getController().toolbar.setBorder(new Border(new BorderStroke(
                Color.GREY_BLUE.getColor(Color.Intensity.I900),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(0, 1, 0, 0)
        )));
    }
}
