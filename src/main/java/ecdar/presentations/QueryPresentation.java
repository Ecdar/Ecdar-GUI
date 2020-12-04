package ecdar.presentations;

import com.jfoenix.controls.*;
import ecdar.Ecdar;
import ecdar.abstractions.Query;
import ecdar.abstractions.QueryState;
import ecdar.backend.BackendDriverManager;
import ecdar.backend.IBackendDriver;
import ecdar.backend.ReveaalDriver;
import ecdar.controllers.CanvasController;
import ecdar.utility.colors.Color;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.When;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.util.Pair;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static javafx.scene.paint.Color.*;

public class QueryPresentation extends AnchorPane {

    private final Query query;
    private final Map<String, Boolean> ignoredInputs = new HashMap<>();
    private final Map<String, Boolean> ignoredOutputs = new HashMap<>();
    private final Tooltip tooltip = new Tooltip();
    private JFXRippler actionButton;

    public QueryPresentation(final Query query) {
        new EcdarFXMLLoader().loadAndGetController("QueryPresentation.fxml", this);

        this.query = query;

        initializeStateIndicator();
        initializeProgressIndicator();
        initializeActionButton();
        initializeDetailsButton();
        initializeTextFields();
        initializeInputOutputPane();
    }

    private void initializeTextFields() {
        final JFXTextField queryTextField = (JFXTextField) lookup("#query");
        final JFXTextField commentTextField = (JFXTextField) lookup("#comment");

        queryTextField.setText(query.getQuery());
        commentTextField.setText(query.getComment());

        query.queryProperty().bind(queryTextField.textProperty());
        query.commentProperty().bind(commentTextField.textProperty());


        queryTextField.setOnKeyPressed(CanvasController.getLeaveTextAreaKeyHandler(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                runQuery();
            }
        }));

        commentTextField.setOnKeyPressed(CanvasController.getLeaveTextAreaKeyHandler());
    }

    private void initializeDetailsButton() {
        final JFXRippler detailsButton = (JFXRippler) lookup("#detailsButton");
        final FontIcon detailsButtonIcon = (FontIcon) lookup("#detailsButtonIcon");

        detailsButtonIcon.setIconColor(Color.GREY.getColor(Color.Intensity.I900));

        detailsButton.setCursor(Cursor.HAND);
        detailsButton.setRipplerFill(Color.GREY.getColor(Color.Intensity.I500));
        detailsButton.setMaskType(JFXRippler.RipplerMask.CIRCLE);

        final DropDownMenu dropDownMenu = new DropDownMenu(detailsButton);

        dropDownMenu.addToggleableListElement("Run Periodically", query.isPeriodicProperty(), event -> {
            // Toggle the property
            query.setIsPeriodic(!query.isPeriodic());
            dropDownMenu.hide();
        });

        dropDownMenu.addSpacerElement();

        dropDownMenu.addClickableListElement("Clear Status", event -> {
            // Clear the state
            query.setQueryState(QueryState.UNKNOWN);
            dropDownMenu.hide();
        });

        dropDownMenu.addSpacerElement();

        dropDownMenu.addClickableListElement("Delete", event -> {
            // Remove the query
            Ecdar.getProject().getQueries().remove(query);
            dropDownMenu.hide();
        });

        detailsButton.getChildren().get(0).setOnMousePressed(event -> {
            // Show the popup
            dropDownMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.RIGHT, -15, 10);
        });
    }

    private void initializeActionButton() {
        // Find the action icon
        actionButton = (JFXRippler) lookup("#actionButton");
        final FontIcon actionButtonIcon = (FontIcon) lookup("#actionButtonIcon");

        actionButtonIcon.setIconColor(Color.GREY.getColor(Color.Intensity.I900));

        actionButton.setCursor(Cursor.HAND);
        actionButton.setRipplerFill(Color.GREY.getColor(Color.Intensity.I500));

        // Delegate that based on the query state updated the action icon
        final Consumer<QueryState> updateIcon = (queryState) -> {
            if (queryState.equals(QueryState.RUNNING)) {
                actionButtonIcon.setIconLiteral("gmi-stop");
            } else {
                actionButtonIcon.setIconLiteral("gmi-play-arrow");
            }
            actionButtonIcon.setIconSize(24);
        };

        // Update the icon initially
        updateIcon.accept(query.getQueryState());

        // Update the icon when ever the query state is updated
        query.queryStateProperty().addListener((observable, oldValue, newValue) -> updateIcon.accept(newValue));

        actionButton.setMaskType(JFXRippler.RipplerMask.CIRCLE);

        actionButton.getChildren().get(0).setOnMousePressed(event -> {
            if (query.getQueryState().equals(QueryState.RUNNING)) {
                query.cancel();
            } else {
                runQuery();
            }
        });
    }

    private void initializeProgressIndicator() {
        // Find the progress indicator
        final JFXSpinner progressIndicator = (JFXSpinner) lookup("#progressIndicator");

        // If the query is running show the indicator, otherwise hide it
        progressIndicator.visibleProperty().bind(new When(query.queryStateProperty().isEqualTo(QueryState.RUNNING)).then(true).otherwise(false));
    }

    private void initializeStateIndicator() {
        // Find the state indicator from the inflated xml
        final StackPane stateIndicator = (StackPane) lookup("#stateIndicator");
        final FontIcon statusIcon = (FontIcon) stateIndicator.lookup("#statusIcon");

        // Delegate that based on a query state updates tooltip of the query
        final Consumer<QueryState> updateToolTip = (queryState) -> {
            if (queryState.getStatusCode() == 1) {
                this.tooltip.setText("This query was a success!");
            } else if (queryState.getStatusCode() == 3) {
                this.tooltip.setText("The query has not been executed yet");
            } else {
                this.tooltip.setText(query.getCurrentErrors());
            }
        };

        // Delegate that based on a query state updates the color of the state indicator
        final Consumer<QueryState> updateStateIndicator = (queryState) -> {
            this.tooltip.setText("");

            final Color color = queryState.getColor();
            final Color.Intensity colorIntensity = queryState.getColorIntensity();

            if (queryState.equals(QueryState.UNKNOWN) || queryState.equals(QueryState.RUNNING)) {
                stateIndicator.setBackground(new Background(new BackgroundFill(TRANSPARENT,
                        CornerRadii.EMPTY,
                        Insets.EMPTY)
                ));
            } else {
                stateIndicator.setBackground(new Background(new BackgroundFill(color.getColor(colorIntensity),
                        CornerRadii.EMPTY,
                        Insets.EMPTY)
                ));
            }

            statusIcon.setIconColor(new javafx.scene.paint.Color(1, 1, 1, 1));
            statusIcon.setIconLiteral("gmi-" + queryState.getIconCode().toString().toLowerCase().replace('_', '-'));

            if (queryState.equals(QueryState.RUNNING) || queryState.equals(QueryState.UNKNOWN)) {
                statusIcon.setIconColor(new javafx.scene.paint.Color(0.75, 0.75, 0.75, 1));
            }

            // The tooltip is updated here to handle all cases that are not syntax error
            updateToolTip.accept(queryState);
        };

        // Update the initial color
        updateStateIndicator.accept(query.getQueryState());

        // Ensure that the color is updated when ever the query state is updated
        query.queryStateProperty().addListener((observable, oldValue, newValue) -> updateStateIndicator.accept(newValue));

        // Ensure that the tooltip is updated when new errors are added
        query.errors().addListener((observable, oldValue, newValue) -> updateToolTip.accept(query.getQueryState()));

        Tooltip.install(stateIndicator, this.tooltip);
    }

    private void initializeInputOutputPane() {
        final TitledPane inputOutputPane = (TitledPane) lookup("#inputOutputPane");
        inputOutputPane.setAnimated(true);

        final Consumer<String> changeTitledPaneVisibility = (query) -> {
            final IBackendDriver backendDriver;

            // Check if the query is a refinement and that the engine is set to Reveaal
            if (query.startsWith("refinement") && (backendDriver = BackendDriverManager.getInstance()) instanceof ReveaalDriver) {
                initiateResetInputOutputButton(inputOutputPane, (ReveaalDriver) backendDriver);

                // Make the input/output pane visible
                inputOutputPaneVisibility(true);
            } else {
                // Hide the input/output pane
                inputOutputPaneVisibility(false);
            }
        };

        // Run the consumer to ensure that the input/output pane is displayed for existing refinement queries
        changeTitledPaneVisibility.accept(query.getQuery());

        // Bind the expand icon to the expand property of the pane
        inputOutputPane.expandedProperty().addListener((observable, oldValue, newValue) -> {
            FontIcon expandIcon = (FontIcon) inputOutputPane.lookup("#inputOutputPaneExpandIcon");
            if (!newValue) {
                expandIcon.setIconLiteral("gmi-keyboard-arrow-down");
            } else {
                expandIcon.setIconLiteral("gmi-keyboard-arrow-up");
            }
        });

        // Make sure the input/output pane state is updated whenever the query text field loses focus
        final JFXTextField queryTextField = (JFXTextField) lookup("#query");
        queryTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                changeTitledPaneVisibility.accept(query.getQuery());
            }
        });

        // Make sure the input/output pane state is updated whenever the backend is changed
        BackendDriverManager.backendSupportsInputOutputs().addListener((observable, oldValue, newValue) -> {
            changeTitledPaneVisibility.accept(query.getQuery());
        });
    }

    private void inputOutputPaneVisibility(Boolean visibility) {
        Platform.runLater(() -> {
            final TitledPane inputOutputPane = (TitledPane) lookup("#inputOutputPane");

            // Hide/show the inputOutputPane and remove/add the space it would occupy respectively
            inputOutputPane.setVisible(visibility);
            inputOutputPane.setManaged(visibility);

            // Set expand property only on visibility false to avoid auto expand
            if (!visibility) {
                inputOutputPane.setExpanded(false);
            }
        });
    }

    private void initiateResetInputOutputButton(TitledPane inputOutputPane, ReveaalDriver backendDriver) {
        Platform.runLater(() -> {
            final JFXRippler resetInputOutputPaneButton = (JFXRippler) inputOutputPane.lookup("#inputOutputPaneUpdateButton");
            final FontIcon resetInputOutputPaneButtonIcon = (FontIcon) lookup("#inputOutputPaneUpdateButtonIcon");
            final JFXSpinner progressIndicator = (JFXSpinner) lookup("#inputOutputProgressIndicator");
            progressIndicator.setVisible(false);

            // Set the initial state of the reset button
            resetInputOutputPaneButton.setCursor(Cursor.HAND);
            resetInputOutputPaneButton.setRipplerFill(Color.GREY.getColor(Color.Intensity.I500));
            resetInputOutputPaneButton.setMaskType(JFXRippler.RipplerMask.CIRCLE);
            resetInputOutputPaneButtonIcon.setIconColor(Color.GREY.getColor(Color.Intensity.I900));

            resetInputOutputPaneButton.setOnMousePressed(event -> {
                // Disable the button on click
                progressIndicator.setVisible(true);
                resetInputOutputPaneButton.setDisable(true);
                resetInputOutputPaneButtonIcon.setIconColor(Color.GREY.getColor(Color.Intensity.I700));

                updateInputOutputs(inputOutputPane, backendDriver, true);

                // Enable the button after inputs and outputs have been updated
                progressIndicator.setVisible(false);
                resetInputOutputPaneButton.setDisable(false);
                resetInputOutputPaneButtonIcon.setIconColor(Color.GREY.getColor(Color.Intensity.I900));
            });

            // Get the inputs and outputs automatically, when executing a refinement query
            actionButton.setOnMousePressed(event -> {
                // Update the ignored inputs and outputs without clearing the lists
                updateInputOutputs(inputOutputPane, backendDriver, false);
            });

            // Install tooltip on refresh button
            final Tooltip buttonTooltip = new Tooltip("Refresh inputs and outputs (resets selections)");
            buttonTooltip.setWrapText(true);
            Tooltip.install(resetInputOutputPaneButton, buttonTooltip);
        });
    }

    private void updateInputOutputs(TitledPane inputOutputPane, ReveaalDriver backendDriver, Boolean shouldResetSelections) {
        final VBox inputBox = (VBox) inputOutputPane.lookup("#inputBox");
        final VBox outputBox = (VBox) inputOutputPane.lookup("#outputBox");

        // Get inputs and outputs
        Pair<ArrayList<String>, ArrayList<String>> inputOutputs = backendDriver.getInputOutputs(query.getQuery());

        // Reset selections for ignored inputs and outputs
        if (shouldResetSelections) {
            clearIgnoredInputsAndOutputs(inputBox, outputBox);
        }

        // Add inputs to list and as checkboxes in UI
        for (String input : inputOutputs.getKey()) {
            if (!ignoredInputs.containsKey(input)){
                addInputOrOutput(inputBox, input, ignoredInputs);
            }
        }

        // Add outputs to list and as checkboxes in UI
        for (String output : inputOutputs.getValue()) {
            if (!ignoredOutputs.containsKey(output)){
                addInputOrOutput(outputBox, output, ignoredOutputs);
            }
        }
    }

    private void clearIgnoredInputsAndOutputs(VBox inputBox, VBox outputBox) {
        ignoredInputs.clear();
        ignoredOutputs.clear();

        Platform.runLater(() -> {
            inputBox.getChildren().clear();
            outputBox.getChildren().clear();
        });
    }

    private void addInputOrOutput(VBox associatedBox, String name, Map<String, Boolean> associatedList) {
        HBox sliderBox = new HBox();
        sliderBox.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(name);
        label.setWrapText(true);

        JFXToggleButton slider = new JFXToggleButton();
        slider.setStyle("-jfx-toggle-color:#dddddd; -jfx-untoggle-color:#dddddd; -jfx-toggle-line-color:#" + Color.YELLOW.getColor(Color.Intensity.I700).toString().substring(2, 8) + ";-jfx-untoggle-line-color:#" + Color.GREY.getColor(Color.Intensity.I400).toString().substring(2, 8) + "; -fx-padding: 0 0 0 0;");
        slider.setSelected(false);
        slider.setOnMouseClicked((event) -> {
            associatedList.replace(name, slider.isSelected());
        });

        sliderBox.getChildren().addAll(slider, label);

        Platform.runLater(() -> {
            // Add checkbox to the scene
            associatedBox.getChildren().add(sliderBox);
        });

        // Add input/output to its respective list
        associatedList.put(name, false);
    }

    private void setIgnoredInputOutputsOnQuery(Boolean shouldRunQueryWithExtraInputOutputs) {
        if (!shouldRunQueryWithExtraInputOutputs) {
            query.setExtraInputOutputs(null);
            return;
        }

        // Create StringBuilder starting with a quotation mark to signal start of extra outputs
        StringBuilder extraInputOutputs = new StringBuilder("--ignored_outputs=\"");

        // Append outputs, comma separated
        appendMapItemsWithValueTrue(extraInputOutputs, ignoredOutputs);

        // Append quotation marks to signal end of outputs and start of inputs
        extraInputOutputs.append("\" --ignored_inputs=\"");

        // Append inputs, comma separated
        appendMapItemsWithValueTrue(extraInputOutputs, ignoredInputs);

        // Append quotation mark to signal end of extra inputs
        extraInputOutputs.append("\"");

        query.setExtraInputOutputs(extraInputOutputs.toString());
    }

    private void appendMapItemsWithValueTrue(StringBuilder stringBuilder, Map<String, Boolean> map) {
        map.forEach((key, value) -> {
            if (value) {
                stringBuilder.append(key);
                stringBuilder.append(",");
            }
        });

        if (stringBuilder.lastIndexOf(",") + 1 == stringBuilder.length()) {
            stringBuilder.setLength(stringBuilder.length() - 1);
        }
    }

    private void runQuery() {
        setIgnoredInputOutputsOnQuery(!ignoredInputs.isEmpty() || !ignoredOutputs.isEmpty());
        query.run();
    }
}
