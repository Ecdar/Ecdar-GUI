package ecdar.presentations;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import ecdar.controllers.LocationController;
import ecdar.utility.colors.Color;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.helpers.BindingHelper;
import ecdar.utility.helpers.SelectHelper;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.*;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.shape.*;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static javafx.util.Duration.millis;

/**
 * Presentation for a location.
 */
public class LocationPresentation extends Group implements SelectHelper.Selectable {
    public static final double RADIUS = 15;
    public static final double INITIAL_RADIUS = RADIUS / 4 * 3;
    private static int id = 0;
    private final LocationController controller;
    private final Timeline initialAnimation = new Timeline();
    private final Timeline hoverAnimationEntered = new Timeline();
    private final Timeline hoverAnimationExited = new Timeline();
    private final Timeline hiddenAreaAnimationEntered = new Timeline();
    private final Timeline hiddenAreaAnimationExited = new Timeline();
    private final Timeline scaleShakeIndicatorBackgroundAnimation = new Timeline();
    private final Timeline shakeContentAnimation = new Timeline();
    private final List<Consumer<EnabledColor>> updateColorDelegates = new ArrayList<>();
    private final DoubleProperty animation = new SimpleDoubleProperty(0);
    private final DoubleBinding reverseAnimation = new SimpleDoubleProperty(1).subtract(animation);
    private final boolean interactable;
    private BooleanProperty isPlaced = new SimpleBooleanProperty(true);
    private Timeline shakeDeleteAnimation = new Timeline();

    public LocationPresentation(final Location location, final Component component) {
        this(location, component, true);
    }

    public LocationPresentation(final Location location, final Component component, final boolean interactable) {
        this.interactable = interactable;
        controller = new EcdarFXMLLoader().loadAndGetController("LocationPresentation.fxml", this);

        // Bind the component with the one of the controller
        controller.setComponent(component);

        // Bind the location with the one of the controller
        controller.setLocation(location);

        controller.initializeInvalidNameError();

        initializeIdLabel();
        initializeTypeGraphics();
        initializeLocationShapes();
        initializeTags();
        initializeInitialAnimation();
        initializeHoverAnimationEntered();
        initializeHoverAnimationExited();
        initializeDeleteShakeAnimation();
        initializeShakeAnimation();
        initializeCircle();
    }

    private void initializeIdLabel() {
        final Location location = controller.getLocation();
        final Label idLabel = controller.idLabel;

        final DropShadow ds = new DropShadow();
        ds.setRadius(2);
        ds.setSpread(1);

        idLabel.setEffect(ds);

        idLabel.textProperty().bind((location.idProperty()));

        // Center align the label
        idLabel.widthProperty().addListener((obsWidth, oldWidth, newWidth) -> idLabel.translateXProperty().set(newWidth.doubleValue() / -2));
        idLabel.heightProperty().addListener((obsHeight, oldHeight, newHeight) -> idLabel.translateYProperty().set(newHeight.doubleValue() / -2));

        // Delegate to style the label based on the color of the location
        final Consumer<EnabledColor> updateColor = (newColor) -> {
            if (location.getFailing()) {
                idLabel.setTextFill(Color.RED.getTextColor(Color.Intensity.I700));
                ds.setColor(Color.RED.getColor(Color.Intensity.I700));
            } else {
                idLabel.setTextFill(newColor.getTextColor());
                ds.setColor(newColor.getPaintColor());
            }
        };

        updateColorDelegates.add(updateColor);

        // Set the initial color
        updateColor.accept(location.getColor());

        // Update the color of the circle when the color of the location is updated
        location.colorProperty().addListener((obs, old, newColor) -> updateColor.accept(newColor));
        location.failingProperty().addListener((obs, old, newFailing) -> updateColor.accept(location.getColor())); // RED if failing, provided color otherwise
    }

