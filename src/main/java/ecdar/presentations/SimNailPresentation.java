package ecdar.presentations;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.abstractions.Nail;
import ecdar.controllers.SimEdgeController;
import ecdar.controllers.SimNailController;
import ecdar.utility.Highlightable;
import ecdar.utility.colors.Color;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.helpers.BindingHelper;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.shape.Line;

import java.util.function.Consumer;

import static javafx.util.Duration.millis;

/**
 * The presentation for the nail shown on a {@link SimEdgePresentation} in the {@link SimulatorOverviewPresentation}<br />
 * This class should be refactored such that code which are duplicated from {@link NailPresentation}
 * have its own base class.
 */
public class SimNailPresentation extends Group implements Highlightable {

    public static final double COLLAPSED_RADIUS = 2d;
    public static final double HOVERED_RADIUS = 7d;

    private final SimNailController controller;
    private final Timeline shakeAnimation = new Timeline();

    /**
     * Constructs a nail which is ready to be displayed in the simulator
     * @param nail the nail which should be presented
     * @param edge the edge where the nail belongs to
     * @param component the component where the nail belongs
     * @param simEdgeController the controller of the edge where the nail belongs to
     */
    public SimNailPresentation(final Nail nail, final Edge edge, final Component component, final SimEdgeController simEdgeController) {
        controller = new EcdarFXMLLoader().loadAndGetController("SimNailPresentation.fxml", this);

        // Bind the component with the one of the controller
        controller.setComponent(component);

        // Bind the edge with the one of the controller
        controller.setEdge(edge);
        // Bind the nail with the one of the controller
        controller.setNail(nail);

        controller.setEdgeController(simEdgeController);

        initializeNailCircleColor();
        initializePropertyTag();
        initializeRadius();
        initializeShakeAnimation();
    }

    /**
     * Sets the radius of the nail
     */
    private void initializeRadius() {
        final Consumer<Edge.PropertyType> radiusUpdater = (propertyType) -> {
            if(!propertyType.equals(Edge.PropertyType.NONE)) {
                controller.getNail().setRadius(SimNailPresentation.HOVERED_RADIUS);
            }
        };
        controller.getNail().propertyTypeProperty().addListener((observable, oldValue, newValue) -> {
            radiusUpdater.accept(newValue);
        });
        radiusUpdater.accept(controller.getNail().getPropertyType());
    }

    /**
     * Initializes the text / PropertyTag shown on a {@link SimTagPresentation} on the nail.
     */
    private void initializePropertyTag() {
        final SimTagPresentation propertyTag = controller.propertyTag;
        final Line propertyTagLine = controller.propertyTagLine;
        propertyTag.setComponent(controller.getComponent());
        propertyTag.setLocationAware(controller.getNail());

        // Bind the line to the tag
        BindingHelper.bind(propertyTagLine, propertyTag);

        // Bind the color of the tag to the color of the component
        propertyTag.bindToColor(controller.getComponent().colorProperty());

        // Updates visibility and placeholder of the tag depending on the type of nail
        final Consumer<Edge.PropertyType> updatePropertyType = (propertyType) -> {

            // If it is not a property nail hide the tag otherwise show it and write proper placeholder
            if(propertyType.equals(Edge.PropertyType.NONE)) {
                propertyTag.setVisible(false);
            } else {

                // Show the property tag since the nail is a property nail
                propertyTag.setVisible(true);

                // Set and bind the location of the property tag
                if((controller.getNail().getPropertyX() != 0) && (controller.getNail().getPropertyY() != 0)) {
                    propertyTag.setTranslateX(controller.getNail().getPropertyX());
                    propertyTag.setTranslateY(controller.getNail().getPropertyY());
                }
                controller.getNail().propertyXProperty().bind(propertyTag.translateXProperty());
                controller.getNail().propertyYProperty().bind(propertyTag.translateYProperty());

                final Label propertyLabel = controller.propertyLabel;

                if(propertyType.equals(Edge.PropertyType.SELECTION)) {
                    propertyLabel.setText(":");
                    propertyLabel.setTranslateX(-3);
                    propertyLabel.setTranslateY(-8);
                    propertyTag.setAndBindString(controller.getEdge().selectProperty());
                } else if(propertyType.equals(Edge.PropertyType.GUARD)) {
                    propertyLabel.setText("<");
                    propertyLabel.setTranslateX(-3);
                    propertyLabel.setTranslateY(-7);
                    propertyTag.setAndBindString(controller.getEdge().guardProperty());
                } else if(propertyType.equals(Edge.PropertyType.SYNCHRONIZATION)) {
                    updateSyncLabel();
                    propertyLabel.setTranslateX(-3);
                    propertyLabel.setTranslateY(-7);
                    propertyTag.setAndBindString(controller.getEdge().syncProperty());
                } else if(propertyType.equals(Edge.PropertyType.UPDATE)) {
                    propertyLabel.setText("=");
                    propertyLabel.setTranslateX(-3);
                    propertyLabel.setTranslateY(-7);
                    propertyTag.setAndBindString(controller.getEdge().updateProperty());
                }

                //Disable the ability to edit the tag if the nails edge is locked
                if(controller.getEdge().getIsLockedProperty().getValue()){
                    propertyTag.setDisabledText(true);
                }
            }
        };

        // Whenever the property type updates, update the tag
        controller.getNail().propertyTypeProperty().addListener((obs, oldPropertyType, newPropertyType) -> {
            updatePropertyType.accept(newPropertyType);
        });

        // Whenever the edge changes I/O status, if sync nail then update its label
        controller.getEdge().ioStatus.addListener((observable, oldValue, newValue) -> {
            if (controller.getNail().getPropertyType().equals(Edge.PropertyType.SYNCHRONIZATION))
                updateSyncLabel();
        });

        // Update the tag initially
        updatePropertyType.accept(controller.getNail().getPropertyType());
    }

