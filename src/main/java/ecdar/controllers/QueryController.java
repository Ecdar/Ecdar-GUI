package ecdar.controllers;

import EcdarProtoBuf.QueryProtos;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jfoenix.controls.*;
import ecdar.Ecdar;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXRippler;
import ecdar.abstractions.Component;
import ecdar.backend.BackendException;
import ecdar.backend.Engine;
import ecdar.abstractions.Query;
import ecdar.abstractions.QueryState;
import ecdar.abstractions.QueryType;
import ecdar.backend.BackendHelper;
import ecdar.presentations.DropDownMenu;
import ecdar.presentations.InformationDialogPresentation;
import ecdar.presentations.MenuElement;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.StringValidator;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material.Material;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

import static javafx.scene.paint.Color.TRANSPARENT;

public class QueryController implements Initializable {
    public VBox stateIndicator;
    public FontIcon statusIcon;
    public JFXRippler queryTypeExpand;
    public Text queryTypeSymbol;
    public FontIcon queryTypeExpandIcon;
    public JFXTextField queryTextField;
    public JFXTextField commentTextField;
    public JFXSpinner progressIndicator;
    public JFXRippler actionButton;
    public FontIcon actionButtonIcon;
    public JFXRippler detailsButton;
    public FontIcon detailsButtonIcon;
    public JFXComboBox<Engine> enginesDropdown;

    private final Tooltip tooltip = new Tooltip();
    private final Tooltip noQueryTypeSetTooltip = new Tooltip("Please select a query type beneath the status icon");
    private Query query;
    private final Map<QueryType, SimpleBooleanProperty> queryTypeListElementsSelectedState = new HashMap<>();
    private final StringProperty queryErrors = new SimpleStringProperty("");

