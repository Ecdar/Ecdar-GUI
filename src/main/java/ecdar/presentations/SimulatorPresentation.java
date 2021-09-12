package ecdar.presentations;

import ecdar.simulation.EcdarSimulationController;
import ecdar.utility.colors.Color;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class SimulatorPresentation extends StackPane {
    private final EcdarSimulationController controller;
    private final BooleanProperty leftPaneOpen = new SimpleBooleanProperty(false);
    private final SimpleDoubleProperty leftPaneAnimationProperty = new SimpleDoubleProperty(0);
    private final BooleanProperty rightPaneOpen = new SimpleBooleanProperty(false);
    private final SimpleDoubleProperty rightPaneAnimationProperty = new SimpleDoubleProperty(0);
    private Timeline openRightPaneAnimation;
    private Timeline closeRightPaneAnimation;
    private Timeline closeLeftPaneAnimation;
    private Timeline openLeftPaneAnimation;

    public SimulatorPresentation() {
        controller = new EcdarFXMLLoader().loadAndGetController("SimulatorPresentation.fxml", this);
        initializeToolbar();
        
        initializeToggleLeftPaneFunctionality();
        initializeCloseLeftPaneAnimation();
        initializeOpenLeftPaneAnimation();

        initializeToggleRightPaneFunctionality();
        initializeOpenRightPaneAnimation();
        initializeCloseRightPaneAnimation();

        controller.leftSimPane.widthProperty().addListener((obs, oldVal, newVal) -> {
            // TODO: Make the panes open initially without a visible animation
            toggleLeftPane();
            toggleRightPane();
        });

        setBackground(new Background(new BackgroundFill(new javafx.scene.paint.Color(0, 0, 1, 1), CornerRadii.EMPTY, Insets.EMPTY)));
    }

    public BooleanProperty toggleLeftPane() {
        if (leftPaneOpen.get()) {
            openLeftPaneAnimation.play();
        } else {
            closeLeftPaneAnimation.play();
        }

        // Toggle the open state
        leftPaneOpen.set(leftPaneOpen.not().get());

        return leftPaneOpen;
    }

    public BooleanProperty toggleRightPane() {
        if (rightPaneOpen.get()) {
            openRightPaneAnimation.play();
        } else {
            closeRightPaneAnimation.play();
        }

        // Toggle the open state
        rightPaneOpen.set(rightPaneOpen.not().get());

        return rightPaneOpen;
    }

    /**
     * Initializes the {@link EcdarSimulationController#toolbar} with the right background color
     */
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

    private void initializeToggleRightPaneFunctionality() {
        // Set the translation of the query pane to be equal to its width
        // Will hide the element, and force it in then the right side of the border pane is enlarged
        controller.rightSimPane.translateXProperty().bind(controller.rightSimPane.widthProperty());

        // Whenever the width of the query pane is updated, update the animations
        controller.rightSimPane.widthProperty().addListener((observable) -> {
            initializeOpenRightPaneAnimation();
            initializeCloseRightPaneAnimation();
        });

        // Whenever the animation property changed, change the size of the filler element to push the simulator overview
        rightPaneAnimationProperty.addListener((observable, oldValue, newValue) -> {
            controller.rightPaneFillerElement.setMinWidth(newValue.doubleValue());
            controller.rightPaneFillerElement.setMaxWidth(newValue.doubleValue());
        });
    }

    private void initializeCloseRightPaneAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        openRightPaneAnimation = new Timeline();

        final KeyValue open = new KeyValue(rightPaneAnimationProperty, controller.rightSimPane.getWidth(), interpolator);
        final KeyValue closed = new KeyValue(rightPaneAnimationProperty, 0, interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), open);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), closed);

        openRightPaneAnimation.getKeyFrames().addAll(kf1, kf2);
    }

    private void initializeOpenRightPaneAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        closeRightPaneAnimation = new Timeline();

        final KeyValue closed = new KeyValue(rightPaneAnimationProperty, 0, interpolator);
        final KeyValue open = new KeyValue(rightPaneAnimationProperty, controller.rightSimPane.getWidth(), interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), closed);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), open);

        closeRightPaneAnimation.getKeyFrames().addAll(kf1, kf2);
    }

    private void initializeToggleLeftPaneFunctionality() {
        // Set the translation of the file pane to be equal to its width
        // Will hide the element, and force it in when the left side of the border pane is enlarged
        controller.leftSimPane.translateXProperty().bind(controller.leftSimPane.widthProperty().multiply(-1));

        // Whenever the width of the file pane is updated, update the animations
        controller.leftSimPane.widthProperty().addListener((observable) -> {
            initializeOpenLeftPaneAnimation();
            initializeCloseLeftPaneAnimation();
        });

        // Whenever the animation property changed, change the size of the filler element to push the simulator overview
        leftPaneAnimationProperty.addListener((observable, oldValue, newValue) -> {
            controller.leftPaneFillerElement.setMinWidth(newValue.doubleValue());
            controller.leftPaneFillerElement.setMaxWidth(newValue.doubleValue());
        });
    }

    private void initializeCloseLeftPaneAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        openLeftPaneAnimation = new Timeline();

        final KeyValue open = new KeyValue(leftPaneAnimationProperty, controller.leftSimPane.getWidth(), interpolator);
        final KeyValue closed = new KeyValue(leftPaneAnimationProperty, 0, interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), open);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), closed);

        openLeftPaneAnimation.getKeyFrames().addAll(kf1, kf2);
    }

    private void initializeOpenLeftPaneAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        closeLeftPaneAnimation = new Timeline();

        final KeyValue closed = new KeyValue(leftPaneAnimationProperty, 0, interpolator);
        final KeyValue open = new KeyValue(leftPaneAnimationProperty, controller.leftSimPane.getWidth(), interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), closed);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), open);

        closeLeftPaneAnimation.getKeyFrames().addAll(kf1, kf2);
    }


    /**
     * The way to get the associated/linked controller of this presenter
     * @return the controller linked to this presenter
     */
    public EcdarSimulationController getController() {
        return controller;
    }
}
