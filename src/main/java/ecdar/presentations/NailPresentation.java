package ecdar.presentations;

import ecdar.abstractions.*;
import ecdar.controllers.EdgeController;
import ecdar.controllers.NailController;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.BindingHelper;
import ecdar.utility.Highlightable;
import ecdar.utility.helpers.SelectHelper;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.shape.Line;

import java.util.function.Consumer;

import static javafx.util.Duration.millis;

public class NailPresentation extends Group implements SelectHelper.Selectable, Highlightable {

    public static final double COLLAPSED_RADIUS = 2d;
    public static final double HOVERED_RADIUS = 7d;

    private final NailController controller;
    private final Timeline shakeAnimation = new Timeline();

    public NailPresentation(final Nail nail, final DisplayableEdge edge, final Component component, final EdgeController edgeController) {
        controller = new EcdarFXMLLoader().loadAndGetController("NailPresentation.fxml", this);

        // Bind the component with the one of the controller
        controller.setComponent(component);

        // Bind the edge with the one of the controller
        controller.setEdge(edge);

        // Bind the nail with the one of the controller
        controller.setNail(nail);

        controller.setEdgeController(edgeController);

        Platform.runLater(() -> {
            if (controller.getNail().getPropertyType() == DisplayableEdge.PropertyType.SYNCHRONIZATION && edge instanceof GroupedEdge) {
                TagPresentation currentTagPresentation = getController().propertyTag;
                controller.propertyTag = new MultiSyncTagPresentation((GroupedEdge) edge, () -> updateSyncLabel(currentTagPresentation));
            } else {
                controller.propertyTag = new TagPresentation();
            }

            controller.propertyTag.setTranslateX(10);
            controller.propertyTag.replaceSigns();
            controller.propertyTag.setTranslateY(-controller.propertyTag.getHeight());
            this.getChildren().add(controller.propertyTag);

            initializeNailCircleColor();
            initializePropertyTag();
            initializeRadius();
            initializeShakeAnimation();
        });
    }

    private void initializeRadius() {
        final Consumer<Edge.PropertyType> radiusUpdater = (propertyType) -> {
            if(!propertyType.equals(Edge.PropertyType.NONE)) {
                controller.getNail().setRadius(NailPresentation.HOVERED_RADIUS);
            }
        };

        controller.getNail().propertyTypeProperty().addListener((observable, oldValue, newValue) -> {
            radiusUpdater.accept(newValue);
        });

        radiusUpdater.accept(controller.getNail().getPropertyType());
    }
    private void initializePropertyTag() {
        final TagPresentation propertyTag = controller.propertyTag;
        final Line propertyTagLine = controller.propertyTagLine;
        setPropertyTagComponentAndLocationAware(propertyTag, propertyTagLine);

        // Updates visibility and placeholder of the tag depending on the type of nail
        final Consumer<Edge.PropertyType> updatePropertyType = (propertyType) -> {

            // If it is not a property nail hide the tag otherwise show it and write proper placeholder
            if(propertyType.equals(Edge.PropertyType.NONE)) {
                propertyTag.setVisible(false);
            } else {

                // Show the property tag since the nail is a property nail
                propertyTag.setVisible(true);

                // Set and bind the location of the property tag
                if((controller.getNail().getPropertyX() != 0) || (controller.getNail().getPropertyY() != 0)) {
                    propertyTag.setTranslateX(controller.getNail().getPropertyX());
                    propertyTag.setTranslateY(controller.getNail().getPropertyY());
                }

                // Check is needed because the property cannot be bound twice
                // which happens when switching from the simulator to the editor
                if (!controller.getNail().propertyXProperty().isBound() && !controller.getNail().propertyYProperty().isBound()) {
                    controller.getNail().propertyXProperty().bindBidirectional(propertyTag.translateXProperty());
                    controller.getNail().propertyYProperty().bindBidirectional(propertyTag.translateYProperty());
                }
                    
                Label propertyLabel = controller.propertyLabel;

                if(propertyType.equals(Edge.PropertyType.SELECTION)) {
                    propertyTag.setPlaceholder("Select");
                    propertyLabel.setText(":");
                    propertyLabel.setTranslateX(-3);
                    propertyLabel.setTranslateY(-8);
                    propertyTag.setAndBindString(controller.getEdge().selectProperty());
                } else if(propertyType.equals(Edge.PropertyType.GUARD)) {
                    propertyTag.setPlaceholder("Guard");
                    propertyLabel.setText("<");
                    propertyLabel.setTranslateX(-3);
                    propertyLabel.setTranslateY(-7);
                    propertyTag.setAndBindString(controller.getEdge().guardProperty());
                } else if(propertyType.equals(Edge.PropertyType.SYNCHRONIZATION)) {
                    updateSyncLabel(propertyTag);
                    propertyLabel.setTranslateX(-3);
                    propertyLabel.setTranslateY(-7);
                    if (controller.getEdge() instanceof Edge)
                        propertyTag.setAndBindString(((Edge) controller.getEdge()).syncProperty());
                } else if(propertyType.equals(Edge.PropertyType.UPDATE)) {
                    propertyTag.setPlaceholder("Update");
                    propertyLabel.setText("=");
                    propertyLabel.setTranslateX(-3);
                    propertyLabel.setTranslateY(-7);
                    propertyTag.setAndBindString(controller.getEdge().updateProperty());
                }

                //Disable the ability to edit the tag if the nails edge is locked
                if(controller.getEdge().getIsLockedProperty().getValue()){
                    propertyTag.setDisabledText(true);
                }

                propertyTag.requestTextFieldFocus();
            }
        };

        // Whenever the property type updates, update the tag
        controller.getNail().propertyTypeProperty().addListener((obs, oldPropertyType, newPropertyType) -> {
            updatePropertyType.accept(newPropertyType);
        });

        // Whenever the edge changes I/O status, if sync nail then update its label
        controller.getEdge().ioStatus.addListener((observable, oldValue, newValue) -> {
            if (controller.getNail().getPropertyType().equals(Edge.PropertyType.SYNCHRONIZATION))
                updateSyncLabel(propertyTag);
        });

        // Update the tag initially
        updatePropertyType.accept(controller.getNail().getPropertyType());
    }

