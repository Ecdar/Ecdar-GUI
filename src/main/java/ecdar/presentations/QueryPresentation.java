package ecdar.presentations;

import com.jfoenix.controls.*;
import ecdar.Ecdar;
import ecdar.abstractions.Query;
import ecdar.abstractions.QueryState;
import ecdar.backend.BackendDriverManager;
import ecdar.backend.BackendHelper;
import ecdar.backend.IBackendDriver;
import ecdar.backend.ReveaalDriver;
import ecdar.controllers.CanvasController;
import ecdar.controllers.EcdarController;
import ecdar.utility.colors.Color;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.util.Pair;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static javafx.scene.paint.Color.*;

public class QueryPresentation extends AnchorPane {

    private final Query query;
    private final Tooltip tooltip = new Tooltip();
    private JFXRippler actionButton;
    private Tooltip swapBackendButtonTooltip;
    private Label currentBackendLabel;

    public QueryPresentation(final Query query) {
        new EcdarFXMLLoader().loadAndGetController("QueryPresentation.fxml", this);

        this.query = query;

        initializeStateIndicator();
        initializeProgressIndicator();
        initializeActionButton();
        initializeDetailsButton();
        initializeTextFields();
        initializeInputOutputPaneAndAddIgnoredInputOutputs();
        initializeSwapBackendButton();
    }

