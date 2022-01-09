package ecdar.presentations;

import ecdar.Ecdar;
import ecdar.abstractions.Query;
import ecdar.controllers.EcdarController;
import ecdar.controllers.MainController;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.helpers.SelectHelper;
import com.jfoenix.controls.JFXPopup;
import com.jfoenix.controls.JFXRippler;
import javafx.animation.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

import static ecdar.utility.colors.EnabledColor.enabledColors;

public class EcdarPresentation extends StackPane {

    private final EcdarController controller;

    private final BooleanProperty queryPaneOpen = new SimpleBooleanProperty(false);
    private final SimpleDoubleProperty queryPaneAnimationProperty = new SimpleDoubleProperty(0);
    private final BooleanProperty filePaneOpen = new SimpleBooleanProperty(false);
    private final SimpleDoubleProperty filePaneAnimationProperty = new SimpleDoubleProperty(0);
    private Timeline closeQueryPaneAnimation;
    private Timeline openQueryPaneAnimation;
    private Timeline closeFilePaneAnimation;
    private Timeline openFilePaneAnimation;

    public EcdarPresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("EcdarPresentation.fxml", this);

        initializeToolbar();
        initializeColorSelector();

        initializeToggleQueryPaneFunctionality();
        initializeToggleFilePaneFunctionality();

        initializeSelectDependentToolbarButton(controller.colorSelected);
        Tooltip.install(controller.colorSelected, new Tooltip("Colour"));

        initializeSelectDependentToolbarButton(controller.deleteSelected);
        Tooltip.install(controller.deleteSelected, new Tooltip("Delete"));

        initializeToolbarButton(controller.undo);
        initializeToolbarButton(controller.redo);
        initializeUndoRedoButtons();

        // Open the file and query panel initially
        final BooleanProperty ranInitialToggle = new SimpleBooleanProperty(false);
        controller.projectPanePresentation.widthProperty().addListener((observable) -> {
            if (ranInitialToggle.get()) return;
            toggleFilePane();
            ranInitialToggle.set(true);
        });

