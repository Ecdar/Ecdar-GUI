package ecdar.presentations;

import com.jfoenix.controls.*;
import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.backend.*;
import ecdar.controllers.QueryController;
import ecdar.controllers.EcdarController;
import ecdar.utility.colors.Color;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import org.kordamp.ikonli.javafx.FontIcon;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import static javafx.scene.paint.Color.*;

public class QueryPresentation extends AnchorPane {
    private final Tooltip tooltip = new Tooltip();
    private Tooltip backendDropdownTooltip;
    private final QueryController controller;

    public QueryPresentation(final Query query) {
        controller = new EcdarFXMLLoader().loadAndGetController("QueryPresentation.fxml", this);
        controller.setQuery(query);

        initializeStateIndicator();
        initializeProgressIndicator();
        initializeActionButton();
        initializeDetailsButton();
        initializeTextFields();
        initializeInputOutputPaneAndAddIgnoredInputOutputs();
        initializeMoreInformationButtonAndQueryTypeSymbol();
        initializeBackendsDropdown();
    }

    private void initializeBackendsDropdown() {
        controller.backendsDropdown.setItems(BackendHelper.getBackendInstances());
        BackendHelper.addBackendInstanceListener(() -> controller.backendsDropdown.setItems(BackendHelper.getBackendInstances()));

        backendDropdownTooltip = new Tooltip();
        backendDropdownTooltip.setText("Current backend used for the query");
        JFXTooltip.install(controller.backendsDropdown, backendDropdownTooltip);

        controller.backendsDropdown.setValue(BackendHelper.getDefaultBackendInstance());
    }

