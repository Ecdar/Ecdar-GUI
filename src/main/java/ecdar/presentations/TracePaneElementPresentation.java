package ecdar.presentations;

import com.jfoenix.controls.JFXRippler;
import ecdar.controllers.TracePaneElementController;
import ecdar.utility.colors.Color;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.layout.*;

import java.util.function.BiConsumer;

/**
 * The presentation class for the trace element that can be inserted into the simulator panes
 */
public class TracePaneElementPresentation extends VBox {
    final private TracePaneElementController controller;

    public TracePaneElementPresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("TracePaneElementPresentation.fxml", this);

        initializeToolbar();
        initializeSummaryView();
    }

    /**
     * Initializes the tool bar that contains the trace pane element's title and buttons
     * Sets the color of the bar and title label. Also sets the look of the rippler effect
     */
    private void initializeToolbar() {
        final Color color = Color.GREY_BLUE;
        final Color.Intensity colorIntensity = Color.Intensity.I800;

        controller.toolbar.setBackground(new Background(new BackgroundFill(
                color.getColor(colorIntensity),
                CornerRadii.EMPTY,
                Insets.EMPTY)));
        controller.traceTitle.setTextFill(color.getTextColor(colorIntensity));

        controller.expandTrace.setMaskType(JFXRippler.RipplerMask.CIRCLE);
        controller.expandTrace.setRipplerFill(color.getTextColor(colorIntensity));
    }

    /**
     * Initializes the summary view so it is update when steps are taken in the trace.
     * Also changes the color and cursor when mouse enters and exits the summary view.
     */
    private void initializeSummaryView() {
        controller.getNumberOfStepsProperty().addListener(
                (observable, oldValue, newValue) -> updateSummaryTitle(newValue.intValue()));

        final Color color = Color.GREY_BLUE;
        final Color.Intensity colorIntensity = Color.Intensity.I50;

        final BiConsumer<Color, Color.Intensity> setBackground = (newColor, newIntensity) -> {
            controller.traceSummary.setBackground(new Background(new BackgroundFill(
                    newColor.getColor(newIntensity),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));

            controller.traceSummary.setBorder(new Border(new BorderStroke(
                    newColor.getColor(newIntensity.next(2)),
                    BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY,
                    new BorderWidths(0, 0, 1, 0)
            )));
        };

        // Update the background when hovered
        controller.traceSummary.setOnMouseEntered(event -> {
            setBackground.accept(color, colorIntensity.next());
            setCursor(Cursor.HAND);
        });

        // Update the background when the mouse exits
        controller.traceSummary.setOnMouseExited(event -> {
            setBackground.accept(color, colorIntensity);
            setCursor(Cursor.DEFAULT);
        });

        // Update the background initially
        setBackground.accept(color, colorIntensity);
    }

    /**
     * Updates the text of the summary title label with the current number of steps in the trace
     * @param steps The number of steps in the trace
     */
    private void updateSummaryTitle(int steps) {
        controller.summaryTitleLabel.setText(steps + " number of steps in trace");
    }

    public TracePaneElementController getController() {
        return controller;
    }
}
