package ecdar.presentations;

import ecdar.controllers.QueryPaneController;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.DropShadowHelper;
import com.jfoenix.controls.JFXRippler;
import javafx.geometry.Insets;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;

public class QueryPanePresentation extends StackPane {

    private final QueryPaneController controller;

    public QueryPanePresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("QueryPanePresentation.fxml", this);

        initializeLeftBorder();
        initializeToolbar();
        initializeBackground();
    }

    private void initializeLeftBorder() {
        controller.toolbar.setBorder(new Border(new BorderStroke(
                Color.GREY_BLUE.getColor(Color.Intensity.I900),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(0, 0, 0, 1)
        )));
    }

    private void initializeBackground() {
        controller.queriesList.setBackground(new Background(new BackgroundFill(
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

        controller.runAllQueriesButton.setBackground(new Background(new BackgroundFill(
                javafx.scene.paint.Color.TRANSPARENT,
                new CornerRadii(100),
                Insets.EMPTY)));

        controller.addButton.setMaskType(JFXRippler.RipplerMask.CIRCLE);
        controller.addButton.setRipplerFill(color.getTextColor(colorIntensity));
        Tooltip.install(controller.addButton, new Tooltip("Add query"));

        controller.runAllQueriesButton.setMaskType(JFXRippler.RipplerMask.CIRCLE);
        controller.runAllQueriesButton.setRipplerFill(color.getTextColor(colorIntensity));
        Tooltip.install(controller.runAllQueriesButton, new Tooltip("Run all queries"));

        controller.clearAllQueriesButton.setMaskType(JFXRippler.RipplerMask.CIRCLE);
        controller.clearAllQueriesButton.setRipplerFill(color.getTextColor(colorIntensity));
        Tooltip.install(controller.clearAllQueriesButton, new Tooltip("Clear all queries"));

        // Set the elevation of the toolbar
        controller.toolbar.setEffect(DropShadowHelper.generateElevationShadow(8));
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

        controller.scrollPane.setBorder(new Border(new BorderStroke(
                Color.GREY.getColor(Color.Intensity.I400),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(0, 1, bottomInsetWidth, 0)
        )));
    }
}