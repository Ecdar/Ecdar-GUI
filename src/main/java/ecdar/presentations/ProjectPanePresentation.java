package ecdar.presentations;

import ecdar.Ecdar;
import ecdar.controllers.ProjectPaneController;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.DropShadowHelper;
import com.jfoenix.controls.JFXRippler;
import javafx.geometry.Insets;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.*;

public class ProjectPanePresentation extends StackPane {

    private final ProjectPaneController controller;

    public ProjectPanePresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("ProjectPanePresentation.fxml", this);

        initializeRightBorder();
        initializeBackground();
        initializeToolbar();
        initializeToolbarButton(controller.createComponent);
        initializeToolbarButton(controller.createSystem);
        initializeAddModelIcons();
        Tooltip.install(controller.createComponent, new Tooltip("Add component"));
        Tooltip.install(controller.createSystem, new Tooltip("Add system"));
    }

    private void initializeRightBorder() {
        controller.toolbar.setBorder(new Border(new BorderStroke(
                Color.GREY_BLUE.getColor(Color.Intensity.I900),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(0, 1, 0, 0)
        )));

        showBottomInset(true);
    }

    private void initializeBackground() {
        controller.filesList.setBackground(new Background(new BackgroundFill(
                Color.GREY.getColor(Color.Intensity.I200),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));
    }

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

        // Set the elevation of the toolbar
        controller.toolbar.setEffect(DropShadowHelper.generateElevationShadow(8));
    }

    private void initializeToolbarButton(final JFXRippler button) {
        final Color color = Color.GREY_BLUE;
        final Color.Intensity colorIntensity = Color.Intensity.I800;

        button.setMaskType(JFXRippler.RipplerMask.CIRCLE);
        button.setRipplerFill(color.getTextColor(colorIntensity));
        button.setPosition(JFXRippler.RipplerPos.BACK);
    }

    /**
     * Initialises the icons for the two buttons for adding a system or a component
     */
    private void initializeAddModelIcons() {
        controller.createComponentImage.setImage(new Image(Ecdar.class.getResource("add_component_frame.png").toExternalForm()));
        MainPresentation.fitSizeWhenAvailable(controller.createComponentImage, controller.createComponentPane);
        controller.createSystemImage.setImage(new Image(Ecdar.class.getResource("add_system_frame.png").toExternalForm()));
        MainPresentation.fitSizeWhenAvailable(controller.createSystemImage, controller.createSystemPane);
    }

    /**
     * Inserts an edge/inset at the bottom of the scrollView
     * which is used to push up the elements of the scrollview
     * @param shouldShow boolean indicating whether to push up the items
     */
    public void showBottomInset(final Boolean shouldShow) {
        double bottomInsetWidth = 0;
        if(shouldShow) {
            bottomInsetWidth = 20;
        }

        // We set the border instead of the padding, as the border also pushes up the scrollbar
        controller.scrollPane.setBorder(new Border(new BorderStroke(
                Color.GREY.getColor(Color.Intensity.I400),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(0, 1, bottomInsetWidth, 0)
        )));
    }

    public ProjectPaneController getController() {
        return controller;
    }
}