        // ToDo NIELS: Check if the keybindings in MainController and CanvasPresentation can replace these
//        KeyboardTracker.registerKeybind(KeyboardTracker.ZOOM_IN, new Keybind(new KeyCodeCombination(KeyCode.PLUS, KeyCombination.SHORTCUT_DOWN), () -> MainController.getActiveCanvasPresentation().getController().zoomHelper.zoomIn()));
//        KeyboardTracker.registerKeybind(KeyboardTracker.ZOOM_OUT, new Keybind(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN), () -> MainController.getActiveCanvasPresentation().getController().zoomHelper.zoomOut()));
//        KeyboardTracker.registerKeybind(KeyboardTracker.ZOOM_TO_FIT, new Keybind(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN), () -> MainController.getActiveCanvasPresentation().getController().zoomHelper.zoomToFit()));
//        KeyboardTracker.registerKeybind(KeyboardTracker.RESET_ZOOM, new Keybind(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN), () -> MainController.getActiveCanvasPresentation().getController().zoomHelper.resetZoom()));
//        KeyboardTracker.registerKeybind(KeyboardTracker.UNDO, new Keybind(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN), UndoRedoStack::undo));
//        KeyboardTracker.registerKeybind(KeyboardTracker.REDO, new Keybind(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), UndoRedoStack::redo));
    }

    private void initializeUndoRedoButtons() {
        UndoRedoStack.canUndoProperty().addListener((obs, oldState, newState) -> {
            if (newState) {
                // Enable the undo button
                controller.undo.setEnabled(true);
                controller.undo.setOpacity(1);
            } else {
                // Disable the undo button
                controller.undo.setEnabled(false);
                controller.undo.setOpacity(0.3);
            }
        });

        UndoRedoStack.canRedoProperty().addListener((obs, oldState, newState) -> {
            if (newState) {
                // Enable the redo button
                controller.redo.setEnabled(true);
                controller.redo.setOpacity(1);
            } else {
                // Disable the redo button
                controller.redo.setEnabled(false);
                controller.redo.setOpacity(0.3);
            }
        });

        // Disable the undo button
        controller.undo.setEnabled(false);
        controller.undo.setOpacity(0.3);

        // Disable the redo button
        controller.redo.setEnabled(false);
        controller.redo.setOpacity(0.3);

        // Set tooltips
        Tooltip.install(controller.undo, new Tooltip("Undo"));
        Tooltip.install(controller.redo, new Tooltip("Redo"));
    }

    private void initializeColorSelector() {

        final JFXPopup popup = new JFXPopup();

        final double listWidth = 136;
        final FlowPane list = new FlowPane();
        for (final EnabledColor color : enabledColors) {
            final Circle circle = new Circle(16, color.color.getColor(color.intensity));
            circle.setStroke(color.color.getColor(color.intensity.next(2)));
            circle.setStrokeWidth(1);

            final Label label = new Label(color.keyCode.getName());
            label.getStyleClass().add("subhead");
            label.setTextFill(color.color.getTextColor(color.intensity));

            final StackPane child = new StackPane(circle, label);
            child.setMinSize(40, 40);
            child.setMaxSize(40, 40);

            child.setOnMouseEntered(event -> {
                final ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(100), circle);
                scaleTransition.setFromX(circle.getScaleX());
                scaleTransition.setFromY(circle.getScaleY());
                scaleTransition.setToX(1.1);
                scaleTransition.setToY(1.1);
                scaleTransition.play();
            });

            child.setOnMouseExited(event -> {
                final ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(100), circle);
                scaleTransition.setFromX(circle.getScaleX());
                scaleTransition.setFromY(circle.getScaleY());
                scaleTransition.setToX(1.0);
                scaleTransition.setToY(1.0);
                scaleTransition.play();
            });

            child.setOnMouseClicked(event -> {
                final List<Pair<SelectHelper.ItemSelectable, EnabledColor>> previousColor = new ArrayList<>();

                SelectHelper.getSelectedElements().forEach(selectable -> {
                    previousColor.add(new Pair<>(selectable, new EnabledColor(selectable.getColor(), selectable.getColorIntensity())));
                });

                controller.changeColorOnSelectedElements(color, previousColor);

                popup.hide();
                SelectHelper.clearSelectedElements();
            });

            list.getChildren().add(child);
        }
        list.setMinWidth(listWidth);
        list.setMaxWidth(listWidth);
        list.setStyle("-fx-background-color: white; -fx-padding: 8;");

        popup.setPopupContent(list);

        controller.colorSelected.setOnMouseClicked((e) -> {
            // If nothing is selected
            if (SelectHelper.getSelectedElements().size() == 0) return;
            popup.show(controller.colorSelected, JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.RIGHT, -10, 15);
        });
    }

    private void initializeSelectDependentToolbarButton(final JFXRippler button) {
        initializeToolbarButton(button);

        // The color button should only be enabled when an element is selected
        SelectHelper.getSelectedElements().addListener(new ListChangeListener<SelectHelper.ItemSelectable>() {
            @Override
            public void onChanged(final Change<? extends SelectHelper.ItemSelectable> c) {
                if (SelectHelper.getSelectedElements().size() > 0) {
                    button.setEnabled(true);

                    final FadeTransition fadeAnimation = new FadeTransition(Duration.millis(100), button);
                    fadeAnimation.setFromValue(button.getOpacity());
                    fadeAnimation.setToValue(1);
                    fadeAnimation.play();
                } else {
                    button.setEnabled(false);

                    final FadeTransition fadeAnimation = new FadeTransition(Duration.millis(100), button);
                    fadeAnimation.setFromValue(1);
                    fadeAnimation.setToValue(0.3);
                    fadeAnimation.play();
                }
            }
        });

        // Disable the button
        button.setEnabled(false);
        button.setOpacity(0.3);
    }

    private void initializeToolbarButton(final JFXRippler button) {
        final Color color = Color.GREY_BLUE;
        final Color.Intensity colorIntensity = Color.Intensity.I800;

        button.setMaskType(JFXRippler.RipplerMask.CIRCLE);
        button.setRipplerFill(color.getTextColor(colorIntensity));
        button.setPosition(JFXRippler.RipplerPos.BACK);
    }

    private void initializeToggleQueryPaneFunctionality() {
        initializeOpenQueryPaneAnimation();
        initializeCloseQueryPaneAnimation();

        // Set the translation of the query pane to be equal to its width
        // Will hide the element, and force it in then the right side of the border pane is enlarged
        controller.rightSimPanePresentation.translateXProperty().bind(controller.rightSimPanePresentation.widthProperty());

        // Whenever the width of the query pane is updated, update the animations
        controller.rightSimPanePresentation.widthProperty().addListener((observable) -> {
            initializeOpenQueryPaneAnimation();
            initializeCloseQueryPaneAnimation();
        });

        // Whenever the animation property changed, change the size of the filler element to push the canvas
        queryPaneAnimationProperty.addListener((observable, oldValue, newValue) -> {
            controller.queryPaneFillerElement.setMinWidth(newValue.doubleValue());
            controller.queryPaneFillerElement.setMaxWidth(newValue.doubleValue());
        });

        // When new queries are added, make sure that the query pane is open
        Ecdar.getProject().getQueries().addListener(new ListChangeListener<Query>() {
            @Override
            public void onChanged(final Change<? extends Query> c) {
                if (queryPaneOpen == null || openQueryPaneAnimation == null)
                    return; // The query pane is not yet initialized

                while (c.next()) {
                    c.getAddedSubList().forEach(o -> {
                        if (!queryPaneOpen.get()) {
                            // Open the pane
                            closeQueryPaneAnimation.play();

                            // Toggle the open state
                            queryPaneOpen.set(queryPaneOpen.not().get());
                        }
                    });
                }
            }
        });
    }

    private void initializeCloseQueryPaneAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        openQueryPaneAnimation = new Timeline();

        final KeyValue open = new KeyValue(queryPaneAnimationProperty, controller.rightSimPanePresentation.getWidth(), interpolator);
        final KeyValue closed = new KeyValue(queryPaneAnimationProperty, 0, interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), open);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), closed);

        openQueryPaneAnimation.getKeyFrames().addAll(kf1, kf2);
    }

    private void initializeOpenQueryPaneAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        closeQueryPaneAnimation = new Timeline();

        final KeyValue closed = new KeyValue(queryPaneAnimationProperty, 0, interpolator);
        final KeyValue open = new KeyValue(queryPaneAnimationProperty, controller.rightSimPanePresentation.getWidth(), interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), closed);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), open);

        closeQueryPaneAnimation.getKeyFrames().addAll(kf1, kf2);
    }

    private void initializeToggleFilePaneFunctionality() {
        // Set the translation of the file pane to be equal to its width
        // Will hide the element, and force it in then the left side of the border pane is enlarged
        controller.projectPanePresentation.translateXProperty().bind(controller.projectPanePresentation.widthProperty().multiply(-1));

        // Whenever the width of the file pane is updated, update the animations
        controller.projectPanePresentation.widthProperty().addListener((observable) -> {
            initializeOpenFilePaneAnimation();
            initializeCloseFilePaneAnimation();
        });

        // Whenever the animation property changed, change the size of the filler element to push the canvas
        filePaneAnimationProperty.addListener((observable, oldValue, newValue) -> {
            controller.filePaneFillerElement.setMinWidth(newValue.doubleValue());
            controller.filePaneFillerElement.setMaxWidth(newValue.doubleValue());
        });
    }

    private void initializeCloseFilePaneAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        openFilePaneAnimation = new Timeline();

        final KeyValue open = new KeyValue(filePaneAnimationProperty, controller.projectPanePresentation.getWidth(), interpolator);
        final KeyValue closed = new KeyValue(filePaneAnimationProperty, 0, interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), open);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), closed);

        openFilePaneAnimation.getKeyFrames().addAll(kf1, kf2);
    }

    private void initializeOpenFilePaneAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        closeFilePaneAnimation = new Timeline();

        final KeyValue closed = new KeyValue(filePaneAnimationProperty, 0, interpolator);
        final KeyValue open = new KeyValue(filePaneAnimationProperty, controller.projectPanePresentation.getWidth(), interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), closed);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), open);

        closeFilePaneAnimation.getKeyFrames().addAll(kf1, kf2);
    }

    private void initializeToolbar() {
        final Color color = Color.GREY_BLUE;
        final Color.Intensity intensity = Color.Intensity.I700;

        // Set the background for the top toolbar
        controller.toolbar.setBackground(
                new Background(new BackgroundFill(color.getColor(intensity),
                        CornerRadii.EMPTY,
                        Insets.EMPTY)
                ));
    }

    public EcdarController getController() {
        return controller;
    }

    public BooleanProperty toggleQueryPane() {
        if (queryPaneOpen.get()) {
            openQueryPaneAnimation.play();
        } else {
            closeQueryPaneAnimation.play();
        }

        // Toggle the open state
        queryPaneOpen.set(queryPaneOpen.not().get());

        return queryPaneOpen;
    }

    public BooleanProperty toggleFilePane() {
        if (filePaneOpen.get()) {
            openFilePaneAnimation.play();
        } else {
            closeFilePaneAnimation.play();
        }

        // Toggle the open state
        filePaneOpen.set(filePaneOpen.not().get());

        return filePaneOpen;
    }

    /**
     * Calls {@link CanvasPresentation#toggleGridUi()}.
     * @return A Boolean Property that is true if the grid has been turned on and false if it is off
     */
    public BooleanProperty toggleGrid(){
        return MainController.getActiveCanvasPresentation().toggleGridUi();
    }
}