    private void setPropertyTagComponentAndLocationAware(TagPresentation propertyTag, Line propertyTagLine) {
        propertyTag.setComponent(controller.getComponent());
        propertyTag.setLocationAware(controller.getNail());

        // Bind the line to the tag
        BindingHelper.bind(propertyTagLine, propertyTag);

        // Bind the color of the tag to the color of the component
        propertyTag.bindToColor(controller.getComponent().colorProperty(), controller.getComponent().colorIntensityProperty());
    }

    /**
     * Updates the synchronization label and tag.
     * The update depends on the edge I/O status.
     * @param propertyTag Property tag to update
     */
    private void updateSyncLabel(final TagPresentation propertyTag) {
        final Label propertyLabel = controller.propertyLabel;

        // show ? or ! dependent on edge I/O status
        if (controller.getEdge().ioStatus.get().equals(EdgeStatus.INPUT)) {
            propertyLabel.setText("?");
            propertyTag.setPlaceholder("Input");
        } else {
            propertyLabel.setText("!");
            propertyTag.setPlaceholder("Output");
        }
    }

    private void initializeNailCircleColor() {
        // When the color of the component updates, update the nail indicator as well
        controller.getComponent().colorProperty().addListener((observable) -> updateNailColor());

        // When the color intensity of the component updates, update the nail indicator
        controller.getComponent().colorIntensityProperty().addListener((observable) -> updateNailColor());
        // Initialize the color of the nail
        updateNailColor();
    }

    /**
     * Update color when the edge of this nails failing property is updated.
     */
    public void onFailingUpdate(boolean isFailing) {
        final Runnable updateNailColorOnFailingUpdate = () -> {
            final Color color = controller.getComponent().getColor();
            final Color.Intensity colorIntensity = controller.getComponent().getColorIntensity();
            controller.nailCircle.setFill(Color.RED.getColor(colorIntensity));
            controller.nailCircle.setStroke(Color.RED.getColor(colorIntensity.next(2)));
        };
        if (isFailing) {
            updateNailColorOnFailingUpdate.run();
        } else {
            updateNailColor();
        }
    }

    private void updateNailColor() {
        final Runnable updateNailColor = () -> {
            final Color color = controller.getComponent().getColor();
            final Color.Intensity colorIntensity = controller.getComponent().getColorIntensity();
            //If edge is failing and is a SYNC
            if (controller.getEdge().getFailing() && controller.getNail().getPropertyType().equals(Edge.PropertyType.SYNCHRONIZATION)) {
                controller.nailCircle.setFill(Color.RED.getColor(colorIntensity));
                controller.nailCircle.setStroke(Color.RED.getColor(colorIntensity.next(2)));
            }
            //If edge is not NONE
            else if (!controller.getNail().getPropertyType().equals(Edge.PropertyType.NONE)) {
                controller.nailCircle.setFill(color.getColor(colorIntensity));
                controller.nailCircle.setStroke(color.getColor(colorIntensity.next(2)));
            } else {
                controller.nailCircle.setFill(Color.GREY_BLUE.getColor(Color.Intensity.I800));
                controller.nailCircle.setStroke(Color.GREY_BLUE.getColor(Color.Intensity.I900));
            }
        };
        updateNailColor.run();
    }

    private void initializeShakeAnimation() {
        final Interpolator interpolator = Interpolator.SPLINE(0.645, 0.045, 0.355, 1);

        final double startX = controller.root.getTranslateX();
        final KeyValue kv1 = new KeyValue(controller.root.translateXProperty(), startX - 3, interpolator);
        final KeyValue kv2 = new KeyValue(controller.root.translateXProperty(), startX + 3, interpolator);
        final KeyValue kv3 = new KeyValue(controller.root.translateXProperty(), startX, interpolator);

        final KeyFrame kf1 = new KeyFrame(millis(50), kv1);
        final KeyFrame kf2 = new KeyFrame(millis(100), kv2);
        final KeyFrame kf3 = new KeyFrame(millis(150), kv1);
        final KeyFrame kf4 = new KeyFrame(millis(200), kv2);
        final KeyFrame kf5 = new KeyFrame(millis(250), kv3);

        shakeAnimation.getKeyFrames().addAll(kf1, kf2, kf3, kf4, kf5);
    }

    public void shake() {
        shakeAnimation.play();
    }

    @Override
    public void select() {
        final Color color = Color.DEEP_ORANGE;
        final Color.Intensity intensity = Color.Intensity.I500;

        // Set the color
        controller.nailCircle.setFill(color.getColor(intensity));
        controller.nailCircle.setStroke(color.getColor(intensity.next(2)));
    }

    @Override
    public void deselect() {
        updateNailColor();
    }

    public NailController getController() {
        return controller;
    }

    /***
     * Highlights the nail
     */
    @Override
    public void highlight() {
        this.select();
    }

    /***
     * Removes the highlight from the nail
     */
    @Override
    public void unhighlight() {
        this.deselect();
    }
}
