package ecdar.presentations;

import com.jfoenix.controls.JFXRippler;
import ecdar.controllers.TransitionPaneElementController;
import ecdar.utility.colors.Color;
import javafx.geometry.Insets;
import javafx.scene.layout.*;

/**
 * The presentation class for the transition pane element that can be inserted into the simulator panes
 */
public class TransitionPaneElementPresentation extends VBox {
    final private TransitionPaneElementController controller;

    public TransitionPaneElementPresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("TransitionPaneElementPresentation.fxml", this);

        initializeToolbar();
        initializeDelayChooser();
    }

    /**
     * Initializes the toolbar for the transition pane element.
     * Sets the background of the toolbar and changes the title color.
     * Also changes the look of the rippler effect.
     */
    private void initializeToolbar() {
        final Color color = Color.GREY_BLUE;
        final Color.Intensity colorIntensity = Color.Intensity.I800;

        // Set the background of the toolbar
        controller.toolbar.setBackground(new Background(new BackgroundFill(
                color.getColor(colorIntensity),
                CornerRadii.EMPTY,
                Insets.EMPTY)));
        // Set the font color of elements in the toolbar
        controller.toolbarTitle.setTextFill(color.getTextColor(colorIntensity));

        controller.refreshRippler.setMaskType(JFXRippler.RipplerMask.CIRCLE);
        controller.refreshRippler.setRipplerFill(color.getTextColor(colorIntensity));

        controller.expandTransition.setMaskType(JFXRippler.RipplerMask.CIRCLE);
        controller.expandTransition.setRipplerFill(color.getTextColor(colorIntensity));
    }

    /**
     * Sets the background color of the delay chooser
     */
    private void initializeDelayChooser() {
        final Color color = Color.GREY_BLUE;
        final Color.Intensity colorIntensity = Color.Intensity.I50;
        controller.delayChooser.setBackground(new Background(new BackgroundFill(
                color.getColor(colorIntensity),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));

        controller.delayChooser.setBorder(new Border(new BorderStroke(
                color.getColor(colorIntensity.next(2)),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(0, 0, 1, 0)
        )));
    }

    public TransitionPaneElementController getController() {
        return controller;
    }
}
