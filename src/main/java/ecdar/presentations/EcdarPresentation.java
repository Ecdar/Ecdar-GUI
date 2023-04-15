package ecdar.presentations;

import com.jfoenix.controls.JFXSnackbarLayout;
import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Query;
import ecdar.abstractions.Snackbar;
import ecdar.controllers.EcdarController;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.ImageScaler;
import com.jfoenix.controls.JFXSnackbar;
import ecdar.utility.keyboard.Keybind;
import ecdar.utility.keyboard.KeyboardTracker;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.util.Duration;

public class EcdarPresentation extends StackPane {
    private final EcdarController controller;

    private final BooleanProperty leftPaneOpen = new SimpleBooleanProperty(true);
    private final SimpleDoubleProperty leftPaneAnimationProperty = new SimpleDoubleProperty(0);
    private final BooleanProperty rightPaneOpen = new SimpleBooleanProperty(true);
    private final SimpleDoubleProperty rightPaneAnimationProperty = new SimpleDoubleProperty(0);
    private Timeline openLeftPaneAnimation;
    private Timeline closeLeftPaneAnimation;
    private Timeline openRightPaneAnimation;
    private Timeline closeRightPaneAnimation;

    public EcdarPresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("EcdarPresentation.fxml", this);
        initializeTopBar();
        initializeQueryDetailsDialog();
        initializeToggleLeftPaneFunctionality();
        initializeToggleRightPaneFunctionality();
        initializeSnackbar();

        // Open the left and right panes initially
        Platform.runLater(() -> {
            // Bind sizing of sides and center panes to ensure correct sizing
            controller.getEditorPresentation().getController().canvasPane.minWidthProperty().bind(controller.root.widthProperty().subtract(leftPaneAnimationProperty.add(rightPaneAnimationProperty)));
            controller.getEditorPresentation().getController().canvasPane.maxWidthProperty().bind(controller.root.widthProperty().subtract(leftPaneAnimationProperty.add(rightPaneAnimationProperty)));

            // Bind the height to ensure that both the top and bottom panes are shown
            // The height of the top pane is multiplied by 4 as the UI does not account for the height otherwise
            controller.getEditorPresentation().getController().canvasPane.minHeightProperty().bind(controller.root.heightProperty().subtract(controller.topPane.heightProperty().multiply(4).add(controller.bottomFillerElement.heightProperty())));
            controller.getEditorPresentation().getController().canvasPane.maxHeightProperty().bind(controller.root.heightProperty().subtract(controller.topPane.heightProperty().multiply(4).add(controller.bottomFillerElement.heightProperty())));

            controller.leftPane.minWidthProperty().bind(leftPaneAnimationProperty);
            controller.leftPane.maxWidthProperty().bind(leftPaneAnimationProperty);

            controller.rightPane.minWidthProperty().bind(rightPaneAnimationProperty);
            controller.rightPane.maxWidthProperty().bind(rightPaneAnimationProperty);

            controller.topPane.minHeightProperty().bind(controller.menuBar.heightProperty());
            controller.topPane.maxHeightProperty().bind(controller.menuBar.heightProperty());

            EcdarController.currentMode.addListener(observable -> {
                initializeToggleLeftPaneFunctionality();
                initializeToggleRightPaneFunctionality();
            });

            Ecdar.getPresentation().controller.scalingProperty.addListener((observable, oldValue, newValue) -> {
                // If the scaling has changed trigger animations for open panes to update width
                Platform.runLater(() -> {
                    if (leftPaneOpen.get()) {
                        openLeftPaneAnimation.play();
                    }
                    if (rightPaneOpen.get()) {
                        openRightPaneAnimation.play();
                    }
                });
            });

            // Trigger closing followed by opening of the left pane to ensure correct placement
            closeLeftPaneAnimation.setOnFinished((e) -> openLeftPaneAnimation.play());
            closeLeftPaneAnimation.play();
        });