    private void initializeTags() {
        if (!interactable) {
            controller.nicknameTag.setVisible(false);
            controller.invariantTag.setVisible(false);
            return;
        }

        controller.nicknameTag.replaceSpace();
        controller.nicknameTag.replaceSigns();
        controller.invariantTag.replaceSigns();


        // Set the layout from the model (if they are not both 0)
        final Location loc = controller.getLocation();
        if ((loc.getNicknameX() != 0) && (loc.getNicknameY() != 0)) {
            controller.nicknameTag.setTranslateX(loc.getNicknameX());
            controller.nicknameTag.setTranslateY(loc.getNicknameY());
        }

        if ((loc.getInvariantX() != 0) && (loc.getInvariantY() != 0)) {
            controller.invariantTag.setTranslateX(loc.getInvariantX());
            controller.invariantTag.setTranslateY(loc.getInvariantY());
        }

        // Bind the model to the layout
        // Check is needed because the property cannot be bound twice
        // which happens when switching from the simulator to the editor
        if (!loc.nicknameXProperty().isBound() && !loc.nicknameYProperty().isBound() &&
                !loc.invariantXProperty().isBound() && !loc.invariantYProperty().isBound()) {
            loc.nicknameXProperty().bindBidirectional(controller.nicknameTag.translateXProperty());
            loc.nicknameYProperty().bindBidirectional(controller.nicknameTag.translateYProperty());
            loc.invariantXProperty().bindBidirectional(controller.invariantTag.translateXProperty());
            loc.invariantYProperty().bindBidirectional(controller.invariantTag.translateYProperty());
        }

        final Consumer<Location> updateTags = location -> {
            // Update the color
            controller.nicknameTag.bindToColor(location.colorProperty(), true);
            controller.invariantTag.bindToColor(location.colorProperty(), false);

            // Update the invariant
            controller.nicknameTag.setAndBindString(location.nicknameProperty());
            controller.invariantTag.setAndBindString(location.invariantProperty());

            // Update the placeholder
            controller.nicknameTag.setPlaceholder("No name");
            controller.invariantTag.setPlaceholder("No invariant");

            // Set the visibility of the name tag depending on the nickname
            final Consumer<String> updateVisibilityFromNickName = (nickname) -> {
                if (nickname.equals("") && !controller.nicknameTag.textFieldFocusProperty().get()) {
                    controller.nicknameTag.setOpacity(0);
                } else {
                    controller.nicknameTag.setOpacity(1);
                }
            };

            controller.nicknameTag.textFieldFocusProperty().addListener((obs, oldFocus, newFocus) -> {
                updateVisibilityFromNickName.accept(controller.getLocation().getNickname());
            });

            // Update the visibility according to if the location have been placed
            controller.nicknameTag.visibleProperty().bind(isPlaced);
            controller.invariantTag.visibleProperty().bind(isPlaced);

            location.nicknameProperty().addListener((obs, oldNickname, newNickname) -> updateVisibilityFromNickName.accept(newNickname));
            updateVisibilityFromNickName.accept(location.getNickname());

            // Set the visibility of the invariant tag depending on the invariant
            final Consumer<String> updateVisibilityFromInvariant = (invariant) -> {
                if (invariant.equals("") && !controller.invariantTag.textFieldFocusProperty().get()) {
                    controller.invariantTag.setOpacity(0);
                } else {
                    controller.invariantTag.setOpacity(1);
                }
            };

            controller.invariantTag.textFieldFocusProperty().addListener((obs, oldFocus, newFocus) -> {
                updateVisibilityFromInvariant.accept(controller.getLocation().getInvariant());
            });

            location.invariantProperty().addListener((obs, oldInvariant, newInvariant) -> updateVisibilityFromInvariant.accept(newInvariant));
            updateVisibilityFromInvariant.accept(location.getInvariant());

            controller.nicknameTag.setComponent(controller.getComponent());
            controller.nicknameTag.setLocationAware(location);
            BindingHelper.bind(controller.nameTagLine, controller.nicknameTag);

            controller.invariantTag.setComponent(controller.getComponent());
            controller.invariantTag.setLocationAware(location);
            BindingHelper.bind(controller.invariantTagLine, controller.invariantTag);
        };

        Platform.runLater(() -> {
            controller.nicknameTag.setOnKeyPressed(Ecdar.getPresentation().getController().getEditorPresentation().getController().getActiveCanvasPresentation().getController().getLeaveTextAreaKeyHandler());
            controller.invariantTag.setOnKeyPressed(Ecdar.getPresentation().getController().getEditorPresentation().getController().getActiveCanvasPresentation().getController().getLeaveTextAreaKeyHandler());
        });

        // Update the tags when the loc updates
        controller.locationProperty().addListener(observable -> updateTags.accept(loc));

        // Initialize the tags from the current loc
        updateTags.accept(loc);

    }

