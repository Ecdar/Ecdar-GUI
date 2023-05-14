package ecdar.controllers;

import EcdarProtoBuf.ObjectProtos;
import com.jfoenix.controls.JFXRippler;
import ecdar.abstractions.State;
import ecdar.presentations.StatePresentation;
import ecdar.utility.colors.Color;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;

/**
 * The controller class for the trace pane element that can be inserted into a simulator pane
 */
public class TracePaneController implements Initializable {
    public VBox root;
    public HBox toolbar;
    public Label traceTitle;
    public JFXRippler expandTrace;
    public VBox traceList;
    public FontIcon expandTraceIcon;
    public AnchorPane traceSummary;
    public Label summaryTitleLabel;
    public Label summarySubtitleLabel;

    private final SimpleBooleanProperty isTraceExpanded = new SimpleBooleanProperty(false);
    private ObservableList<StatePresentation> traceLog;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeToolbar();
        initializeSummaryView();
    }

    /**
     * Initializes the toolbar that contains the trace pane element's title and buttons
     * Sets the color of the bar and title label. Also sets the look of the rippler effect
     */
    private void initializeToolbar() {
        final Color color = Color.GREY_BLUE;
        final Color.Intensity colorIntensity = Color.Intensity.I800;

        toolbar.setBackground(new Background(new BackgroundFill(
                color.getColor(colorIntensity),
                CornerRadii.EMPTY,
                Insets.EMPTY)));
        traceTitle.setTextFill(color.getTextColor(colorIntensity));

        expandTrace.setMaskType(JFXRippler.RipplerMask.CIRCLE);
        expandTrace.setRipplerFill(color.getTextColor(colorIntensity));
    }

    /**
     * Initializes the summary view to be updated when steps are taken in the trace.
     * Also changes the color and cursor when mouse enters and exits the summary view.
     */
    private void initializeSummaryView() {
        final Color color = Color.GREY_BLUE;
        final Color.Intensity colorIntensity = Color.Intensity.I50;

        final BiConsumer<Color, Color.Intensity> setBackground = (newColor, newIntensity) -> {
            traceSummary.setBackground(new Background(new BackgroundFill(
                    newColor.getColor(newIntensity),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));

            traceSummary.setBorder(new Border(new BorderStroke(
                    newColor.getColor(newIntensity.next(2)),
                    BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY,
                    new BorderWidths(0, 0, 1, 0)
            )));
        };

        // Update the background when hovered
        traceSummary.setOnMouseEntered(event -> {
            setBackground.accept(color, colorIntensity.next());
            root.setCursor(Cursor.HAND);
        });

        // Update the background when the mouse exits
        traceSummary.setOnMouseExited(event -> {
            setBackground.accept(color, colorIntensity);
            root.setCursor(Cursor.DEFAULT);
        });

        // Update the background initially
        setBackground.accept(color, colorIntensity);
    }

    protected void setTraceLog(ObservableList<StatePresentation> traceLog) {
        this.traceLog = traceLog;

        this.traceLog.addListener((ListChangeListener<StatePresentation>) c -> {
            updateSummaryTitle(traceLog.size());

            if (!isTraceExpanded.get()) return;
            while (c.next()) {
                c.getRemoved().forEach(statePresentation -> traceList.getChildren().remove(statePresentation));
                c.getAddedSubList().forEach(this::insertStateInTrace);
            }
        });

        isTraceExpanded.addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                Platform.runLater(this::showTrace);
                expandTraceIcon.setIconLiteral("gmi-expand-less");
                expandTraceIcon.setIconSize(24);
            } else {
                Platform.runLater(this::hideTrace);
                expandTraceIcon.setIconLiteral("gmi-expand-more");
                expandTraceIcon.setIconSize(24);
            }
        });
    }

    /**
     * Updates the text of the summary title label with the current number of steps in the trace
     * @param steps The number of steps in the trace
     */
    private void updateSummaryTitle(int steps) {
        summaryTitleLabel.setText(steps + " number of steps in trace");
    }

    /**
     * Removes all the trace view elements as to hide the trace from the user
     * Also shows the summary view when the trace is hidden
     */
    private void hideTrace() {
        traceList.getChildren().clear();
        root.getChildren().add(traceSummary);
    }

    /**
     * Shows the trace by inserting a {@link StatePresentation} for each trace state
     * Also hides the summary view, since it should only be visible when the trace is hidden
     */
    private void showTrace() {
        root.getChildren().remove(traceSummary);
        this.traceLog.forEach(this::insertStateInTrace);
    }

    /**
     * Instantiates a {@link StatePresentation} for a {@link State} and adds it to the view
     *
     * @param statePresentation The statePresentation the should be inserted into the trace log
     */
    private void insertStateInTrace(final StatePresentation statePresentation) {
        EventHandler mouseEntered = statePresentation.getOnMouseEntered();
        statePresentation.setOnMouseEntered(event -> {
            SimulationController.setSelectedState(statePresentation.getController().getState());
            mouseEntered.handle(event);
        });

        EventHandler mouseExited = statePresentation.getOnMouseExited();
        statePresentation.setOnMouseExited(event -> {
            SimulationController.setSelectedState(null);
            mouseExited.handle(event);
        });

        statePresentation.getController().setLocationsString(getStateLocationsString(statePresentation.getController().getState()));
        statePresentation.getController().setClocksString(getStateClockConstraintsString(statePresentation.getController().getState()));

        // Only insert the presentation into the view if the trace is expanded & statePresentation is not null
        if (isTraceExpanded.get()) {
            traceList.getChildren().add(statePresentation);
        }
    }

    /**
     * A helper method that returns a string representing the locations of a state in the trace log
     *
     * @param state The State to represent
     * @return A string representing the locations
     */
    private String getStateLocationsString(State state) {
        StringBuilder locationsString = new StringBuilder("(");

        var leafLocations = new ArrayList<ObjectProtos.LeafLocation>();
        state.consumeLeafLocations(leafLocations::add);

        int length = leafLocations.size();
        for (int i = 0; i < length; i++) {
            locationsString.append(leafLocations.get(i).getComponentInstance().getComponentName());
            locationsString.append(leafLocations.get(i).getId());

            if (i != length - 1) {
                locationsString.append(", ");
            }
        }
        locationsString.append(")\n");

        return locationsString.toString();
    }

    /**
     * A helper method that returns a string representing the clock constraints of a state in the trace log
     *
     * @param state The State to represent
     * @return A string representing the clock constraints
     */
    private String getStateClockConstraintsString(State state) {
        StringBuilder clocksString = new StringBuilder();
        for (var constraint : state.getProtoState().getZone().getConjunctions(0).getConstraintsList()) {
            var x = constraint.getX().getComponentClock().getClockName();
            var y = constraint.getY().getComponentClock().getClockName();
            var c = constraint.getC();
            var strict = constraint.getStrict();
            clocksString.append(x).append(" - ").append(y).append(strict ? " < " : " <= ").append(c).append("\n");
        }

        return clocksString.toString();
    }

    /**
     * Method to be called when clicking on the expand rippler in the trace toolbar
     */
    @FXML
    private void toggleTraceExpand() {
        isTraceExpanded.set(!isTraceExpanded.get());
    }
}
