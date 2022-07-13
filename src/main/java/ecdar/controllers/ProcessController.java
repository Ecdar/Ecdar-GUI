package ecdar.controllers;

import com.jfoenix.controls.JFXRippler;
import ecdar.abstractions.*;
import ecdar.presentations.SimEdgePresentation;
import ecdar.presentations.SimLocationPresentation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.net.URL;
import java.util.*;

/**
 * The controller for the process shown in the {@link SimulatorOverviewController}
 */
public class ProcessController extends ModelController implements Initializable {
    public StackPane componentPane;
    public Pane modelContainerEdge;
    public Pane modelContainerLocation;
    public JFXRippler toggleValuesButton;
    public VBox valueArea;
    public FontIcon toggleValueButtonIcon;
    private ObjectProperty<Component> component;

    /**
     * Keep track of locations/edges and their associated presentation class, by having the as key-value pairs in a Map
     * E.g. a {@link Location} key is the model behind the value {@link SimLocationPresentation} view
     */
    private final Map<Location, SimLocationPresentation> locationPresentationMap = new HashMap<>();
    private final Map<Edge, SimEdgePresentation> edgePresentationMap = new HashMap<>();
    private final ObservableMap<String, BigDecimal> variables = FXCollections.observableHashMap();
    private final ObservableMap<String, BigDecimal> clocks = FXCollections.observableHashMap();

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        component = new SimpleObjectProperty<>(new Component(true));
        initializeValues();
    }

    private void initializeValues() {
        final Circle circle = new Circle(0);
        if (getComponent().isDeclarationOpen()) {
            circle.setRadius(1000);
        }
        final ObjectProperty<Node> clip = new SimpleObjectProperty<>(circle);
        valueArea.clipProperty().bind(clip);
        clip.set(circle);
    }

    /**
     * Highlights the edges and accompanying source/target locations in the process
     * @param edges The edges to highlight
     */
    public void highlightEdges(final Edge[] edges) {
        for (int i = 0; i < edges.length; i++) {
            final Edge edge = edges[i];

            final Location source = edge.getSourceLocation();
            String sourceId = source.getId();
            final Location target = edge.getTargetLocation();
            String targetId = target.getId();

            // If target name is empty the edge is a self loop
            if (Objects.equals(targetId, "")) {
                targetId = sourceId;
            }

            boolean isSourceUniversal = false;

            // Iterate through all locations to check for Universal and Inconsistent locations
            // The name of a Universal location may be "U2" in our system, but it is mapped to "Universal" in the engine
            // This loop maps "Universal" to for example "U2"
            for (Map.Entry<Location, SimLocationPresentation> locEntry: locationPresentationMap.entrySet()) {
                if(locEntry.getKey().getType() == Location.Type.UNIVERSAL) {
                    if(sourceId.equals("Universal")) {
                        sourceId = locEntry.getKey().getId();
                        isSourceUniversal = true;
                    }

                    if(targetId.equals("Universal")) {
                        targetId = locEntry.getKey().getId();
                    }
                }

                if(locEntry.getKey().getType() == Location.Type.INCONSISTENT) {
                    if(sourceId.equals("Inconsistent")) {
                        sourceId = locEntry.getKey().getId();
                    }

                    if(targetId.equals("Inconsistent")) {
                        targetId = locEntry.getKey().getId();
                    }
                }
            }

            // Self loop on a Universal locations means that the edge name should be mapped to *
            String edgeId = edge.getId();
            if (isSourceUniversal && sourceId.equals(targetId)) {
                edgeId = "*";
            }

            highlightEdge(edgeId, edge.getStatus(), sourceId, targetId);
        }
    }

    /**
     * Unhighlights all edges and locations in the process
     */
    public void unhighlightProcess() {
        edgePresentationMap.forEach((key, value) -> value.getController().unhighlight());
        locationPresentationMap.forEach((key, value) -> value.unhighlight());
    }

    /**
     * Helper method that finds the {@link SimLocationPresentation} and highlights it.
     * Calls {@link ProcessController#highlightEdgeLocations(String, String)} to highlight the source/targets locations
     * @param edgeName The name of the edge
     * @param edgeStatus The status (input/output) of the edge to highlight
     * @param sourceName The name of the source location
     * @param targetName The name of the target location
     */
    private void highlightEdge(final String edgeName, final EdgeStatus edgeStatus, final String sourceName, final String targetName) {
        for (Map.Entry<Edge, SimEdgePresentation> entry: edgePresentationMap.entrySet()) {
            final String keyName = entry.getKey().getSync();
            final String keySourceId = entry.getKey().getSourceLocation().getId();
            final String keyTargetId = entry.getKey().getTargetLocation().getId();

            // Multiple edges may have the same name, so we also check that the source and target match this edge
            if(keyName.equals(edgeName) &&
                    keySourceId.equals(sourceName) &&
                    keyTargetId.equals(targetName) &&
                    entry.getKey().ioStatus.get() == edgeStatus) {

                entry.getValue().getController().highlight();
                highlightEdgeLocations(keySourceId, keyTargetId);
            }
        }
    }

    /**
     * Helper method that finds the source/target {@link SimLocationPresentation} and highlights it
     * @param sourceId The name of the source location
     * @param targetId The name of the target location
     */
    private void highlightEdgeLocations(final String sourceId, final String targetId) {
        for (Map.Entry<Location, SimLocationPresentation> locEntry: locationPresentationMap.entrySet()) {
            final String locationId = locEntry.getKey().getId();

            // Check if location is either source or target and highlight it
            if(locationId.equals(sourceId) || locationId.equals(targetId)) {
                locEntry.getValue().highlight();
            }
        }
    }

    /**
     * Method that highlights all locations with the input ID
     * @param locationId The locations to highlight
     */
    public void highlightLocation(final String locationId) {
        for (Map.Entry<Location, SimLocationPresentation> locEntry: locationPresentationMap.entrySet()) {
            if(locEntry.getKey().getId().equals(locationId)) {
                locEntry.getValue().highlight();
            }
        }
    }

    /**
     * Sets the component which is going to be shown as a process. <br />
     * This also initializes the rest of the views needed for the process to be shown properly
     * @param component the component of the process
     */
    public void setComponent(final Component component){
        this.component.set(component);
        modelContainerEdge.getChildren().clear();
        modelContainerLocation.getChildren().clear();

        component.getLocations().forEach(location -> {
            final SimLocationPresentation lp = new SimLocationPresentation(location, component);
            modelContainerLocation.getChildren().add(lp);
            locationPresentationMap.put(location, lp);
        });

        component.getEdges().forEach(edge -> {
            final SimEdgePresentation ep = new SimEdgePresentation(edge, component);
            modelContainerEdge.getChildren().add(ep);
            edgePresentationMap.put(edge, ep);
        });
    }

    public void toggleValues(final MouseEvent mouseEvent) {
        final Circle circle = new Circle(0);
        circle.setCenterX(component.get().getBox().getWidth() - (toggleValuesButton.getWidth() - mouseEvent.getX()));
        circle.setCenterY(-1 * mouseEvent.getY());

        final ObjectProperty<Node> clip = new SimpleObjectProperty<>(circle);
        valueArea.clipProperty().bind(clip);

        final Transition rippleEffect = new Transition() {
            private final double maxRadius = Math.sqrt(Math.pow(getComponent().getBox().getWidth(), 2) + Math.pow(getComponent().getBox().getHeight(), 2));
            {
                setCycleDuration(Duration.millis(500));
            }

            protected void interpolate(final double fraction) {
                if (getComponent().isDeclarationOpen()) {
                    circle.setRadius(fraction * maxRadius);
                } else {
                    circle.setRadius(maxRadius - fraction * maxRadius);
                }
                clip.set(circle);
            }
        };

        final Interpolator interpolator = Interpolator.SPLINE(0.785, 0.135, 0.15, 0.86);
        rippleEffect.setInterpolator(interpolator);

        rippleEffect.play();
        getComponent().declarationOpenProperty().set(!getComponent().isDeclarationOpen());
    }

    /**
     * Gets the component linked to this process
     * @return the component of the process
     */
    public Component getComponent(){
        return component.get();
    }

    @Override
    public HighLevelModelObject getModel() {
        return component.get();
    }

    public ObservableMap<String, BigDecimal> getVariables() {
        return variables;
    }

    public ObservableMap<String, BigDecimal> getClocks() {
        return clocks;
    }
}
