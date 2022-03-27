package ecdar.controllers;

import com.jfoenix.controls.JFXRippler;
import com.jfoenix.controls.JFXTabPane;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.code_analysis.CodeAnalysis;
import ecdar.presentations.MessageCollectionPresentation;
import ecdar.presentations.MessagePresentation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class MessageTabPaneController implements Initializable {
    public StackPane root;
    public JFXTabPane tabPane;
    public Tab errorsTab;
    public Tab warningsTab;
    public Rectangle tabPaneResizeElement;
    public StackPane tabPaneContainer;
    public JFXRippler collapseMessages;
    public FontIcon collapseMessagesIcon;
    public ScrollPane errorsScrollPane;
    public VBox errorsList;
    public ScrollPane warningsScrollPane;
    public VBox warningsList;
    public Tab backendErrorsTab;
    public ScrollPane backendErrorsScrollPane;
    public VBox backendErrorsList;

    private double tabPanePreviousY = 0;
    private double expandHeight = 300;
    private final Property<Boolean> isOpen = new SimpleBooleanProperty(false);
    private Runnable openCloseExternalAction;
    public boolean shouldISkipOpeningTheMessagesContainer = true;

    private final Transition expandMessagesContainer = new Transition() {
        {
            setInterpolator(Interpolator.SPLINE(0.645, 0.045, 0.355, 1));
            setCycleDuration(Duration.millis(200));
        }

        @Override
        protected void interpolate(final double frac) {
            tabPaneContainer.setMaxHeight(35 + frac * (expandHeight - 35));
        }
    };

    private Transition collapseMessagesContainer;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        Platform.runLater(() -> {
            collapseMessagesContainer = new Transition() {
                {
                    setInterpolator(Interpolator.SPLINE(0.645, 0.045, 0.355, 1));
                    setCycleDuration(Duration.millis(200));
                }

                @Override
                protected void interpolate(final double frac) {
                    tabPaneContainer.setMaxHeight(((tabPaneContainer.getMaxHeight() - 35) * (1 - frac)) + 35);
                }
            };

            initializeTabPane();
            initializeMessages();

            collapseMessagesContainer.play();
        });
    }

    private void initializeTabPane() {
        tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldSelected, newSelected) -> {
            if (newSelected.intValue() < 0 || isOpen.getValue()) return;

            if (shouldISkipOpeningTheMessagesContainer) {
                tabPane.getSelectionModel().clearSelection();
                shouldISkipOpeningTheMessagesContainer = false;
            } else {
                expandMessagesIfNotExpanded();
            }
        });

        tabPane.getSelectionModel().clearSelection();
        tabPane.setTabMinHeight(35);
        tabPane.setTabMaxHeight(35);

        isOpen.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                collapseMessagesIcon.setIconLiteral("gmi-close");
            } else {
                collapseMessagesIcon.setIconLiteral("gmi-expand-less");
            }

            openCloseExternalAction.run();
        });

        isOpen.setValue(false);
    }

    private void initializeMessages() {
        final Map<Component, MessageCollectionPresentation> componentMessageCollectionPresentationMapForErrors = new HashMap<>();
        final Map<Component, MessageCollectionPresentation> componentMessageCollectionPresentationMapForWarnings = new HashMap<>();

        final Consumer<Component> addComponent = (component) -> {
            final MessageCollectionPresentation messageCollectionPresentationErrors = new MessageCollectionPresentation(component, CodeAnalysis.getErrors(component));
            componentMessageCollectionPresentationMapForErrors.put(component, messageCollectionPresentationErrors);
            errorsList.getChildren().add(messageCollectionPresentationErrors);

            final Runnable addIfErrors = () -> {
                if (CodeAnalysis.getErrors(component).size() == 0) {
                    errorsList.getChildren().remove(messageCollectionPresentationErrors);
                } else if (!errorsList.getChildren().contains(messageCollectionPresentationErrors)) {
                    errorsList.getChildren().add(messageCollectionPresentationErrors);
                }
            };

            addIfErrors.run();
            CodeAnalysis.getErrors(component).addListener((ListChangeListener<CodeAnalysis.Message>) c -> {
                while (c.next()) {
                    addIfErrors.run();
                }
            });

            final MessageCollectionPresentation messageCollectionPresentationWarnings = new MessageCollectionPresentation(component, CodeAnalysis.getWarnings(component));
            componentMessageCollectionPresentationMapForWarnings.put(component, messageCollectionPresentationWarnings);
            warningsList.getChildren().add(messageCollectionPresentationWarnings);

            final Runnable addIfWarnings = () -> {
                if (CodeAnalysis.getWarnings(component).size() == 0) {
                    warningsList.getChildren().remove(messageCollectionPresentationWarnings);
                } else if (!warningsList.getChildren().contains(messageCollectionPresentationWarnings)) {
                    warningsList.getChildren().add(messageCollectionPresentationWarnings);
                }
            };

            addIfWarnings.run();
            CodeAnalysis.getWarnings(component).addListener((ListChangeListener<CodeAnalysis.Message>) c -> {
                while (c.next()) {
                    addIfWarnings.run();
                }
            });
        };

        // Add error that is project wide but not a backend error
        addComponent.accept(null);

        Ecdar.getProject().getComponents().forEach(addComponent);
        Ecdar.getProject().getComponents().addListener((ListChangeListener<Component>) c -> {
            while (c.next()) {
                c.getAddedSubList().forEach(addComponent::accept);

                c.getRemoved().forEach(component -> {
                    errorsList.getChildren().remove(componentMessageCollectionPresentationMapForErrors.get(component));
                    componentMessageCollectionPresentationMapForErrors.remove(component);

                    warningsList.getChildren().remove(componentMessageCollectionPresentationMapForWarnings.get(component));
                    componentMessageCollectionPresentationMapForWarnings.remove(component);
                });
            }
        });

        final Map<CodeAnalysis.Message, MessagePresentation> messageMessagePresentationHashMap = new HashMap<>();

        CodeAnalysis.getBackendErrors().addListener((ListChangeListener<CodeAnalysis.Message>) c -> {
            while (c.next()) {
                c.getAddedSubList().forEach(addedMessage -> {
                    final MessagePresentation messagePresentation = new MessagePresentation(addedMessage);
                    backendErrorsList.getChildren().add(messagePresentation);
                    messageMessagePresentationHashMap.put(addedMessage, messagePresentation);
                });

                c.getRemoved().forEach(removedMessage -> {
                    backendErrorsList.getChildren().remove(messageMessagePresentationHashMap.get(removedMessage));
                    messageMessagePresentationHashMap.remove(removedMessage);
                });
            }
        });
    }

    public void expandMessagesIfNotExpanded() {
        if (!isOpen.getValue()) {
            expandMessagesContainer.play();
            isOpen.setValue(true);
        }
    }

    public void collapseMessagesIfNotCollapsed() {
        if (isOpen.getValue()) {
            expandHeight = tabPaneContainer.getHeight();
            collapseMessagesContainer.play();
            isOpen.setValue(false);
        }
    }

    public void setRunnableForOpeningAndClosingMessageTabPane(Runnable runnable) {
        openCloseExternalAction = runnable;
    }

    public boolean isOpen() {
        return isOpen.getValue();
    }

    @FXML
    public void collapseMessagesClicked() {
        if (isOpen()) {
            expandHeight = tabPaneContainer.getHeight();
            collapseMessagesContainer.play();
        } else {
            expandMessagesContainer.play();
        }

        isOpen.setValue(!isOpen.getValue());
    }

    @FXML
    public void tabPaneResizeElementPressed(final MouseEvent event) {
        this.tabPanePreviousY = event.getScreenY();
    }

    @FXML
    public void tabPaneResizeElementDragged(final MouseEvent event) {
        expandMessagesIfNotExpanded();

        final double mouseY = event.getScreenY();
        double newHeight = tabPaneContainer.getMaxHeight() - (mouseY - tabPanePreviousY);
        newHeight = Math.max(35, newHeight);

        tabPaneContainer.setMaxHeight(newHeight);

        openCloseExternalAction.run();
        this.tabPanePreviousY = mouseY;
    }
}