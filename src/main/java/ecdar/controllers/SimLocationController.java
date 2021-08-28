package ecdar.controllers;

import ecdar.abstractions.Component;
import ecdar.abstractions.Location;
import ecdar.presentations.SimLocationPresentation;
import ecdar.presentations.SimTagPresentation;
import ecdar.utility.colors.Color;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * The controller of a location shown in the {@link ecdar.presentations.SimulatorOverviewPresentation}
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
     * @param intensity the intensity of the color
     */
    public void color(final Color color, final Color.Intensity intensity) {
        final Location location = getLocation();

        // Set the color of the location
        location.setColorIntensity(intensity);
        location.setColor(color);
    }

    public Color getColor() {
        return getLocation().getColor();
    }

    public Color.Intensity getColorIntensity() {
        return getLocation().getColorIntensity();
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
