package ecdar.presentations;

import com.jfoenix.controls.JFXSnackbarLayout;
import ecdar.Ecdar;
import ecdar.abstractions.Query;
import ecdar.abstractions.Snackbar;
import ecdar.code_analysis.CodeAnalysis;
import ecdar.controllers.EcdarController;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.helpers.SelectHelper;
import com.jfoenix.controls.JFXPopup;
import com.jfoenix.controls.JFXRippler;
import com.jfoenix.controls.JFXSnackbar;
import ecdar.utility.keyboard.Keybind;
import ecdar.utility.keyboard.KeyboardTracker;
import javafx.animation.*;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
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
        initializeTopBar();
        initializeToolbar();
        initializeQueryDetailsDialog();
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
        initializeSnackbar();

        // Open the file and query panel initially
        final BooleanProperty ranInitialToggle = new SimpleBooleanProperty(false);
        controller.filePane.widthProperty().addListener((observable) -> {
            if (ranInitialToggle.get()) return;
            toggleFilePane();
            toggleQueryPane();
            ranInitialToggle.set(true);
        });
        initializeHelpImages();

        KeyboardTracker.registerKeybind(KeyboardTracker.ZOOM_IN, new Keybind(new KeyCodeCombination(KeyCode.PLUS, KeyCombination.SHORTCUT_DOWN), () -> EcdarController.getActiveCanvasShellPresentation().getController().zoomHelper.zoomIn()));
        KeyboardTracker.registerKeybind(KeyboardTracker.ZOOM_OUT, new Keybind(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN), () -> EcdarController.getActiveCanvasShellPresentation().getController().zoomHelper.zoomOut()));
        KeyboardTracker.registerKeybind(KeyboardTracker.ZOOM_TO_FIT, new Keybind(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN), () -> EcdarController.getActiveCanvasShellPresentation().getController().zoomHelper.zoomToFit()));
        KeyboardTracker.registerKeybind(KeyboardTracker.RESET_ZOOM, new Keybind(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN), () -> EcdarController.getActiveCanvasShellPresentation().getController().zoomHelper.resetZoom()));
        KeyboardTracker.registerKeybind(KeyboardTracker.UNDO, new Keybind(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN), UndoRedoStack::undo));
        KeyboardTracker.registerKeybind(KeyboardTracker.REDO, new Keybind(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), UndoRedoStack::redo));
    }

    private void initializeSnackbar() {
        controller.snackbar = new Snackbar(controller.root);
        controller.snackbar.setPrefWidth(568);
        controller.snackbar.autosize();
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

    private void initializeQueryDetailsDialog() {
        final Color modalBarColor = Color.GREY_BLUE;
        final Color.Intensity modalBarColorIntensity = Color.Intensity.I500;

        // Set the background of the modal bar
        controller.modalBar.setBackground(new Background(new BackgroundFill(
                modalBarColor.getColor(modalBarColorIntensity),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));
    }

    private void initializeToggleQueryPaneFunctionality() {
        // Set the translation of the query pane to be equal to its width
        // Will hide the element, and force it in then the right side of the border pane is enlarged
        controller.queryPane.translateXProperty().bind(controller.queryPane.widthProperty());

        // Whenever the width of the query pane is updated, update the animations
        controller.queryPane.widthProperty().addListener((observable) -> {
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

        final KeyValue open = new KeyValue(queryPaneAnimationProperty, controller.queryPane.getWidth(), interpolator);
        final KeyValue closed = new KeyValue(queryPaneAnimationProperty, 0, interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), open);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), closed);

        openQueryPaneAnimation.getKeyFrames().addAll(kf1, kf2);
    }

    private void initializeOpenQueryPaneAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        closeQueryPaneAnimation = new Timeline();

        final KeyValue closed = new KeyValue(queryPaneAnimationProperty, 0, interpolator);
        final KeyValue open = new KeyValue(queryPaneAnimationProperty, controller.queryPane.getWidth(), interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), closed);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), open);

        closeQueryPaneAnimation.getKeyFrames().addAll(kf1, kf2);
    }

    private void initializeToggleFilePaneFunctionality() {
        // Set the translation of the file pane to be equal to its width
        // Will hide the element, and force it in then the left side of the border pane is enlarged
        controller.filePane.translateXProperty().bind(controller.filePane.widthProperty().multiply(-1));

        // Whenever the width of the file pane is updated, update the animations
        controller.filePane.widthProperty().addListener((observable) -> {
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

        final KeyValue open = new KeyValue(filePaneAnimationProperty, controller.filePane.getWidth(), interpolator);
        final KeyValue closed = new KeyValue(filePaneAnimationProperty, 0, interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), open);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), closed);

        openFilePaneAnimation.getKeyFrames().addAll(kf1, kf2);
    }

    private void initializeOpenFilePaneAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        closeFilePaneAnimation = new Timeline();

        final KeyValue closed = new KeyValue(filePaneAnimationProperty, 0, interpolator);
        final KeyValue open = new KeyValue(filePaneAnimationProperty, controller.filePane.getWidth(), interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), closed);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), open);

        closeFilePaneAnimation.getKeyFrames().addAll(kf1, kf2);
    }

    private void initializeTopBar() {
        final Color color = Color.GREY_BLUE;
        final Color.Intensity intensity = Color.Intensity.I800;

        // Set the background for the top toolbar
        controller.menuBar.setBackground(
                new Background(new BackgroundFill(color.getColor(intensity),
                        CornerRadii.EMPTY,
                        Insets.EMPTY)
                ));

        // Set the bottom border
        controller.menuBar.setBorder(new Border(new BorderStroke(
                color.getColor(intensity.next()),
                BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY,
                new BorderWidths(0, 0, 1, 0)
        )));
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

    /**
     * Initialize help image views.
     */
    private void initializeHelpImages() {
        controller.helpInitialImage.setImage(new Image(Ecdar.class.getResource("ic_help_initial.png").toExternalForm()));
        fitSizeWhenAvailable(controller.helpInitialImage, controller.helpInitialPane);

        controller.helpUrgentImage.setImage(new Image(Ecdar.class.getResource("ic_help_urgent.png").toExternalForm()));
        fitSizeWhenAvailable(controller.helpUrgentImage, controller.helpUrgentPane);

        controller.helpInputImage.setImage(new Image(Ecdar.class.getResource("ic_help_input.png").toExternalForm()));
        fitSizeWhenAvailable(controller.helpInputImage, controller.helpInputPane);

        controller.helpOutputImage.setImage(new Image(Ecdar.class.getResource("ic_help_output.png").toExternalForm()));
        fitSizeWhenAvailable(controller.helpOutputImage, controller.helpOutputPane);
    }

    public static void fitSizeWhenAvailable(final ImageView imageView, final StackPane pane) {
        pane.widthProperty().addListener((observable, oldValue, newValue) ->
                imageView.setFitWidth(pane.getWidth()));
        pane.heightProperty().addListener((observable, oldValue, newValue) ->
                imageView.setFitHeight(pane.getHeight()));
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
     * Calls {@link CanvasShellPresentation#toggleGridUi()}.
     * @return A Boolean Property that is true if the grid has been turned on and false if it is off
     */
    public BooleanProperty toggleGrid(){
        return EcdarController.getActiveCanvasShellPresentation().toggleGridUi();
    }

    public void showSnackbarMessage(final String message) {
        JFXSnackbarLayout content = new JFXSnackbarLayout(message);
        controller.snackbar.enqueue(new JFXSnackbar.SnackbarEvent(content, new Duration(3000)));
    }

    public void showHelp() {
        controller.dialogContainer.setVisible(true);
        controller.dialog.show(controller.dialogContainer);
    }

    public EcdarController getController() {
        return controller;
    }
}