    private final Consumer<Boolean> successConsumer = (aBoolean) -> {
        if (aBoolean) {
            getQuery().setQueryState(QueryState.SUCCESSFUL);
        } else {
            getQuery().setQueryState(QueryState.ERROR);
        }
    };
    private Boolean forcedCancel = false;
    private final Consumer<Exception> failureConsumer = (e) -> {
        if (forcedCancel) {
            getQuery().setQueryState(QueryState.UNKNOWN);
        } else {
            getQuery().setQueryState(QueryState.SYNTAX_ERROR);
            if (e instanceof BackendException.MissingFileQueryException) {
                Ecdar.showToast("Please save the project before trying to run queries");
            }

            addError(e.getMessage());
            final Throwable cause = e.getCause();
            if (cause != null) {
                // We had trouble generating the model if we get a NullPointerException
                if (cause instanceof NullPointerException) {
                    getQuery().setQueryState(QueryState.UNKNOWN);
                } else {
                    Platform.runLater(() -> EcdarController.openQueryDialog(getQuery(), cause.toString()));
                }
            }
        }
    };

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeStateIndicator();
        initializeProgressIndicator();
        initializeActionButton();
        initializeDetailsButton();
        initializeTextFields();
        initializeMoreInformationButtonAndQueryTypeSymbol();
        initializeBackendsDropdown();
    }

    private void initializeBackendsDropdown() {
        enginesDropdown.setItems(BackendHelper.getEngines());
        Tooltip backendDropdownTooltip = new Tooltip();
        backendDropdownTooltip.setText("Current backend used for the query");
        JFXTooltip.install(enginesDropdown, backendDropdownTooltip);
        enginesDropdown.setValue(BackendHelper.getDefaultEngine());
    }

    private void initializeTextFields() {
        Platform.runLater(() -> {
            queryTextField.setText(getQuery().getQuery());
            commentTextField.setText(getQuery().getComment());

            getQuery().queryProperty().bind(queryTextField.textProperty());
            getQuery().commentProperty().bind(commentTextField.textProperty());


            queryTextField.setOnKeyPressed(EcdarController.getActiveCanvasPresentation().getController().getLeaveTextAreaKeyHandler(keyEvent -> {
                Platform.runLater(() -> {
                    if (keyEvent.getCode().equals(KeyCode.ENTER)) {
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
            detailsButtonIcon.setIconColor(Color.GREY.getColor(Color.Intensity.I900));

            detailsButton.setCursor(Cursor.HAND);
            detailsButton.setRipplerFill(Color.GREY.getColor(Color.Intensity.I500));
            detailsButton.setMaskType(JFXRippler.RipplerMask.CIRCLE);

            final DropDownMenu dropDownMenu = new DropDownMenu(detailsButton);

            dropDownMenu.addToggleableListElement("Run Periodically", getQuery().isPeriodicProperty(), event -> {
                // Toggle the property
                getQuery().setIsPeriodic(!getQuery().isPeriodic());
                dropDownMenu.hide();
            });
            dropDownMenu.addSpacerElement();
            dropDownMenu.addClickableListElement("Clear Status", event -> {
                // Clear the state
                getQuery().setQueryState(QueryState.UNKNOWN);
                dropDownMenu.hide();
            });
            dropDownMenu.addSpacerElement();
            dropDownMenu.addClickableListElement("Delete", event -> {
                // Remove the query
                Ecdar.getProject().getQueries().remove(getQuery());
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
            if (getQuery() == null) {
                actionButtonIcon.setIconColor(Color.GREY.getColor(Color.Intensity.I500));
            }

            actionButton.setCursor(Cursor.HAND);
            actionButton.setRipplerFill(Color.GREY.getColor(Color.Intensity.I500));

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
            updateIcon.accept(getQuery().getQueryState());

            // Update the icon when ever the query state is updated
            getQuery().queryStateProperty().addListener((observable, oldValue, newValue) -> updateIcon.accept(newValue));

            actionButton.setMaskType(JFXRippler.RipplerMask.CIRCLE);

            actionButton.getChildren().get(0).setOnMousePressed(event -> {
                Platform.runLater(() -> {
                    if (getQuery().getQueryState().equals(QueryState.RUNNING)) {
                        cancelQuery();
                    } else {
                        runQuery();
                    }
                });
            });

            Platform.runLater(() -> {
                if (query.getType() == null) {
                    actionButton.setDisable(true);
                    actionButtonIcon.setIconColor(Color.GREY.getColor(Color.Intensity.I500));
                    Tooltip.install(actionButton.getParent(), noQueryTypeSetTooltip);
                }
            });
        });
    }

    private void initializeProgressIndicator() {
        Platform.runLater(() -> {
            // If the query is running show the indicator, otherwise hide it
            progressIndicator.visibleProperty().bind(new When(getQuery().queryStateProperty().isEqualTo(QueryState.RUNNING)).then(true).otherwise(false));
        });
    }

    private void initializeStateIndicator() {
        Platform.runLater(() -> {
            // Delegate that based on a query state updates tooltip of the query
            final Consumer<QueryState> updateToolTip = (queryState) -> {
                if (queryState.getStatusCode() == 1) {
                    if(queryState.getIconCode().equals(Material.DONE)) {
                        this.tooltip.setText("This query was a success!");
                    } else {
                        this.tooltip.setText("The component has been created (can be accessed in the project pane)");
                    }
                } else if (queryState.getStatusCode() == 3) {
                    this.tooltip.setText("The query has not been executed yet");
                } else {
                    this.tooltip.setText(getCurrentErrors());
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
            updateStateIndicator.accept(getQuery().getQueryState());

            // Ensure that the color is updated when ever the query state is updated
            getQuery().queryStateProperty().addListener((observable, oldValue, newValue) -> updateStateIndicator.accept(newValue));

            // Ensure that the tooltip is updated when new errors are added
            queryErrors.addListener((observable, oldValue, newValue) -> updateToolTip.accept(getQuery().getQueryState()));
            this.tooltip.setMaxWidth(300);
            this.tooltip.setWrapText(true);

            // Installing the tooltip on the statusIcon itself scales the tooltip unexpectedly, hence its parent StackPane is used
            Tooltip.install(statusIcon.getParent(), this.tooltip);

            queryTypeSymbol.setText(getQuery() != null && getQuery().getType() != null ? getQuery().getType().getSymbol() : "---");

            statusIcon.setOnMouseClicked(event -> {
                if (getQuery().getQuery().isEmpty()) return;

                Label label = new Label(tooltip.getText());
                JFXDialog dialog = new InformationDialogPresentation("Result from query: " + getQuery().getQuery(), label);
                dialog.show(Ecdar.getPresentation());
            });
        });
    }

    private void initializeMoreInformationButtonAndQueryTypeSymbol() {
        Platform.runLater(() -> {
            queryTypeExpand.setVisible(true);
            queryTypeExpand.setMaskType(JFXRippler.RipplerMask.RECT);
            queryTypeExpand.setPosition(JFXRippler.RipplerPos.BACK);
            queryTypeExpand.setRipplerFill(Color.GREY_BLUE.getColor(Color.Intensity.I500));

            final DropDownMenu queryTypeDropDown = new DropDownMenu(queryTypeExpand);

            queryTypeDropDown.addListElement("Query Type");
            QueryType[] queryTypes = QueryType.values();
            for (QueryType type : queryTypes) {
                addQueryTypeListElement(type, queryTypeDropDown);
            }

            queryTypeExpand.setOnMousePressed((e) -> {
                e.consume();
                queryTypeDropDown.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 16, 16);
            });

            queryTypeSymbol.setText(getQuery() != null && getQuery().getType() != null ? getQuery().getType().getSymbol() : "---");
        });
    }

    public void setQuery(final Query query) {
        this.query = query;
        this.query.getTypeProperty().addListener(((observable, oldValue, newValue) -> {
            if(newValue != null) {
                actionButton.setDisable(false);
                actionButtonIcon.setIconColor(Color.GREY.getColor(Color.Intensity.I900));
                Platform.runLater(() -> {
                    Tooltip.uninstall(actionButton.getParent(), noQueryTypeSetTooltip);
                });
            } else {
                actionButton.setDisable(true);
                actionButtonIcon.setIconColor(Color.GREY.getColor(Color.Intensity.I500));
                Platform.runLater(() -> {
                    Tooltip.install(actionButton.getParent(), noQueryTypeSetTooltip);
                });
            }
        }));

        if (BackendHelper.getEngines().contains(query.getEngine())) {
            enginesDropdown.setValue(query.getEngine());
        } else {
            enginesDropdown.setValue(BackendHelper.getDefaultEngine());
        }

        enginesDropdown.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                query.setEngine(newValue);
            } else {
                enginesDropdown.setValue(BackendHelper.getDefaultEngine());
            }
        });

        BackendHelper.addEngineInstanceListener(() -> {
            Platform.runLater(() -> {
                // The value must be set before the items (https://stackoverflow.com/a/29483445)
                if (BackendHelper.getEngines().contains(query.getEngine())) {
                    enginesDropdown.setValue(query.getEngine());
                } else {
                    enginesDropdown.setValue(BackendHelper.getDefaultEngine());
                }
                enginesDropdown.setItems(BackendHelper.getEngines());
            });
        });
    }

    public Query getQuery() {
        return query;
    }

    private void setStatusIndicatorContentColor(javafx.scene.paint.Color color, FontIcon statusIcon, FontIcon queryTypeExpandIcon, QueryState queryState) {
        statusIcon.setIconColor(color);
        queryTypeSymbol.setFill(color);
        queryTypeExpandIcon.setIconColor(color);

        if (queryState != null) {
            statusIcon.setIconLiteral("gmi-" + queryState.getIconCode().toString().toLowerCase().replace('_', '-'));
        }
    }

    private void addQueryTypeListElement(final QueryType type, final DropDownMenu dropDownMenu) {
        MenuElement listElement = new MenuElement(type.getQueryName() + " [" + type.getSymbol() + "]", "gmi-done", mouseEvent -> {
            getQuery().setType(type);
            queryTypeSymbol.setText(type.getSymbol());
            dropDownMenu.hide();

            Set<Map.Entry<QueryType, SimpleBooleanProperty>> queryTypesSelected = getQueryTypeListElementsSelectedState().entrySet();

            // Reflect the selection on the dropdown menu
            for (Map.Entry<QueryType, SimpleBooleanProperty> pair : queryTypesSelected) {
                pair.getValue().set(pair.getKey().equals(type));
            }
        });

        // Add boolean to the element to handle selection
        SimpleBooleanProperty selected = new SimpleBooleanProperty(getQuery().getType() != null && getQuery().getType().getSymbol().equals(type.getSymbol()));
        getQueryTypeListElementsSelectedState().put(type, selected);
        listElement.setToggleable(selected);

        dropDownMenu.addMenuElement(listElement);
    }

    /**
     * Executes the query
     */
    public void runQuery() throws NoSuchElementException {
        if (getQuery().getQueryState().equals(QueryState.RUNNING) || !StringValidator.validateQuery(getQuery().getQuery()))
            return;

        if (getQuery().getQuery().isEmpty()) {
            getQuery().setQueryState(QueryState.SYNTAX_ERROR);
            addError("Query is empty");
            return;
        }

        getQuery().setQueryState(QueryState.RUNNING);
        clearQueryErrors();

        getQuery().getEngine().enqueueQuery(getQuery(), this::handleQueryResponse, this::handleQueryBackendError);
    }

    public void cancelQuery() {
        if (query.getQueryState().equals(QueryState.RUNNING)) {
            setForcedCancel(true);
            query.setQueryState(QueryState.UNKNOWN);
        }
    }

    public void setForcedCancel(Boolean forcedCancel) {
        this.forcedCancel = forcedCancel;
    }

    public void addError(String e) {
        queryErrors.set(queryErrors.getValue() + e + "\n");
    }

    private void clearQueryErrors() {
        queryErrors.set("");
    }

    public String getCurrentErrors() {
        return queryErrors.getValue();
    }

    private void handleQueryResponse(QueryProtos.QueryResponse value) {
        // If the query has been cancelled, ignore the result
        if (getQuery().getQueryState() == QueryState.UNKNOWN) return;

        if (value.hasRefinement() && value.getRefinement().getSuccess()) {
            getQuery().setQueryState(QueryState.SUCCESSFUL);
            successConsumer.accept(true);
        } else if (value.hasConsistency() && value.getConsistency().getSuccess()) {
            getQuery().setQueryState(QueryState.SUCCESSFUL);
            successConsumer.accept(true);
        } else if (value.hasDeterminism() && value.getDeterminism().getSuccess()) {
            getQuery().setQueryState(QueryState.SUCCESSFUL);
            successConsumer.accept(true);
        } else if (value.hasComponent()) {
            getQuery().setQueryState(QueryState.SUCCESSFUL);
            successConsumer.accept(true);
            JsonObject returnedComponent = (JsonObject) JsonParser.parseString(value.getComponent().getComponent().getJson());
            addGeneratedComponent(new Component(returnedComponent));
        } else {
            getQuery().setQueryState(QueryState.ERROR);
            successConsumer.accept(false);
        }
    }

    private void handleQueryBackendError(Throwable t) {
        // If the query has been cancelled, ignore the error
        if (getQuery().getQueryState() == QueryState.UNKNOWN) return;

        // Each error starts with a capitalized description of the error equal to the gRPC error type encountered
        String errorType = t.getMessage().split(":\\s+", 2)[0];

        if ("DEADLINE_EXCEEDED".equals(errorType)) {
            getQuery().setQueryState(QueryState.ERROR);
            failureConsumer.accept(new BackendException.QueryErrorException("The engine did not answer the request in time"));
        } else {
            try {
                getQuery().setQueryState(QueryState.ERROR);
                failureConsumer.accept(new BackendException.QueryErrorException("The execution of this query failed with message:" + System.lineSeparator() + t.getLocalizedMessage()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void addGeneratedComponent(Component newComponent) {
        Platform.runLater(() -> {
            newComponent.setTemporary(true);

            ObservableList<Component> listOfGeneratedComponents = Ecdar.getProject().getTempComponents(); // ToDo NIELS: Refactor
            Component matchedComponent = null;

            for (Component currentGeneratedComponent : listOfGeneratedComponents) {
                int comparisonOfNames = currentGeneratedComponent.getName().compareTo(newComponent.getName());

                if (comparisonOfNames == 0) {
                    matchedComponent = currentGeneratedComponent;
                    break;
                } else if (comparisonOfNames < 0) {
                    break;
                }
            }

            if (matchedComponent == null) {
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    Ecdar.getProject().getTempComponents().add(newComponent);
                }, () -> { // Undo
                    Ecdar.getProject().getTempComponents().remove(newComponent);
                }, "Created new component: " + newComponent.getName(), "add-circle");
            } else {
                // Remove current component with name and add the newly generated one
                Component finalMatchedComponent = matchedComponent;
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    Ecdar.getProject().getTempComponents().remove(finalMatchedComponent);
                    Ecdar.getProject().getTempComponents().add(newComponent);
                }, () -> { // Undo
                    Ecdar.getProject().getTempComponents().remove(newComponent);
                    Ecdar.getProject().getTempComponents().add(finalMatchedComponent);
                }, "Created new component: " + newComponent.getName(), "add-circle");
            }

            Ecdar.getProject().addComponent(newComponent);
        });
    }

    public Map<QueryType, SimpleBooleanProperty> getQueryTypeListElementsSelectedState() {
        return queryTypeListElementsSelectedState;
    }
}
