package ecdar.controllers;

import com.jfoenix.controls.JFXRippler;
import ecdar.Ecdar;
import ecdar.abstractions.Location;
import ecdar.backend.SimulationHandler;
import ecdar.simulation.SimulationState;
import ecdar.presentations.TransitionPresentation;
import ecdar.utility.colors.Color;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
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
    private final SimpleIntegerProperty numberOfSteps = new SimpleIntegerProperty(0);
    private final Map<SimulationState, TransitionPresentation> transitionPresentationMap = new LinkedHashMap<>();

    private SimulationHandler simulationHandler;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        simulationHandler = SimulatorController.getSimulationHandler();

        initializeToolbar();
        initializeSummaryView();
        initializeTraceExpand();

        simulationHandler.getTraceLog().addListener((ListChangeListener<SimulationState>) c -> {
            while (c.next()) {
                for (final SimulationState state : c.getAddedSubList()) {
                    if (state != null) insertTraceState(state, true);
                }

                for (final SimulationState state : c.getRemoved()) {
                    traceList.getChildren().remove(transitionPresentationMap.get(state));
                    transitionPresentationMap.remove(state);
                }
            }

            numberOfSteps.set(transitionPresentationMap.size());
        });
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
        getNumberOfStepsProperty().addListener(
                (observable, oldValue, newValue) -> updateSummaryTitle(newValue.intValue()));

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

    /**
     * Updates the text of the summary title label with the current number of steps in the trace
     * @param steps The number of steps in the trace
     */
    private void updateSummaryTitle(int steps) {
        summaryTitleLabel.setText(steps + " number of steps in trace");
    }

    /**
     * Initializes the expand functionality that allows the user to show or hide the trace.
     * By default, the trace is shown.
     */
    private void initializeTraceExpand() {
        isTraceExpanded.addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                showTrace();
                expandTraceIcon.setIconLiteral("gmi-expand-less");
                expandTraceIcon.setIconSize(24);
            } else {
                hideTrace();
                expandTraceIcon.setIconLiteral("gmi-expand-more");
                expandTraceIcon.setIconSize(24);
            }
        });

        isTraceExpanded.set(true);
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
     * Shows the trace by inserting a {@link TransitionPresentation} for each trace state
     * Also hides the summary view, since it should only be visible when the trace is hidden
     */
    private void showTrace() {
        transitionPresentationMap.forEach((state, presentation) -> {
            insertTraceState(state, false);
        });
        root.getChildren().remove(traceSummary);
    }

    private void previewStep(final SimulationState state) {
        traceList.getChildren().forEach(trace -> trace.setOpacity(1));
        int i = traceList.getChildren().size() - 1;
        while (traceList.getChildren().get(i) != transitionPresentationMap.get(state)) {
            traceList.getChildren().get(i).setOpacity(0.4);
            i--;
        }
        simulationHandler.currentState.set(state);
    }

    /**
     * Instantiates a {@link TransitionPresentation} for a {@link SimulationState} and adds it to the view
     *
     * @param state         The state the should be inserted into the trace log
     * @param shouldAnimate A boolean that indicates whether the trace should fade in when added to the view
     */
    private void insertTraceState(final SimulationState state, final boolean shouldAnimate) {
        final TransitionPresentation transitionPresentation = new TransitionPresentation();
        transitionPresentationMap.put(state, transitionPresentation);

        transitionPresentation.setOnMouseReleased(event -> {
            event.consume();
            if (simulationHandler == null) return;
            previewStep(state);
        });

        EventHandler mouseEntered = transitionPresentation.getOnMouseEntered();
        transitionPresentation.setOnMouseEntered(event -> {
            SimulatorController.setSelectedState(state);
            mouseEntered.handle(event);
        });

        EventHandler mouseExited = transitionPresentation.getOnMouseExited();
        transitionPresentation.setOnMouseExited(event -> {
            SimulatorController.setSelectedState(null);
            mouseExited.handle(event);
        });

        String title = traceString(state);
        transitionPresentation.getController().setTitle(title);

        // Only insert the presentation into the view if the trace is expanded & state is not null
        if (isTraceExpanded.get() && state != null) {
            traceList.getChildren().add(transitionPresentation);
            if (shouldAnimate) {
                transitionPresentation.playFadeAnimation();
            }
        }
    }

    /**
     * A helper method that returns a string representing a state in the trace log
     *
     * @param state The SimulationState to represent
     * @return A string representing the state
     */
    private String traceString(SimulationState state) {
        StringBuilder title = new StringBuilder("(");
        int length = state.getLocations().size();
        for (int i = 0; i < length; i++) {
            Location loc = Ecdar.getProject()
                    .findComponent(state.getLocations().get(i).getKey())
                    .findLocation(state.getLocations().get(i).getValue());
            String locationName = loc.getId();
            if (i == length - 1) {
                title.append(locationName);
            } else  {
                title.append(locationName).append(", ");
            }
        }
        title.append(")\n");

        StringBuilder clocks = new StringBuilder();
        for (var constraint : state.getState().getFederation().getDisjunction().getConjunctions(0).getConstraintsList()) {
            var x = constraint.getX().getClockName();
            var y = constraint.getY().getClockName();
            var c = constraint.getC();
            var strict = constraint.getStrict();
            clocks.append(x).append(" - ").append(y).append(strict ? " < " : " <= ").append(c).append("\n");
        }
        return title.toString() + clocks.toString();
    }

    /**
     * Method to be called when clicking on the expand rippler in the trace toolbar
     */
    @FXML
    private void expandTrace() {
        if (isTraceExpanded.get()) {
            isTraceExpanded.set(false);
        } else {
            isTraceExpanded.set(true);
        }
    }

    public SimpleIntegerProperty getNumberOfStepsProperty() {
        return numberOfSteps;
    }
}