        initializeHelpImages();
        KeyboardTracker.registerKeybind(KeyboardTracker.UNDO, new Keybind(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN), UndoRedoStack::undo));
        KeyboardTracker.registerKeybind(KeyboardTracker.REDO, new Keybind(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN), UndoRedoStack::redo));
    }

    private void initializeSnackbar() {
        controller.snackbar = new Snackbar(controller.root);
        controller.snackbar.setPrefWidth(568);
        controller.snackbar.autosize();
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

    private void initializeToggleLeftPaneFunctionality() {
        initializeOpenLeftPaneAnimation();
        initializeCloseLeftPaneAnimation();

        // Translate the x coordinate to create the open/close animations
        controller.getLeftModePane().translateXProperty().bind(leftPaneAnimationProperty.subtract(controller.getLeftModePane().widthProperty()));

        // Whenever the width of the file pane is updated, update the animations
        controller.getLeftModePane().widthProperty().addListener((observable) -> {
            initializeOpenLeftPaneAnimation();
            initializeCloseLeftPaneAnimation();
        });

        // Whenever the width of the file pane is updated, update the animations
        controller.leftPane.widthProperty().addListener((observable) -> {
            initializeOpenLeftPaneAnimation();
            initializeCloseLeftPaneAnimation();
        });

    }

    private void initializeCloseLeftPaneAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        closeLeftPaneAnimation = new Timeline();

        final KeyValue open = new KeyValue(leftPaneAnimationProperty, controller.getLeftModePane().getWidth(), interpolator);
        final KeyValue closed = new KeyValue(leftPaneAnimationProperty, 0, interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), open);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), closed);

        closeLeftPaneAnimation.getKeyFrames().addAll(kf1, kf2);
    }

    private void initializeOpenLeftPaneAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        openLeftPaneAnimation = new Timeline();

        final KeyValue closed = new KeyValue(leftPaneAnimationProperty, 0, interpolator);
        final KeyValue open = new KeyValue(leftPaneAnimationProperty, controller.getLeftModePane().getWidth(), interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), closed);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), open);

        openLeftPaneAnimation.getKeyFrames().addAll(kf1, kf2);
    }

    private void initializeToggleRightPaneFunctionality() {
        initializeOpenRightPaneAnimation();
        initializeCloseRightPaneAnimation();

        // Translate the x coordinate to create the open/close animations
        controller.getRightModePane().translateXProperty().bind(rightPaneAnimationProperty.multiply(-1).add(controller.getRightModePane().widthProperty()));

        // Whenever the width of the query pane is updated, update the animations
        controller.getRightModePane().widthProperty().addListener((observable, oldWidth, newWidth) -> {
            Platform.runLater(() -> rightPaneAnimationProperty.set(controller.getRightModePane().getWidth()));

            initializeOpenRightPaneAnimation();
            initializeCloseRightPaneAnimation();
        });

        Platform.runLater(() -> {
            // When new queries are added, make sure that the query pane is open
            Ecdar.getProject().getQueries().addListener((ListChangeListener<Query>) c -> {
                if (closeRightPaneAnimation == null)
                    return; // The query pane is not yet initialized

                while (c.next()) {
                    c.getAddedSubList().forEach(o -> {
                        if (!rightPaneOpen.get()) {
                            // Open the pane
                            openRightPaneAnimation.play();

                            // Toggle the open state
                            rightPaneOpen.set(true);
                        }
                    });
                }
            });
        });
    }

    private void initializeCloseRightPaneAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        closeRightPaneAnimation = new Timeline();

        final KeyValue open = new KeyValue(rightPaneAnimationProperty, controller.getRightModePane().getWidth(), interpolator);
        final KeyValue closed = new KeyValue(rightPaneAnimationProperty, 0, interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), open);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), closed);

        closeRightPaneAnimation.getKeyFrames().addAll(kf1, kf2);
    }

    private void initializeOpenRightPaneAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        openRightPaneAnimation = new Timeline();

        final KeyValue closed = new KeyValue(rightPaneAnimationProperty, 0, interpolator);
        final KeyValue open = new KeyValue(rightPaneAnimationProperty, controller.getRightModePane().getWidth(), interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), closed);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), open);

        openRightPaneAnimation.getKeyFrames().addAll(kf1, kf2);
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

    /**
     * Initialize help image views.
     */
    private void initializeHelpImages() {
        controller.helpInitialImage.setImage(new Image(Ecdar.class.getResource("ic_help_initial.png").toExternalForm()));
        ImageScaler.fitImageToPane(controller.helpInitialImage, controller.helpInitialPane);

        controller.helpUrgentImage.setImage(new Image(Ecdar.class.getResource("ic_help_urgent.png").toExternalForm()));
        ImageScaler.fitImageToPane(controller.helpUrgentImage, controller.helpUrgentPane);

        controller.helpInputImage.setImage(new Image(Ecdar.class.getResource("ic_help_input.png").toExternalForm()));
        ImageScaler.fitImageToPane(controller.helpInputImage, controller.helpInputPane);

        controller.helpOutputImage.setImage(new Image(Ecdar.class.getResource("ic_help_output.png").toExternalForm()));
        ImageScaler.fitImageToPane(controller.helpOutputImage, controller.helpOutputPane);
    }

    public BooleanProperty toggleLeftPane() {
        if (leftPaneOpen.get()) {
            closeLeftPaneAnimation.play();
        } else {
            openLeftPaneAnimation.play();
        }

        // Toggle the open state
        leftPaneOpen.set(leftPaneOpen.not().get());

        return leftPaneOpen;
    }

    public BooleanProperty toggleRightPane() {
        if (rightPaneOpen.get()) {
            closeRightPaneAnimation.play();
        } else {
            openRightPaneAnimation.play();
        }

        // Toggle the open state
        rightPaneOpen.set(rightPaneOpen.not().get());

        return rightPaneOpen;
    }

    public void showSnackbarMessage(final String message) {
        JFXSnackbarLayout content = new JFXSnackbarLayout(message);
        controller.snackbar.enqueue(new JFXSnackbar.SnackbarEvent(content, new Duration(5000)));
    }

    public void showHelp() {
        controller.modellingHelpDialogContainer.setVisible(true);
        controller.modellingHelpDialog.show(controller.modellingHelpDialogContainer);
    }

    public EcdarController getController() {
        return controller;
    }
}