    private void initializeHoverAnimationEntered() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        final KeyValue scale1x = new KeyValue(controller.scaleContent.scaleXProperty(), 1, interpolator);
        final KeyValue scale2x = new KeyValue(controller.scaleContent.scaleXProperty(), 1.1, interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), scale1x);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(100), scale2x);

        hoverAnimationEntered.getKeyFrames().addAll(kf1, kf2);
    }

    private void initializeHoverAnimationExited() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        final KeyValue scale2x = new KeyValue(controller.scaleContent.scaleXProperty(), 1.1, interpolator);
        final KeyValue scale1x = new KeyValue(controller.scaleContent.scaleXProperty(), 1, interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), scale2x);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(100), scale1x);

        hoverAnimationExited.getKeyFrames().addAll(kf1, kf2);
    }

    private void initializeInitialAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);
        final KeyValue scale0x = new KeyValue(controller.scaleContent.scaleXProperty(), 0, interpolator);
        final KeyValue scale2x = new KeyValue(controller.scaleContent.scaleXProperty(), 1.1, interpolator);
        final KeyValue scale1x = new KeyValue(controller.scaleContent.scaleXProperty(), 1, interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), scale0x);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(200), scale2x);
        final KeyFrame kf3 = new KeyFrame(Duration.millis(250), scale1x);

        initialAnimation.getKeyFrames().addAll(kf1, kf2, kf3);
    }

    private void initializeCircle() {
        final Location location = controller.getLocation();

        final Circle circle = controller.circle;
        circle.setRadius(RADIUS);
        final ObjectProperty<EnabledColor> color = location.colorProperty();

        // Delegate to style the label based on the color of the location
        final Consumer<EnabledColor> updateColor = (newColor) -> {
            circle.setFill(newColor.getPaintColor());
            circle.setStroke(newColor.getStrokeColor());
        };

        updateColorDelegates.add(updateColor);

        // Set the initial color
        updateColor.accept(color.get());

        // Update the color of the circle when the color of the location is updated
        color.addListener((obs, old, newColor) -> updateColor.accept(newColor));
    }

    private void initializeLocationShapes() {
        final Path notCommittedShape = controller.notCommittedShape;

        final Rectangle committedShape = controller.committedShape;

        // Bind sizes for shape that transforms between urgent and normal
        initializeLocationShapes(notCommittedShape, RADIUS);

        // Bind sized for committed shape
        committedShape.setWidth(RADIUS * 2);
        committedShape.setHeight(RADIUS * 2);
        committedShape.setLayoutX(-RADIUS);
        committedShape.setLayoutY(-RADIUS);

        final Location location = controller.getLocation();

        BiConsumer<Location.Urgency, Location.Urgency> updateUrgencies = (oldUrgency, newUrgency) -> {
            final Transition toUrgent = new Transition() {
                {
                    setCycleDuration(Duration.millis(200));
                }

                @Override
                protected void interpolate(final double frac) {
                    animation.set(frac);
                }
            };

            final Transition toNormal = new Transition() {
                {
                    setCycleDuration(Duration.millis(200));
                }

                @Override
                protected void interpolate(final double frac) {
                    animation.set(1 - frac);
                }
            };

            boolean isNormalOrProhibited = newUrgency.equals(Location.Urgency.NORMAL) || newUrgency.equals(Location.Urgency.PROHIBITED);

            if (!oldUrgency.equals(Location.Urgency.URGENT) && !isNormalOrProhibited) {
                toUrgent.play();
            } else if (isNormalOrProhibited && oldUrgency.equals(Location.Urgency.URGENT)) {
                toNormal.play();
            }

            if (newUrgency.equals(Location.Urgency.COMMITTED)) {
                committedShape.setVisible(true);
                notCommittedShape.setVisible(false);
            } else {
                committedShape.setVisible(false);
                notCommittedShape.setVisible(true);
            }

            if (newUrgency.equals(Location.Urgency.PROHIBITED)) {
                notCommittedShape.setStrokeWidth(4);
                notCommittedShape.setStroke(Color.RED.getColor(Color.Intensity.A700));
                controller.prohibitedLocStrikeThrough.setVisible(true);
            } else {
                notCommittedShape.setStrokeWidth(1);
                notCommittedShape.setStroke(location.getColor().getStrokeColor());
                controller.prohibitedLocStrikeThrough.setVisible(false);
            }
        };

        location.urgencyProperty().addListener((obsUrgency, oldUrgency, newUrgency) -> {
            updateUrgencies.accept(oldUrgency, newUrgency);
        });

        updateUrgencies.accept(Location.Urgency.NORMAL, location.getUrgency());

        // Delegate to style the label based on the color of the location
        final Consumer<EnabledColor> updateColor = (newColor) -> { // ToDo NIELS: Fix this consumer, it seems a bit weird
            if (!location.getUrgency().equals(Location.Urgency.PROHIBITED)) {
                if (location.getFailing()) {
                    notCommittedShape.setFill(Color.RED.getColor(Color.Intensity.I700));
                } else {
                    notCommittedShape.setFill(newColor.getStrokeColor());
                }
            } else if (location.getFailing()) {
                notCommittedShape.setFill(Color.RED.getColor(Color.Intensity.I700));
                committedShape.setFill(Color.RED.getColor(Color.Intensity.I700));
                committedShape.setStroke(Color.RED.getColor(Color.Intensity.I700.next(2)));
            } else {
                notCommittedShape.setFill(newColor.getPaintColor());
                committedShape.setFill(newColor.getPaintColor());
                committedShape.setStroke(newColor.getStrokeColor());
            }
        };

        updateColorDelegates.add(updateColor);

        // Set the initial color
        updateColor.accept(location.getColor());

        // Update the color of the circle when the color of the location is updated
        location.colorProperty().addListener((obs, old, newColor) -> updateColor.accept(newColor));
        location.failingProperty().addListener(obs -> updateColor.accept(location.getColor()));
    }

    private void initializeTypeGraphics() {
        final Location location = controller.getLocation();

        final Path notCommittedInitialIndicator = controller.notCommittedInitialIndicator;

        // Bind visibility and size of not committed shape
        initializeLocationShapes(notCommittedInitialIndicator, INITIAL_RADIUS);
        notCommittedInitialIndicator.visibleProperty().bind(location.typeProperty().isEqualTo(Location.Type.INITIAL).and(location.urgencyProperty().isNotEqualTo(Location.Urgency.COMMITTED)));

        final Rectangle committedInitialIndicator = controller.committedInitialIndicator;

        // Bind size of committed shape
        committedInitialIndicator.setWidth(INITIAL_RADIUS * 2);
        committedInitialIndicator.setHeight(INITIAL_RADIUS * 2);
        committedInitialIndicator.setLayoutX(-INITIAL_RADIUS);
        committedInitialIndicator.setLayoutY(-INITIAL_RADIUS);

        committedInitialIndicator.visibleProperty().bind(location.typeProperty().isEqualTo(Location.Type.INITIAL).and(location.urgencyProperty().isEqualTo(Location.Urgency.COMMITTED)));
    }

    public void setLocation(final Location location) {
        controller.setLocation(location);
    }

    public void animateIn() {
        initialAnimation.play();
    }

    public void animateHoverEntered() {

        if (shakeContentAnimation.getStatus().equals(Animation.Status.RUNNING) || !interactable) return;

        hoverAnimationEntered.play();
    }

    public void animateHoverExited() {
        if (shakeContentAnimation.getStatus().equals(Animation.Status.RUNNING) || !interactable) return;

        hoverAnimationExited.play();
    }

    public void animateLocationEntered() {
        hiddenAreaAnimationExited.stop();
        hiddenAreaAnimationEntered.play();
    }

    public void animateLocationExited() {
        hiddenAreaAnimationEntered.stop();
        hiddenAreaAnimationExited.play();
    }

    private void initializeDeleteShakeAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        final double startX = controller.scaleContent.getLayoutX();
        final KeyValue kv1 = new KeyValue(controller.scaleContent.layoutXProperty(), startX - 3, interpolator);
        final KeyValue kv2 = new KeyValue(controller.scaleContent.layoutXProperty(), startX + 3, interpolator);
        final KeyValue kv3 = new KeyValue(controller.scaleContent.layoutXProperty(), startX, interpolator);

        final KeyFrame kf1 = new KeyFrame(millis(50), kv1);
        final KeyFrame kf2 = new KeyFrame(millis(100), kv2);
        final KeyFrame kf3 = new KeyFrame(millis(150), kv1);
        final KeyFrame kf4 = new KeyFrame(millis(200), kv2);
        final KeyFrame kf5 = new KeyFrame(millis(250), kv3);

        shakeDeleteAnimation.getKeyFrames().addAll(kf1, kf2, kf3, kf4, kf5);
    }

    public void shake() {
        shakeDeleteAnimation.play();
    }

    public boolean isPlaced() {
        return isPlaced.get();
    }

    public void setPlaced(boolean placed) {
        isPlaced.set(placed);
    }

    public boolean isInteractable() {
        return interactable;
    }

    private void initializeShakeAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        final KeyValue scale0x = new KeyValue(controller.scaleContent.scaleXProperty(), 1, interpolator);
        final KeyValue radius0 = new KeyValue(controller.circleShakeIndicator.radiusProperty(), 0, interpolator);
        final KeyValue opacity0 = new KeyValue(controller.circleShakeIndicator.opacityProperty(), 0, interpolator);

        final KeyValue scale1x = new KeyValue(controller.scaleContent.scaleXProperty(), 1.3, interpolator);
        final KeyValue radius1 = new KeyValue(controller.circleShakeIndicator.radiusProperty(), controller.circle.getRadius() * 0.85, interpolator);
        final KeyValue opacity1 = new KeyValue(controller.circleShakeIndicator.opacityProperty(), 0.2, interpolator);

        final KeyFrame kf1 = new KeyFrame(Duration.millis(0), scale0x, radius0, opacity0);
        final KeyFrame kf2 = new KeyFrame(Duration.millis(2500), scale1x, radius1, opacity1);
        final KeyFrame kf3 = new KeyFrame(Duration.millis(3300), radius0, opacity0);
        final KeyFrame kf4 = new KeyFrame(Duration.millis(3500), scale0x);
        final KeyFrame kfEnd = new KeyFrame(Duration.millis(8000));

        scaleShakeIndicatorBackgroundAnimation.getKeyFrames().addAll(kf1, kf2, kf3, kf4, kfEnd);

        final KeyValue noShakeX = new KeyValue(controller.shakeContent.translateXProperty(), 0, interpolator);
        final KeyValue shakeLeftX = new KeyValue(controller.shakeContent.translateXProperty(), -1, interpolator);
        final KeyValue shakeRightX = new KeyValue(controller.shakeContent.translateXProperty(), 1, interpolator);

        final KeyFrame[] shakeFrames = {
                new KeyFrame(Duration.millis(0), noShakeX),
                new KeyFrame(Duration.millis(1450), noShakeX),

                new KeyFrame(Duration.millis(1500), shakeLeftX),
                new KeyFrame(Duration.millis(1550), shakeRightX),
                new KeyFrame(Duration.millis(1600), shakeLeftX),
                new KeyFrame(Duration.millis(1650), shakeRightX),
                new KeyFrame(Duration.millis(1700), shakeLeftX),
                new KeyFrame(Duration.millis(1750), shakeRightX),
                new KeyFrame(Duration.millis(1800), shakeLeftX),
                new KeyFrame(Duration.millis(1850), shakeRightX),
                new KeyFrame(Duration.millis(1900), shakeLeftX),
                new KeyFrame(Duration.millis(1950), shakeRightX),
                new KeyFrame(Duration.millis(2000), shakeLeftX),
                new KeyFrame(Duration.millis(2050), shakeRightX),
                new KeyFrame(Duration.millis(2100), shakeLeftX),
                new KeyFrame(Duration.millis(2150), shakeRightX),
                new KeyFrame(Duration.millis(2200), shakeLeftX),
                new KeyFrame(Duration.millis(2250), shakeRightX),

                new KeyFrame(Duration.millis(2300), noShakeX),
                new KeyFrame(Duration.millis(8000))
        };

        shakeContentAnimation.getKeyFrames().addAll(shakeFrames);

        shakeContentAnimation.setCycleCount(1000);
        scaleShakeIndicatorBackgroundAnimation.setCycleCount(1000);
    }

    public void animateShakeWarning(final boolean start) {
        if (start) {
            scaleShakeIndicatorBackgroundAnimation.play();
            shakeContentAnimation.play();
        } else {
            controller.scaleContent.scaleXProperty().set(1);
            scaleShakeIndicatorBackgroundAnimation.playFromStart();
            scaleShakeIndicatorBackgroundAnimation.stop();

            controller.circleShakeIndicator.setOpacity(0);
            shakeContentAnimation.playFromStart();
            shakeContentAnimation.stop();
        }
    }

    @Override
    public void select() {
        updateColorDelegates.forEach(colorConsumer -> colorConsumer.accept(new EnabledColor(SelectHelper.SELECT_COLOR, SelectHelper.SELECT_COLOR_INTENSITY_NORMAL)));
    }

    @Override
    public void deselect() {
        updateColorDelegates.forEach(colorConsumer -> {
            final Location location = controller.getLocation();

            colorConsumer.accept(location.getColor());
        });
    }

    public LocationController getController() {
        return controller;
    }

    private void initializeLocationShapes(final Path locationShape, final double radius) {
        final double c = 0.551915024494;
        final double circleToOctagonLineRatio = 0.35;

        final MoveTo moveTo = new MoveTo();
        moveTo.xProperty().bind(animation.multiply(circleToOctagonLineRatio * radius));
        moveTo.yProperty().set(radius);

        final CubicCurveTo cc1 = new CubicCurveTo();
        cc1.controlX1Property().bind(reverseAnimation.multiply(c * radius).add(animation.multiply(circleToOctagonLineRatio * radius)));
        cc1.controlY1Property().bind(reverseAnimation.multiply(radius).add(animation.multiply(radius)));
        cc1.controlX2Property().bind(reverseAnimation.multiply(radius).add(animation.multiply(radius)));
        cc1.controlY2Property().bind(reverseAnimation.multiply(c * radius).add(animation.multiply(circleToOctagonLineRatio * radius)));
        cc1.setX(radius);
        cc1.yProperty().bind(animation.multiply(circleToOctagonLineRatio * radius));


        final LineTo lineTo1 = new LineTo();
        lineTo1.xProperty().bind(cc1.xProperty());
        lineTo1.yProperty().bind(cc1.yProperty().multiply(-1));

        final CubicCurveTo cc2 = new CubicCurveTo();
        cc2.controlX1Property().bind(cc1.controlX2Property());
        cc2.controlY1Property().bind(cc1.controlY2Property().multiply(-1));
        cc2.controlX2Property().bind(cc1.controlX1Property());
        cc2.controlY2Property().bind(cc1.controlY1Property().multiply(-1));
        cc2.xProperty().bind(moveTo.xProperty());
        cc2.yProperty().bind(moveTo.yProperty().multiply(-1));


        final LineTo lineTo2 = new LineTo();
        lineTo2.xProperty().bind(cc2.xProperty().multiply(-1));
        lineTo2.yProperty().bind(cc2.yProperty());

        final CubicCurveTo cc3 = new CubicCurveTo();
        cc3.controlX1Property().bind(cc2.controlX2Property().multiply(-1));
        cc3.controlY1Property().bind(cc2.controlY2Property());
        cc3.controlX2Property().bind(cc2.controlX1Property().multiply(-1));
        cc3.controlY2Property().bind(cc2.controlY1Property());
        cc3.xProperty().bind(lineTo1.xProperty().multiply(-1));
        cc3.yProperty().bind(lineTo1.yProperty());


        final LineTo lineTo3 = new LineTo();
        lineTo3.xProperty().bind(cc3.xProperty());
        lineTo3.yProperty().bind(cc3.yProperty().multiply(-1));

        final CubicCurveTo cc4 = new CubicCurveTo();
        cc4.controlX1Property().bind(cc3.controlX2Property());
        cc4.controlY1Property().bind(cc3.controlY2Property().multiply(-1));
        cc4.controlX2Property().bind(cc3.controlX1Property());
        cc4.controlY2Property().bind(cc3.controlY1Property().multiply(-1));
        cc4.xProperty().bind(lineTo2.xProperty());
        cc4.yProperty().bind(lineTo2.yProperty().multiply(-1));


        final LineTo lineTo4 = new LineTo();
        lineTo4.xProperty().bind(moveTo.xProperty());
        lineTo4.yProperty().bind(moveTo.yProperty());


        locationShape.getElements().add(moveTo);
        locationShape.getElements().add(cc1);

        locationShape.getElements().add(lineTo1);
        locationShape.getElements().add(cc2);

        locationShape.getElements().add(lineTo2);
        locationShape.getElements().add(cc3);

        locationShape.getElements().add(lineTo3);
        locationShape.getElements().add(cc4);

        locationShape.getElements().add(lineTo4);
    }
}
