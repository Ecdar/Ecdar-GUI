package ecdar.presentations;

import com.jfoenix.controls.*;
import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.backend.*;
import ecdar.controllers.QueryController;
import ecdar.controllers.EcdarController;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.StringHelper;
import ecdar.utility.helpers.StringValidator;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.geometry.Point2D;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material.Material;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static javafx.scene.paint.Color.*;

public class QueryPresentation extends HBox {
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
        initializeMoreInformationButtonAndQueryTypeSymbol();
        initializeBackendsDropdown();

        // Ensure that the icons are scaled to current font scale
        Platform.runLater(() -> Ecdar.getPresentation().getController().scaleIcons(this));
    }

    private void initializeBackendsDropdown() {
        controller.backendsDropdown.setItems(BackendHelper.getBackendInstances());
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
                    if (keyEvent.getCode().equals(KeyCode.ENTER) && controller.getQuery().getType() != null) {
                        runQuery();
                    }
                });
            }));

            queryTextField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue && !StringValidator.validateQuery(queryTextField.getText())) {
                    queryTextField.getStyleClass().add("input-violation");
                } else {
                    queryTextField.getStyleClass().remove("input-violation");
                }
            });

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
            final VBox stateIndicator = (VBox) lookup("#stateIndicator");
            final FontIcon statusIcon = (FontIcon) stateIndicator.lookup("#statusIcon");
            final FontIcon queryTypeExpandIcon = (FontIcon) stateIndicator.lookup("#queryTypeExpandIcon");

            // Delegate that based on a query state updates tooltip of the query
            final Consumer<QueryState> updateToolTip = (queryState) -> {
                if (queryState.getStatusCode() == 1) {
                    if(queryState.getIconCode().equals(Material.DONE)) {
                        this.tooltip.setText("This query was a success!");
                    } else {
                        this.tooltip.setText("The component has been created (can be accessed in the project pane)");
                    }
                }
                else if (queryState.getStatusCode() == 2){
                    this.tooltip.setText("This query was not a success: " + controller.getQuery().getCurrentErrors());
                }
                else if (queryState.getStatusCode() == 3) {
                    this.tooltip.setText("The query has not been executed yet");
                } else {
                    this.tooltip.setText(controller.getQuery().getCurrentErrors());
                }
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

            statusIcon.setOnMouseClicked(event -> {
                if (controller.getQuery().getQuery().isEmpty()) return;

                Label label = new Label(tooltip.getText());

                JFXDialog dialog = new InformationDialogPresentation("Result from query: " + StringHelper.ConvertSymbolsToUnicode(controller.getQuery().getQuery()), label);
                dialog.show(Ecdar.getPresentation());
            });
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
                //height of app window
                double windowHeight = this.getScene().getHeight();
                //Location of dropdown relative to the app window
                Point2D Origin = this.localToScene(this.getWidth(), this.getHeight());
                //Generate the popups properties before displaying
                queryTypeDropDown.show(this);
                //Check if the dropdown can fit the app window.
                if(Origin.getY()+queryTypeDropDown.getHeight() >= windowHeight){
                    queryTypeDropDown.show(JFXPopup.PopupVPosition.BOTTOM, JFXPopup.PopupHPosition.RIGHT, -55,-(Origin.getY()-windowHeight)-50);
                }
                else{
                    queryTypeDropDown.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.RIGHT, 16, 16);
                }

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
        Ecdar.getQueryExecutor().executeQuery(this.controller.getQuery());
    }
}