    /**
     * Updates the synchronization label and tag.
     * The update depends on the edge I/O status.
     */
    private void updateSyncLabel() {
        final Label propertyLabel = controller.propertyLabel;

        // show ? or ! dependent on edge I/O status
        if (controller.getEdge().ioStatus.get().equals(EdgeStatus.INPUT)) {
            propertyLabel.setText("?");
        } else {
            propertyLabel.setText("!");
        }
    }

    /**
     * Set up Listeners for updating the color of the nail, set the color initially
     */
    private void initializeNailCircleColor() {
        final Runnable updateNailColor = () -> {
            final EnabledColor color = controller.getComponent().getColor();

            if(!controller.getNail().getPropertyType().equals(Edge.PropertyType.NONE)) {
                controller.nailCircle.setFill(color.getPaintColor());
                controller.nailCircle.setStroke(color.getStrokeColor());
            } else {
                controller.nailCircle.setFill(Color.GREY_BLUE.getColor(Color.Intensity.I800));
                controller.nailCircle.setStroke(Color.GREY_BLUE.getColor(Color.Intensity.I900));
            }
        };

        // When the color of the component updates, update the nail indicator as well
        controller.getComponent().colorProperty().addListener((observable) -> updateNailColor.run());

        // Initialize the color of the nail
        updateNailColor.run();
    }

    /**
     * Initializes a shake animation found in {@link SimNailPresentation#shakeAnimation} which can
     * be played using {@link SimNailPresentation#shake()}
     */
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

    /**
     * Plays the {@link SimNailPresentation#shakeAnimation}
     */
    public void shake() {
        shakeAnimation.play();
    }

    /**
     * Highlights the nail
     */
    @Override
    public void highlight() {
        final Color color = Color.DEEP_ORANGE;
        final Color.Intensity intensity = Color.Intensity.I500;

        // Set the color
        controller.nailCircle.setFill(color.getColor(intensity));
        controller.nailCircle.setStroke(color.getColor(intensity.next(2)));
    }

    @Override
    public void highlightPurple() {
        final Color color1 = Color.DEEP_PURPLE;
        final Color.Intensity intensity = Color.Intensity.I500;

        // Set the color
        controller.nailCircle.setFill(color1.getColor(intensity));
        controller.nailCircle.setStroke(color1.getColor(intensity.next(2)));
    }

    /**
     * Removes the highlight from the nail
     */
    @Override
    public void unhighlight() {
        EnabledColor color = new EnabledColor(Color.GREY_BLUE, Color.Intensity.I800);

        // Set the color
        if(!controller.getNail().getPropertyType().equals(Edge.PropertyType.NONE)) {
            color = controller.getComponent().getColor();
        }

        controller.nailCircle.setFill(color.getPaintColor());
        controller.nailCircle.setStroke(color.getStrokeColor());
    }
}
