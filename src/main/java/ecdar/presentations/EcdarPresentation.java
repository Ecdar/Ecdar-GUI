package ecdar.presentations;

import com.jfoenix.controls.JFXSnackbarLayout;
import ecdar.Ecdar;
import ecdar.abstractions.Query;
import ecdar.abstractions.Snackbar;
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
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
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

    private final BooleanProperty filePaneOpen = new SimpleBooleanProperty(false);
    private final SimpleDoubleProperty filePaneAnimationProperty = new SimpleDoubleProperty(0);
    private final BooleanProperty queryPaneOpen = new SimpleBooleanProperty(false);
    private final SimpleDoubleProperty queryPaneAnimationProperty = new SimpleDoubleProperty(0);
    private Timeline openQueryPaneAnimation;
    private Timeline closeQueryPaneAnimation;
    private Timeline openFilePaneAnimation;
    private Timeline closeFilePaneAnimation;

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
        Platform.runLater(() -> {
            // Bind sizing of sides and center panes to ensure correct sizing
            controller.canvasPane.minWidthProperty().bind(controller.root.widthProperty().subtract(filePaneAnimationProperty.add(queryPaneAnimationProperty)));
            controller.canvasPane.maxWidthProperty().bind(controller.root.widthProperty().subtract(filePaneAnimationProperty.add(queryPaneAnimationProperty)));

            // Bind the height to ensure that both the top and bottom panes are shown
            // The height of the top pane is multiplied by 4 as the UI does not account for the height otherwise
            controller.canvasPane.minHeightProperty().bind(controller.root.heightProperty().subtract(controller.topPane.heightProperty().multiply(4).add(controller.bottomFillerElement.heightProperty())));
            controller.canvasPane.maxHeightProperty().bind(controller.root.heightProperty().subtract(controller.topPane.heightProperty().multiply(4).add(controller.bottomFillerElement.heightProperty())));

            controller.leftPane.minWidthProperty().bind(filePaneAnimationProperty);
            controller.leftPane.maxWidthProperty().bind(filePaneAnimationProperty);

            controller.rightPane.minWidthProperty().bind(queryPaneAnimationProperty);
            controller.rightPane.maxWidthProperty().bind(queryPaneAnimationProperty);

            controller.topPane.minHeightProperty().bind(controller.menuBar.heightProperty());
            controller.topPane.maxHeightProperty().bind(controller.menuBar.heightProperty());

            Platform.runLater(() -> {
                toggleFilePane();
                toggleQueryPane();
            });

            Ecdar.getPresentation().controller.scalingProperty.addListener((observable, oldValue, newValue) -> {
                // If the scaling has changed trigger animations for open panes to update width
                Platform.runLater(() -> {
                    if (filePaneOpen.get()) {
                        openFilePaneAnimation.play();
                    }
                    if (queryPaneOpen.get()) {
                        openQueryPaneAnimation.play();
                    }
                });

                // Make sure that the grid covers the canvas even when the side panes are shrunk
                EcdarController.getActiveCanvasPresentation().getController().grid.updateGrid();
            });
        });

        initializeHelpImages();
        KeyboardTracker.registerKeybind(KeyboardTracker.ZOOM_IN, new Keybind(new KeyCodeCombination(KeyCode.PLUS, KeyCombination.SHORTCUT_DOWN), () -> EcdarController.getActiveCanvasPresentation().getController().zoomHelper.zoomIn()));
        KeyboardTracker.registerKeybind(KeyboardTracker.ZOOM_OUT, new Keybind(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN), () -> EcdarController.getActiveCanvasPresentation().getController().zoomHelper.zoomOut()));
        KeyboardTracker.registerKeybind(KeyboardTracker.ZOOM_TO_FIT, new Keybind(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN), () -> EcdarController.getActiveCanvasPresentation().getController().zoomHelper.zoomToFit()));
        KeyboardTracker.registerKeybind(KeyboardTracker.RESET_ZOOM, new Keybind(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN), () -> EcdarController.getActiveCanvasPresentation().getController().zoomHelper.resetZoom()));
        KeyboardTracker.registerKeybind(KeyboardTracker.UNDO, new Keybind(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN), UndoRedoStack::undo));
        KeyboardTracker.registerKeybind(KeyboardTracker.REDO, new Keybind(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), UndoRedoStack::redo));

        initializeResizeQueryPane();
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

    private void initializeToggleFilePaneFunctionality() {
        initializeOpenFilePaneAnimation();
        initializeCloseFilePaneAnimation();

        // Translate the x coordinate to create the open/close animations
        controller.filePane.translateXProperty().bind(filePaneAnimationProperty.subtract(controller.filePane.widthProperty()));

        // Whenever the width of the file pane is updated, update the animations
        controller.filePane.widthProperty().addListener((observable) -> {
            initializeOpenFilePaneAnimation();
            initializeCloseFilePaneAnimation();
        });
    }

    private void initializeCloseFilePaneAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        closeFilePaneAnimation = new Timeline();

        final KeyValue open = new KeyValue(filePaneAnimationProperty, controller.filePane.getWidth(), interpolator);
        final KeyValue closed = new KeyValue(filePaneAnimationProperty, 0, interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), open);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), closed);

        closeFilePaneAnimation.getKeyFrames().addAll(kf1, kf2);
    }

    private void initializeOpenFilePaneAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        openFilePaneAnimation = new Timeline();

        final KeyValue closed = new KeyValue(filePaneAnimationProperty, 0, interpolator);
        final KeyValue open = new KeyValue(filePaneAnimationProperty, controller.filePane.getWidth(), interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), closed);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), open);

        openFilePaneAnimation.getKeyFrames().addAll(kf1, kf2);
    }

    private void initializeToggleQueryPaneFunctionality() {
        initializeOpenQueryPaneAnimation();
        initializeCloseQueryPaneAnimation();

        // Translate the x coordinate to create the open/close animations
        controller.queryPane.translateXProperty().bind(queryPaneAnimationProperty.multiply(-1).add(controller.queryPane.widthProperty()));

        // Whenever the width of the query pane is updated, update the animations
        controller.queryPane.widthProperty().addListener((observable) -> {
            initializeOpenQueryPaneAnimation();
            initializeCloseQueryPaneAnimation();
        });

        // When new queries are added, make sure that the query pane is open
        Ecdar.getProject().getQueries().addListener((ListChangeListener<Query>) c -> {
            if (closeQueryPaneAnimation == null)
                return; // The query pane is not yet initialized

            while (c.next()) {
                c.getAddedSubList().forEach(o -> {
                    if (!queryPaneOpen.get()) {
                        // Open the pane
                        openQueryPaneAnimation.play();

                        // Toggle the open state
                        queryPaneOpen.set(queryPaneOpen.not().get());
                    }
                });
            }
        });
    }

    private void initializeCloseQueryPaneAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        closeQueryPaneAnimation = new Timeline();

        final KeyValue open = new KeyValue(queryPaneAnimationProperty, controller.queryPane.getWidth(), interpolator);
        final KeyValue closed = new KeyValue(queryPaneAnimationProperty, 0, interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), open);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), closed);

        closeQueryPaneAnimation.getKeyFrames().addAll(kf1, kf2);
    }

    private void initializeOpenQueryPaneAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        openQueryPaneAnimation = new Timeline();

        final KeyValue closed = new KeyValue(queryPaneAnimationProperty, 0, interpolator);
        final KeyValue open = new KeyValue(queryPaneAnimationProperty, controller.queryPane.getWidth(), interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), closed);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), open);

        openQueryPaneAnimation.getKeyFrames().addAll(kf1, kf2);
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

    private void initializeResizeQueryPane() {
        final DoubleProperty prevX = new SimpleDoubleProperty();
        final DoubleProperty prevWidth = new SimpleDoubleProperty();

        controller.queryPane.getController().resizeAnchor.setOnMousePressed(event -> {
            event.consume();

            prevX.set(event.getScreenX());
            prevWidth.set(controller.queryPane.getWidth());
        });

        controller.queryPane.getController().resizeAnchor.setOnMouseDragged(event -> {
            double diff = prevX.get() - event.getScreenX();

            // Set bounds for resizing to be between 280px and half the screen width
            final double newWidth = Math.min(Math.max(prevWidth.get() + diff, 280), controller.root.getWidth() / 2);

            queryPaneAnimationProperty.set(newWidth);
            controller.queryPane.setMaxWidth(newWidth);
            controller.queryPane.setMinWidth(newWidth);
        });
    }


    public BooleanProperty toggleFilePane() {
        if (filePaneOpen.get()) {
            closeFilePaneAnimation.play();
        } else {
            openFilePaneAnimation.play();
        }

        // Toggle the open state
        filePaneOpen.set(filePaneOpen.not().get());

        return filePaneOpen;
    }

    public BooleanProperty toggleQueryPane() {
        if (queryPaneOpen.get()) {
            closeQueryPaneAnimation.play();
        } else {
            openQueryPaneAnimation.play();
        }

        // Toggle the open state
        queryPaneOpen.set(queryPaneOpen.not().get());

        return queryPaneOpen;
    }

    public static void fitSizeWhenAvailable(final ImageView imageView, final StackPane pane) {
        pane.widthProperty().addListener((observable, oldValue, newValue) ->
                imageView.setFitWidth(pane.getWidth()));
        pane.heightProperty().addListener((observable, oldValue, newValue) ->
                imageView.setFitHeight(pane.getHeight()));
    }

    /**
     * Calls {@link CanvasPresentation#toggleGridUi()}.
     *
     * @return A Boolean Property that is true if the grid has been turned on and false if it is off
     */
    public BooleanProperty toggleGrid() {
        return EcdarController.getActiveCanvasPresentation().toggleGridUi();
    }

    public void showSnackbarMessage(final String message) {
        JFXSnackbarLayout content = new JFXSnackbarLayout(message);
        controller.snackbar.enqueue(new JFXSnackbar.SnackbarEvent(content, new Duration(5000)));
    }

    public void showHelp() {
        controller.dialogContainer.setVisible(true);
        controller.dialog.show(controller.dialogContainer);
    }

    public EcdarController getController() {
        return controller;
    }
}