    private void initializeTextFields() {
        Platform.runLater(() -> {
            final JFXTextField queryTextField = (JFXTextField) lookup("#query");
            final JFXTextField commentTextField = (JFXTextField) lookup("#comment");

            queryTextField.setText(controller.getQuery().getQuery());
            commentTextField.setText(controller.getQuery().getComment());

            controller.getQuery().queryProperty().bind(queryTextField.textProperty());
            controller.getQuery().commentProperty().bind(commentTextField.textProperty());


            queryTextField.setOnKeyPressed(EcdarController.getActiveCanvasPresentation().getController().getLeaveTextAreaKeyHandler(keyEvent -> {
                Platform.runLater(() -> {
                    if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                        runQuery();
                    }
                });
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

            dropDownMenu.addToggleableListElement("Run Periodically", controller.getQuery().isPeriodicProperty(), event -> {
                // Toggle the property
                controller.getQuery().setIsPeriodic(!controller.getQuery().isPeriodic());
                dropDownMenu.hide();
            });
            dropDownMenu.addSpacerElement();
            dropDownMenu.addClickableListElement("Clear Status", event -> {
                // Clear the state
                controller.getQuery().setQueryState(QueryState.UNKNOWN);
                dropDownMenu.hide();
            });
            dropDownMenu.addSpacerElement();
            dropDownMenu.addClickableListElement("Delete", event -> {
                // Remove the query
                Ecdar.getProject().getQueries().remove(controller.getQuery());
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
            final FontIcon actionButtonIcon = (FontIcon) lookup("#actionButtonIcon");

            if (controller.getQuery() == null) {
                actionButtonIcon.setIconColor(Color.GREY.getColor(Color.Intensity.I500));
            }

            controller.actionButton.setCursor(Cursor.HAND);
            controller.actionButton.setRipplerFill(Color.GREY.getColor(Color.Intensity.I500));

            // Delegate that based on the query state updated the action icon
            final Consumer<QueryState> updateIcon = (queryState) -> {
                Platform.runLater(() -> {
                    if (queryState.equals(QueryState.RUNNING)) {
                        actionButtonIcon.setIconLiteral("gmi-stop");
                    } else {
                        actionButtonIcon.setIconLiteral("gmi-play-arrow");
                    }
                });
            };

            // Update the icon initially
            updateIcon.accept(controller.getQuery().getQueryState());

            // Update the icon when ever the query state is updated
            controller.getQuery().queryStateProperty().addListener((observable, oldValue, newValue) -> updateIcon.accept(newValue));

            controller.actionButton.setMaskType(JFXRippler.RipplerMask.CIRCLE);

            controller.actionButton.getChildren().get(0).setOnMousePressed(event -> {
                Platform.runLater(() -> {
                    if (controller.getQuery().getQueryState().equals(QueryState.RUNNING)) {
                        controller.getQuery().cancel();
                    } else {
                        runQuery();
                    }
                });
            });
        });
    }

    private void initializeProgressIndicator() {
        Platform.runLater(() -> {
            // Find the progress indicator
            final JFXSpinner progressIndicator = (JFXSpinner) lookup("#progressIndicator");

            // If the query is running show the indicator, otherwise hide it
            progressIndicator.visibleProperty().bind(new When(controller.getQuery().queryStateProperty().isEqualTo(QueryState.RUNNING)).then(true).otherwise(false));
        });
    }

    private void initializeStateIndicator() {
        Platform.runLater(() -> {
            // Find the state indicator from the inflated xml
            final StackPane stateIndicator = (StackPane) lookup("#stateIndicator");
            final FontIcon statusIcon = (FontIcon) stateIndicator.lookup("#statusIcon");
            final FontIcon queryTypeExpandIcon = (FontIcon) stateIndicator.lookup("#queryTypeExpandIcon");

            // Delegate that based on a query state updates tooltip of the query
            final Consumer<QueryState> updateToolTip = (queryState) -> {
                Platform.runLater(() -> {
                    if (queryState.getStatusCode() == 1) {
                        this.tooltip.setText("This query was successful!");
                    } else if (queryState.getStatusCode() == 3) {
                        this.tooltip.setText("The query has not been executed yet");
                    } else {
                        this.tooltip.setText(controller.getQuery().getCurrentErrors());
                    }
                });
            };

            // Delegate that based on a query state updates the color of the state indicator
            final Consumer<QueryState> updateStateIndicator = (queryState) -> {
                Platform.runLater(() -> {
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

                    setStatusIndicatorContentColor(new javafx.scene.paint.Color(1, 1, 1, 1), statusIcon, queryTypeExpandIcon, queryState);

                    if (queryState.equals(QueryState.RUNNING) || queryState.equals(QueryState.UNKNOWN)) {
                        setStatusIndicatorContentColor(Color.GREY.getColor(Color.Intensity.I700), statusIcon, queryTypeExpandIcon, null);
                    }

                    // The tooltip is updated here to handle all cases that are not syntax error
                    updateToolTip.accept(queryState);
                });
            };

            // Update the initial color
            updateStateIndicator.accept(controller.getQuery().getQueryState());

            // Ensure that the color is updated when ever the query state is updated
            controller.getQuery().queryStateProperty().addListener((observable, oldValue, newValue) -> updateStateIndicator.accept(newValue));

            // Ensure that the tooltip is updated when new errors are added
            controller.getQuery().errors().addListener((observable, oldValue, newValue) -> updateToolTip.accept(controller.getQuery().getQueryState()));
            this.tooltip.setMaxWidth(300);
            this.tooltip.setWrapText(true);

            // Installing the tooltip on the statusIcon itself scales the tooltip unexpectedly, hence its parent StackPane is used
            Tooltip.install(statusIcon.getParent(), this.tooltip);

            controller.queryTypeSymbol.setText(controller.getQuery() != null && controller.getQuery().getType() != null ? controller.getQuery().getType().getSymbol() : "---");
        });
    }

    private void setStatusIndicatorContentColor(javafx.scene.paint.Color color, FontIcon statusIcon, FontIcon queryTypeExpandIcon, QueryState queryState) {
        statusIcon.setIconColor(color);
        controller.queryTypeSymbol.setFill(color);
        queryTypeExpandIcon.setIconColor(color);

        if (queryState != null) {
            statusIcon.setIconLiteral("gmi-" + queryState.getIconCode().toString().toLowerCase().replace('_', '-'));
        }
    }

    private void initializeInputOutputPaneAndAddIgnoredInputOutputs() {
        Platform.runLater(() -> {
            final TitledPane inputOutputPane = (TitledPane) lookup("#inputOutputPane");
            inputOutputPane.setAnimated(true);

            final Runnable changeTitledPaneVisibility = () -> updateTitlePaneVisibility(inputOutputPane);

            // Run the consumer to ensure that the input/output pane is displayed for existing refinement queries
            changeTitledPaneVisibility.run();

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
                    changeTitledPaneVisibility.run();
                }
            });

            // Change visibility of input/output Pane when backend is changed for the query ToDo NIELS
            // lookup("#swapBackendButton").setOnMousePressed(event -> changeTitledPaneVisibility.accept(controller.getQuery().getQuery()));
            Platform.runLater(() -> addIgnoredInputOutputsFromQuery(inputOutputPane));
        });
    }

    private void initiateResetInputOutputButton(TitledPane inputOutputPane) {
        Platform.runLater(() -> {
            final JFXRippler resetInputOutputPaneButton = (JFXRippler) inputOutputPane.lookup("#inputOutputPaneUpdateButton");

            initializeResetInputOutputPaneButton(inputOutputPane, resetInputOutputPaneButton);

            // Get the inputs and outputs automatically, when executing a refinement query
            controller.actionButton.setOnMousePressed(event -> {
                // Update the ignored inputs and outputs without clearing the lists
                updateInputOutputs(inputOutputPane, false);
            });

            // Install tooltip on the reset button
            final Tooltip buttonTooltip = new Tooltip("Refresh inputs and outputs (resets selections)");
            buttonTooltip.setWrapText(true);
            Tooltip.install(resetInputOutputPaneButton, buttonTooltip);
        });
    }

    private void initializeResetInputOutputPaneButton(TitledPane inputOutputPane,
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

                updateInputOutputs(inputOutputPane, true);

                // Enable the button after inputs and outputs have been updated
                progressIndicator.setVisible(false);
                resetInputOutputPaneButton.setDisable(false);
                resetInputOutputPaneButtonIcon.setIconColor(Color.GREY.getColor(Color.Intensity.I900));
            });
        });
    }

    private void updateTitlePaneVisibility(TitledPane inputOutputPane) {
        // Check if the query is a refinement and that the engine is set to Reveaal
        if (controller.getQuery().getQuery().startsWith("refinement") && BackendHelper.backendSupportsInputOutputs(controller.getQuery().getBackend())) {
            initiateResetInputOutputButton(inputOutputPane);

            // Make the input/output pane visible
            inputOutputPaneVisibility(true);
        } else {
            // Hide the input/output pane
            inputOutputPaneVisibility(false);
        }
    }

    private void updateInputOutputs(TitledPane inputOutputPane, Boolean shouldResetSelections) {
        final VBox inputBox = (VBox) inputOutputPane.lookup("#inputBox");
        final VBox outputBox = (VBox) inputOutputPane.lookup("#outputBox");

        IgnoredInputOutputQuery query = new IgnoredInputOutputQuery(this.controller.getQuery(), this, controller.getQuery().ignoredInputs, inputBox, controller.getQuery().ignoredOutputs, outputBox);

        if (shouldResetSelections) {
            // Reset selections for ignored inputs and outputs
            clearIgnoredInputsAndOutputs(inputBox, outputBox);
        }

        Ecdar.getBackendDriver().getInputOutputs(query, controller.getQuery().getBackend());
    }

    private void clearIgnoredInputsAndOutputs(VBox inputBox, VBox outputBox) {
        controller.getQuery().ignoredInputs.clear();
        controller.getQuery().ignoredOutputs.clear();

        Platform.runLater(() -> {
            inputBox.getChildren().clear();
            outputBox.getChildren().clear();
        });
    }

    public void addInputOrOutput(String name, Boolean state, Map<String, Boolean> associatedMap, VBox associatedBox) {
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
        for (Map.Entry<String, Boolean> entry : controller.getQuery().ignoredInputs.entrySet()) {
            addInputOrOutput(entry.getKey(), entry.getValue(), controller.getQuery().ignoredInputs, inputBox);
        }

        // Add outputs as toggles in the GUI
        for (Map.Entry<String, Boolean> entry : controller.getQuery().ignoredOutputs.entrySet()) {
            addInputOrOutput(entry.getKey(), entry.getValue(), controller.getQuery().ignoredOutputs, outputBox);
        }
    }