    private void initializeTextFields() {
        Platform.runLater(() -> {
            final JFXTextField queryTextField = (JFXTextField) lookup("#query");
            final JFXTextField commentTextField = (JFXTextField) lookup("#comment");

            queryTextField.setText(query.getQuery());
            commentTextField.setText(query.getComment());

            query.queryProperty().bind(queryTextField.textProperty());
            query.commentProperty().bind(commentTextField.textProperty());


            queryTextField.setOnKeyPressed(EcdarController.getActiveCanvasPresentation().getController().getLeaveTextAreaKeyHandler(keyEvent -> {
                if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                    runQuery();
                }
            }));

            commentTextField.setOnKeyPressed(EcdarController.getActiveCanvasPresentation().getController().getLeaveTextAreaKeyHandler());
        });
    }

    private void initializeDetailsButton() {
        Platform.runLater(() -> {
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
                dropDownMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.RIGHT, -24, 20);
            });
        });
    }

    private void initializeActionButton() {
        Platform.runLater(() -> {
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
        });
    }

    private void initializeProgressIndicator() {
        Platform.runLater(() -> {
            // Find the progress indicator
            final JFXSpinner progressIndicator = (JFXSpinner) lookup("#progressIndicator");

            // If the query is running show the indicator, otherwise hide it
            progressIndicator.visibleProperty().bind(new When(query.queryStateProperty().isEqualTo(QueryState.RUNNING)).then(true).otherwise(false));
        });
    }

    private void initializeStateIndicator() {
        Platform.runLater(() -> {
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
        });
    }

    private void initializeInputOutputPaneAndAddIgnoredInputOutputs() {
        Platform.runLater(() -> {
            final TitledPane inputOutputPane = (TitledPane) lookup("#inputOutputPane");
            inputOutputPane.setAnimated(true);

            final Consumer<String> changeTitledPaneVisibility = (queryString) -> updateTitlePaneVisibility(inputOutputPane, queryString);

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

            // Change visibility of input/output Pane when backend is changed for the query
            lookup("#swapBackendButton").setOnMousePressed(event -> changeTitledPaneVisibility.accept(query.getQuery()));

            Platform.runLater(() -> addIgnoredInputOutputsFromQuery(inputOutputPane));
        });
    }

    private void initiateResetInputOutputButton(TitledPane inputOutputPane, ReveaalDriver backendDriver) {
        Platform.runLater(() -> {
            final JFXRippler resetInputOutputPaneButton = (JFXRippler) inputOutputPane.lookup("#inputOutputPaneUpdateButton");

            initializeResetInputOutputPaneButton(inputOutputPane, backendDriver, resetInputOutputPaneButton);

            // Get the inputs and outputs automatically, when executing a refinement query
            actionButton.setOnMousePressed(event -> {
                // Update the ignored inputs and outputs without clearing the lists
                updateInputOutputs(inputOutputPane, backendDriver, false);
            });

            // Install tooltip on the reset button
            final Tooltip buttonTooltip = new Tooltip("Refresh inputs and outputs (resets selections)");
            buttonTooltip.setWrapText(true);
            Tooltip.install(resetInputOutputPaneButton, buttonTooltip);
        });
    }

    private void initializeResetInputOutputPaneButton(TitledPane inputOutputPane,
                                                      ReveaalDriver backendDriver,
                                                      JFXRippler resetInputOutputPaneButton) {
        Platform.runLater(() -> {
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
        });
    }

    private void initializeSwapBackendButton() {
        Platform.runLater(() -> {
            final JFXRippler swapBackendButton = (JFXRippler) lookup("#swapBackendButton");
            final TitledPane inputOutputPane = (TitledPane) lookup("#inputOutputPane");
            this.currentBackendLabel = (Label) lookup("#currentBackendLabel");

            swapBackendButton.setCursor(Cursor.HAND);
            swapBackendButton.setRipplerFill(Color.GREY.getColor(Color.Intensity.I500));
            swapBackendButton.setMaskType(JFXRippler.RipplerMask.CIRCLE);
            swapBackendButton.setOnMousePressed(event -> {
                // Set the backend to the one not currently used and update GUI
                final BackendHelper.BackendNames newBackend;
                if (this.query.getCurrentBackend().equals(BackendHelper.BackendNames.jEcdar)) {
                    newBackend = BackendHelper.BackendNames.Reveaal;
                } else {
                    newBackend = BackendHelper.BackendNames.jEcdar;
                }

                this.query.setCurrentBackend(newBackend);
                setSwapBackendTooltipAndLabel(newBackend);
                updateTitlePaneVisibility(inputOutputPane, query.getQuery());
            });

            swapBackendButtonTooltip = new Tooltip();
            setSwapBackendTooltipAndLabel(this.query.getCurrentBackend());
            JFXTooltip.install(swapBackendButton, swapBackendButtonTooltip);
        });
    }

    private void updateTitlePaneVisibility(TitledPane inputOutputPane, String queryString) {
        final IBackendDriver backendDriver;

        // Check if the query is a refinement and that the engine is set to Reveaal
        if (queryString.startsWith("refinement") && (backendDriver = BackendDriverManager.getInstance(this.query.getCurrentBackend())) instanceof ReveaalDriver) {
            initiateResetInputOutputButton(inputOutputPane, (ReveaalDriver) backendDriver);

            // Make the input/output pane visible
            inputOutputPaneVisibility(true);
        } else {
            // Hide the input/output pane
            inputOutputPaneVisibility(false);
        }
    }

    private void updateInputOutputs(TitledPane inputOutputPane, ReveaalDriver backendDriver, Boolean shouldResetSelections) {
        final VBox inputBox = (VBox) inputOutputPane.lookup("#inputBox");
        final VBox outputBox = (VBox) inputOutputPane.lookup("#outputBox");

        // Get inputs and outputs
        Pair<ArrayList<String>, ArrayList<String>> inputOutputs = backendDriver.getInputOutputs(query.getQuery());

        if (shouldResetSelections) {
            // Reset selections for ignored inputs and outputs
            clearIgnoredInputsAndOutputs(inputBox, outputBox);
        }

        addNewElementsToMap(inputOutputs.getKey(), query.ignoredInputs, inputBox);
        addNewElementsToMap(inputOutputs.getValue(), query.ignoredOutputs, outputBox);
    }

    private void clearIgnoredInputsAndOutputs(VBox inputBox, VBox outputBox) {
        query.ignoredInputs.clear();
        query.ignoredOutputs.clear();

        Platform.runLater(() -> {
            inputBox.getChildren().clear();
            outputBox.getChildren().clear();
        });
    }

    private void addNewElementsToMap(ArrayList<String> keys, HashMap<String, Boolean> associatedMap, VBox associatedVBox) {
        // Add inputs to list and as checkboxes in UI
        for (String key : keys) {
            if (!associatedMap.containsKey(key)) {
                addInputOrOutput(key, false, associatedMap, associatedVBox);
                associatedMap.put(key, false);
            }
        }
    }

    private void addInputOrOutput(String name, Boolean state, Map<String, Boolean> associatedMap, VBox associatedBox) {
        HBox sliderBox = new HBox();
        sliderBox.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label(name);
        label.setWrapText(true);

        // Initialize the toggle slider
        JFXToggleButton slider = new JFXToggleButton();
        slider.setStyle("-jfx-toggle-color:#dddddd; -jfx-untoggle-color:#dddddd; -jfx-toggle-line-color:#" + Color.YELLOW.getColor(Color.Intensity.I700).toString().substring(2, 8) + ";-jfx-untoggle-line-color:#" + Color.GREY.getColor(Color.Intensity.I400).toString().substring(2, 8) + "; -fx-padding: 0 0 0 0;");
        slider.setSelected(state);

        // Add label beneath toggle slider to display state
        Label stateLabel = new Label();
        stateLabel.setText(state ? "Ignored" : "Included");
        stateLabel.setTextAlignment(TextAlignment.CENTER);

        // Enforce changes of the slider
        slider.setOnMouseClicked((event) -> {
            associatedMap.replace(name, slider.isSelected());
            stateLabel.setText(slider.isSelected() ? "Ignored" : "Included");
        });

        // Add toggle slider and state label to VBox and set its width
        VBox sliderAndStateLabel = new VBox();
        sliderAndStateLabel.setMinWidth(64);
        sliderAndStateLabel.setMaxWidth(64);
        sliderAndStateLabel.setSpacing(-7.5);
        sliderAndStateLabel.getChildren().addAll(slider, stateLabel);

        // Horizontal space to ensure that the toggle slider and input/output label is not intertwined
        Region horizontalSpace = new Region();
        horizontalSpace.setMinWidth(16);
        horizontalSpace.setMaxWidth(16);

        sliderBox.getChildren().addAll(sliderAndStateLabel, horizontalSpace, label);

        Platform.runLater(() -> {
            // Add checkbox to the scene
            associatedBox.getChildren().add(sliderBox);
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

    private void addIgnoredInputOutputsFromQuery(TitledPane inputOutputPane) {
        final VBox inputBox = (VBox) inputOutputPane.lookup("#inputBox");
        final VBox outputBox = (VBox) inputOutputPane.lookup("#outputBox");

        // Add inputs as toggles in the GUI
        for (Map.Entry<String, Boolean> entry : query.ignoredInputs.entrySet()) {
            addInputOrOutput(entry.getKey(), entry.getValue(), query.ignoredInputs, inputBox);
        }

        // Add outputs as toggles in the GUI
        for (Map.Entry<String, Boolean> entry : query.ignoredOutputs.entrySet()) {
            addInputOrOutput(entry.getKey(), entry.getValue(), query.ignoredOutputs, outputBox);
        }
    }

    private void setSwapBackendTooltipAndLabel(BackendHelper.BackendNames backend) {
        boolean isReveaal;
        if(backend == null){
            isReveaal = false;
        } else {
            isReveaal = backend.equals(BackendHelper.BackendNames.Reveaal);
        }

        swapBackendButtonTooltip.setText("Switch to the " + (isReveaal ? "jEcdar" : "Reveaal") + " backend");
        currentBackendLabel.setText((isReveaal ? "Reveaal" : "jEcdar"));
    }

    private void runQuery() {
        query.run();
    }
}
