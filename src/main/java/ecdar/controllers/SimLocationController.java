package ecdar.controllers;

import EcdarProtoBuf.ObjectProtos;
import com.jfoenix.controls.JFXPopup;
import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.presentations.DropDownMenu;
import ecdar.presentations.SimLocationPresentation;
import ecdar.presentations.SimTagPresentation;
import ecdar.abstractions.State;
import ecdar.presentations.SimulationPresentation;
import ecdar.utility.colors.EnabledColor;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.function.Consumer;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * The controller of a location shown in the {@link SimulationPresentation}
 */
public class SimLocationController implements Initializable {
    private final ObjectProperty<Location> location = new SimpleObjectProperty<>();
    private final ObjectProperty<Component> component = new SimpleObjectProperty<>();
    public SimLocationPresentation root;
    public Path notCommittedShape;
    public Path notCommittedInitialIndicator;
    public Group shakeContent;
    public Circle circle;
    public Circle circleShakeIndicator;
    public Group scaleContent;
    public SimTagPresentation nicknameTag;
    public SimTagPresentation invariantTag;
    public Label idLabel;
    public Line nameTagLine;
    public Line invariantTagLine;
    private DropDownMenu dropDownMenu;

    public static String getSimLocationReachableQuery(final Location endLocation, final Component component, final String query) {
        return getSimLocationReachableQuery(endLocation, component, query, null);
    }

    /**
         * Generates a reachability query based on the given location and component
         *
         * @param endLocation  The location which should be checked for reachability
         * @return A reachability query string
         */
    public static String getSimLocationReachableQuery(final Location endLocation, final Component component, final String query, final State state) {
        var stringBuilder = new StringBuilder();

        // append simulation query
        stringBuilder.append(query);

        // append arrow
        stringBuilder.append(" -> ");

        // ToDo: append start location here
        if (state != null){
            stringBuilder.append(getInitialStateString(state));
            stringBuilder.append(";");
        }

        // append end state
        stringBuilder.append(getEndStateString(component.getName(), endLocation.getId()));

        //  return example: m1||M2->[L1,L4](y<3);[L2, L7](y<2)
        System.out.println(stringBuilder);
        return stringBuilder.toString();
    }

    /**
     * ToDo NIELS: Determine if this is actually what it does
     * Returns a string representation of an array of initial location IDs for all components in the simulation
     *
     * @param state
     * @return
     */
    private static String getInitialStateString(State state) {
        var initialStateStringBuilder = new StringBuilder();
        var locations = new ArrayList<Pair<ObjectProtos.ComponentInstance, String>>();

        state.consumeLeafLocations((leafLocation -> locations.add(new Pair<>(leafLocation.getComponentInstance(), leafLocation.getId()))));

        // append locations
        initialStateStringBuilder.append("[");
        var appendLocationWithSeparator = false;

        // ToDO NIELS: Determine how to process this, if it is indeed the initial locations
        for(var component : SimulationController.getSimulatedComponents()){
            var locationFound = false;

            for(var location : locations){
                if (location.getKey().getComponentName().equals(component.getName())){
                    if (appendLocationWithSeparator){
                        initialStateStringBuilder.append(",")
                                .append(location.getValue());
                    }
                    else{
                        initialStateStringBuilder.append(location.getValue());
                    }
                    locationFound = true;
                }
                if (locationFound){
                    // don't go through more locations, when a location is found for the specific component that we're looking at
                    break;
                }
            }
            appendLocationWithSeparator = true;
        }
        initialStateStringBuilder.append("]");

        // ToDo: append clock values
        var clocks = state.getSimulationClocks();
        initialStateStringBuilder.append("()");

        return initialStateStringBuilder.toString();
    }