//    private void setSwapBackendTooltipAndLabel(BackendInstance backend) {
//        swapBackendButtonTooltip.setText("Switch to the " + (isReveaal ? "jEcdar" : "Reveaal") + " backend");
//        currentBackendLabel.setText((isReveaal ? "Reveaal" : "jEcdar"));
//    }

    private void initializeMoreInformationButtonAndQueryTypeSymbol() {
        Platform.runLater(() -> {
            controller.queryTypeExpand.setVisible(true);
            controller.queryTypeExpand.setMaskType(JFXRippler.RipplerMask.RECT);
            controller.queryTypeExpand.setPosition(JFXRippler.RipplerPos.BACK);
            controller.queryTypeExpand.setRipplerFill(Color.GREY_BLUE.getColor(Color.Intensity.I500));

            final DropDownMenu queryTypeDropDown = new DropDownMenu(controller.queryTypeExpand);

            queryTypeDropDown.addListElement("Query Type");
            QueryType[] queryTypes = QueryType.values();
            for (QueryType type : queryTypes) {
                addQueryTypeListElement(type, queryTypeDropDown);
            }

            controller.queryTypeExpand.setOnMousePressed((e) -> {
                e.consume();
                queryTypeDropDown.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 16, 64);
            });

            controller.queryTypeSymbol.setText(controller.getQuery() != null && controller.getQuery().getType() != null ? controller.getQuery().getType().getSymbol() : "---");
        });
    }

    private void addQueryTypeListElement(final QueryType type, final DropDownMenu dropDownMenu) {
        MenuElement listElement = new MenuElement(type.getQueryName() + " [" + type.getSymbol() + "]", "gmi-done", mouseEvent -> {
            controller.getQuery().setType(type);
            controller.queryTypeSymbol.setText(type.getSymbol());
            dropDownMenu.hide();

            Set<Map.Entry<QueryType, SimpleBooleanProperty>> queryTypesSelected = controller.getQueryTypeListElementsSelectedState().entrySet();

            // Reflect the selection on the dropdown menu
            for (Map.Entry<QueryType, SimpleBooleanProperty> pair : queryTypesSelected) {
                pair.getValue().set(pair.getKey().equals(type));
            }
        });

        // Add boolean to the element to handle selection
        SimpleBooleanProperty selected = new SimpleBooleanProperty(controller.getQuery().getType() != null && controller.getQuery().getType().getSymbol().equals(type.getSymbol()));
        controller.getQueryTypeListElementsSelectedState().put(type, selected);
        listElement.setToggleable(selected);

        dropDownMenu.addMenuElement(listElement);
    }

    private void runQuery() {
        controller.getQuery().run();
    }
}
