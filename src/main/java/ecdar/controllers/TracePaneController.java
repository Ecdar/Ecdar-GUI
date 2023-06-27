package ecdar.controllers;

import com.jfoenix.controls.JFXRippler;
import com.jfoenix.controls.JFXTooltip;
import ecdar.abstractions.State;
import ecdar.presentations.StatePresentation;
import ecdar.utility.colors.Color;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
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
    public JFXRippler restartSimulation;
    public VBox traceSection;
    public VBox traceList;
    public FontIcon expandTraceIcon;
    public AnchorPane traceSummary;
    public Label summaryTitleLabel;
    public Label summarySubtitleLabel;

    private final SimpleBooleanProperty isTraceExpanded = new SimpleBooleanProperty(false);
    private ObservableList<StatePresentation> traceLog;

    /**
     * Keep reference to the traceLogListener, such that it can be removed and added to future logs
     */
    private final ListChangeListener<StatePresentation> traceLogListener = c -> {
        updateSummaryTitle(traceLog.size());

        if (!isTraceExpanded.get()) return;
        while (c.next()) {
            c.getRemoved().forEach(statePresentation -> traceList.getChildren().remove(statePresentation));
            c.getAddedSubList().forEach(this::insertStateInTrace);
        }
    };


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeToolbar();
        initializeSummaryView();

        isTraceExpanded.addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                Platform.runLater(this::showTrace);
                expandTraceIcon.setIconLiteral("gmi-expand-less");
            } else {
                Platform.runLater(this::hideTrace);
                expandTraceIcon.setIconLiteral("gmi-expand-more");
            }

            traceSection.setManaged(newVal);
        });

        Platform.runLater(this::toggleTraceExpand);
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

        JFXTooltip.install(restartSimulation, new JFXTooltip("Restart Simulation"));
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
        if (this.traceLog != null) {
            // Remove all information from previous simulation run
            this.traceLog.removeListener(traceLogListener);
            this.traceLog.clear();
        };

        this.traceList.getChildren().clear();
        this.traceLog = traceLog;

        // Add listener to new log
        this.traceLog.addListener(traceLogListener);
    }

    /**
     * Updates the text of the summary title label with the current number of steps in the trace
     *
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
        // After initialization, the log will be null
        if (this.traceLog != null) this.traceLog.forEach(this::insertStateInTrace);
    }

    /**
     * Instantiates a {@link StatePresentation} for a {@link State} and adds it to the view
     *
     * @param statePresentation The statePresentation to insert into the trace log
     */
    private void insertStateInTrace(final StatePresentation statePresentation) {
        // Install mouse event listeners on the state
        EventHandler<? super MouseEvent> mouseEntered = statePresentation.getOnMouseEntered();
        statePresentation.setOnMouseEntered(event -> {
            SimulationController.setSelectedState(statePresentation.getController().getState());
            mouseEntered.handle(event);
        });

        EventHandler<? super MouseEvent> mouseExited = statePresentation.getOnMouseExited();
        statePresentation.setOnMouseExited(event -> {
            SimulationController.setSelectedState(null);
            mouseExited.handle(event);
        });

        // Only insert the presentation into the view if the trace is expanded
        if (isTraceExpanded.get()) {
            traceList.getChildren().add(0, statePresentation);
        }
    }

    /**
     * Method to be called when clicking on the expand rippler in the trace toolbar
     */
    @FXML
    private void toggleTraceExpand() {
        isTraceExpanded.set(!isTraceExpanded.get());
    }
}
