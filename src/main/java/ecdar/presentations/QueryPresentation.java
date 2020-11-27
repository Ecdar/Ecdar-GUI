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
import javafx.beans.binding.When;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
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
    private final Map<String, Boolean> inputs = new HashMap<>();
    private final Map<String, Boolean> outputs = new HashMap<>();
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
            // Get related FXML nodes
            final IBackendDriver backendDriver;

            // Check if the query is a refinement and that the engine is set to Reveaal
            if (query.startsWith("refinement") && (backendDriver = BackendDriverManager.getInstance()) instanceof ReveaalDriver) {
                initiateRefreshInputOutputButton(inputOutputPane, (ReveaalDriver) backendDriver);

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

        // Make sure the input/output pane is added whenever the query text field loses focus
        final JFXTextField queryTextField = (JFXTextField) lookup("#query");
        queryTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                changeTitledPaneVisibility.accept(query.getQuery());
            }
        });

        // Make sure the input/output pane is updated whenever the backend is changed
        BackendDriverManager.getSupportsInputOutputs().addListener((observable, oldValue, newValue) -> {
            changeTitledPaneVisibility.accept(query.getQuery());
        });
    }

    private void inputOutputPaneVisibility(Boolean visibility) {
        Platform.runLater(() -> {
            final TitledPane inputOutputPane = (TitledPane) lookup("#inputOutputPane");

            // Hide the inputOutputPane and collapse the space it would occupy
            inputOutputPane.setVisible(visibility);
            inputOutputPane.setManaged(visibility);

            // Collapse the pane when visibility is false, but do not expand it when set to visible
            if (!visibility) {
                inputOutputPane.setExpanded(false);
            }
        });
    }

    private void initiateRefreshInputOutputButton(TitledPane inputOutputPane, ReveaalDriver backendDriver) {
        Platform.runLater(() -> {
            final JFXRippler refreshInputOutputPaneButton = (JFXRippler) inputOutputPane.lookup("#inputOutputPaneUpdateButton");
            final FontIcon refreshInputOutputPaneButtonIcon = (FontIcon) lookup("#inputOutputPaneUpdateButtonIcon");

            final JFXSpinner progressIndicator = (JFXSpinner) lookup("#inputOutputProgressIndicator");
            progressIndicator.setVisible(false);

            // Set the initial state of the refresh button
            refreshInputOutputPaneButton.setCursor(Cursor.HAND);
            refreshInputOutputPaneButton.setRipplerFill(Color.GREY.getColor(Color.Intensity.I500));
            refreshInputOutputPaneButton.setMaskType(JFXRippler.RipplerMask.CIRCLE);
            refreshInputOutputPaneButtonIcon.setIconColor(Color.GREY.getColor(Color.Intensity.I900));

            refreshInputOutputPaneButton.setOnMousePressed(event -> {
                // Disable the button on click
                progressIndicator.setVisible(true);
                refreshInputOutputPaneButton.setDisable(true);
                refreshInputOutputPaneButtonIcon.setIconColor(Color.GREY.getColor(Color.Intensity.I700));

                refreshInputOutputs(inputOutputPane, backendDriver);

                // Enable the button after inputs and outputs have been updated
                progressIndicator.setVisible(false);
                refreshInputOutputPaneButton.setDisable(false);
                refreshInputOutputPaneButtonIcon.setIconColor(Color.GREY.getColor(Color.Intensity.I900));
            });

            // Get the inputs and outputs automatically, when executing the query
            actionButton.setOnMousePressed(event -> {
                // Check if both maps are empty, to avoid resetting the selected checkboxes when executing the query
                if(inputs.isEmpty() && outputs.isEmpty()){
                    refreshInputOutputs(inputOutputPane, backendDriver);
                }
            });

            // Install tooltip on refresh button
            final Tooltip buttonTooltip = new Tooltip("Refresh inputs and outputs");
            buttonTooltip.setWrapText(true);
            Tooltip.install(refreshInputOutputPaneButton, buttonTooltip);
        });
    }

    private void refreshInputOutputs(TitledPane inputOutputPane, ReveaalDriver backendDriver) {
        final VBox inputBox = (VBox) inputOutputPane.lookup("#inputBox");
        final VBox outputBox = (VBox) inputOutputPane.lookup("#outputBox");

        //Get inputs and outputs
        Pair<ArrayList<String>, ArrayList<String>> inputOutputs = backendDriver.getInputOutputs(query.getQuery());

        // Remove checkboxes
        Platform.runLater(() -> {
            inputBox.getChildren().clear();
            outputBox.getChildren().clear();
        });

        // Clear input and output lists
        inputs.clear();
        outputs.clear();

        final Consumer<Pair<String, Boolean>> updateInputState = (in) -> {
            inputs.replace(in.getKey(), in.getValue());
        };

        final Consumer<Pair<String, Boolean>> updateOutputState = (out) -> {
            outputs.replace(out.getKey(), out.getValue());
        };

        // Add inputs to list and as checkboxes in UI
        for (String input : inputOutputs.getKey()) {
            addInputOrOutput(updateInputState, inputBox, input, inputs);
        }

        // Add outputs to list and as checkboxes in UI
        for (String output : inputOutputs.getValue()) {
            addInputOrOutput(updateOutputState, outputBox, output, outputs);
        }
    }

    private void addInputOrOutput(Consumer<Pair<String, Boolean>> updateInputOrOutputState, VBox inputOrOutputBox, String inputOrOutput, Map<String, Boolean> inputsOrOutputs) {
        Platform.runLater(() -> {
            JFXCheckBox checkBox = new JFXCheckBox(inputOrOutput);
            checkBox.setSelected(true);
            checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
                updateInputOrOutputState.accept(new Pair<>(inputOrOutput, newValue));
            });
            inputOrOutputBox.getChildren().add(checkBox);
        });
        inputsOrOutputs.put(inputOrOutput, true);
    }

    private void setExtraInputOutputsOnQuery(Boolean shouldRunQueryWithExtraInputOutputs) {
        if (!shouldRunQueryWithExtraInputOutputs) {
            query.setExtraInputOutputs(null);
            return;
        }

        StringBuilder extraInputOutputs = new StringBuilder("\"");

        // Append outputs separated by commas
        appendMapItems(extraInputOutputs, outputs);
        extraInputOutputs.append("\" \"");

        // Append inputs separated by commas
        appendMapItems(extraInputOutputs, inputs);
        extraInputOutputs.append("\"");

        query.setExtraInputOutputs(extraInputOutputs.toString());
    }

    private void appendMapItems(StringBuilder extraInputOutputs, Map<String, Boolean> map) {
        map.forEach((key, value) -> {
            if (value) {
                extraInputOutputs.append(key);
                extraInputOutputs.append(",");
            }
        });

        if (extraInputOutputs.lastIndexOf(",") + 1 == extraInputOutputs.length()) {
            extraInputOutputs.setLength(extraInputOutputs.length() - 1);
        }
    }

    private void runQuery() {
        setExtraInputOutputsOnQuery(!inputs.isEmpty() || !outputs.isEmpty());
        query.run();
    }
}