    private static String getEndStateString(String componentName, String endLocationId) {
        var stringBuilder = new StringBuilder();

        stringBuilder.append("[");
        var appendLocationWithSeparator = false;

        for (var component : SimulationController.getSimulatedComponents())
        {
            if (component.getName().equals(componentName)){
                if (appendLocationWithSeparator){
                    stringBuilder.append(",")
                            .append(endLocationId);
                }
                else{
                    stringBuilder.append(endLocationId);
                }
            }
            else{ // add underscore to indicate, that we don't care about the end locations in the other components
                if (appendLocationWithSeparator){
                    stringBuilder.append(",_");
                }
                else{
                    stringBuilder.append("_");
                }
            }
            if (!appendLocationWithSeparator) {
                appendLocationWithSeparator = true;
            }
        }
        stringBuilder.append("]");
        stringBuilder.append("()");

        return stringBuilder.toString();
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        this.location.addListener((obsLocation, oldLocation, newLocation) -> {
            // The radius property on the abstraction must reflect the radius in the view
            newLocation.radiusProperty().bind(circle.radiusProperty());

            // The scale property on the abstraction must reflect the radius in the view
            newLocation.scaleProperty().bind(scaleContent.scaleXProperty());
        });

        // Scale x and y 1:1 (based on the x-scale)
        scaleContent.scaleYProperty().bind(scaleContent.scaleXProperty());
        initializeMouseControls();
    }

    private void initializeMouseControls() {
        final Consumer<MouseEvent> mouseClicked = (event) -> {
            if (root.isPlaced()) {
                if (event.getButton().equals(MouseButton.SECONDARY)) {
                    initializeDropDownMenu();
                    dropDownMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 0, 0);
                }
            }

        };
        locationProperty().addListener((obs, oldLocation, newLocation) -> {
            if(newLocation == null) return;
            root.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseClicked::accept);
        });
    }

    /**
     * Creates the dropdown when right clicking a location.
     * When reachability is chosen a request will be send to the backend to see if the location can be reached
     */
    public void initializeDropDownMenu(){
        dropDownMenu = new DropDownMenu(root);

        String composition;
        State currentState;

        try {
            composition = SimulationController.getComposition();
            currentState = SimulationController.getCurrentState();
        } catch (NullPointerException e) {
            Ecdar.showToast("Unable to inittialize dropdown due to null simulation");
            return;
        }

        dropDownMenu.addClickableListElement("Is " + getLocation().getId() + " reachable from initial state?", event -> {
            // Generate the query from the backend
            final String reachabilityQuery = getSimLocationReachableQuery(getLocation(), getComponent(), composition);

            // Add proper comment
            final String reachabilityComment = "Is " + getLocation().getMostDescriptiveIdentifier() + " reachable from initial state?";

            // Add new query for this location
            final Query query = new Query(reachabilityQuery, reachabilityComment, QueryState.UNKNOWN);
            query.setType(QueryType.REACHABILITY);

            // execute query
            query.execute();

            dropDownMenu.hide();
        });

        dropDownMenu.addClickableListElement("Is " + getLocation().getId() + " reachable from current locations?", event -> {
            // Generate the query from the backend ToDo NIELS: Remove static methods
            final String reachabilityQuery = getSimLocationReachableQuery(getLocation(), getComponent(), composition, currentState);

            // Add proper comment
            final String reachabilityComment = "Is " + getLocation().getMostDescriptiveIdentifier() + " reachable from current locations?";

            // Add new query for this location
            final Query query = new Query(reachabilityQuery, reachabilityComment, QueryState.UNKNOWN);
            query.setType(QueryType.REACHABILITY);

            // execute query
            query.execute();

            dropDownMenu.hide();
        });
    }

    public Location getLocation() {
        return location.get();
    }

    /**
     * Set/places the given location on the view.
     * This have to be done before adding the {@link SimLocationPresentation} to the view as nothing
     * would then be displayed.
     * @param location the location
     */
    public void setLocation(final Location location) {
        this.location.set(location);
        root.setLayoutX(location.getX());
        root.setLayoutY(location.getY());
        location.xProperty().bindBidirectional(root.layoutXProperty());
        location.yProperty().bindBidirectional(root.layoutYProperty());
    }

    public ObjectProperty<Location> locationProperty() {
        return location;
    }

    public Component getComponent() {
        return component.get();
    }

    public void setComponent(final Component component) {
        this.component.set(component);
    }

    public ObjectProperty<Component> componentProperty() {
        return component;
    }

    /**
     * Colors the location model
     * @param color the new color of the location
     */
    public void color(final EnabledColor color) {
        final Location location = getLocation();

        // Set the color of the location
        location.setColor(color);
    }

    public EnabledColor getColor() {
        return getLocation().getColor();
    }

    public DoubleProperty xProperty() {
        return root.layoutXProperty();
    }

    public DoubleProperty yProperty() {
        return root.layoutYProperty();
    }

    public double getX() {
        return xProperty().get();
    }

    public double getY() {
        return yProperty().get();
    }
}
