package ecdar.presentations;

import ecdar.Ecdar;
import ecdar.abstractions.Component;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.LocationAware;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;

import java.util.function.BiConsumer;

/**
 * The presentation for the tag shown on a {@link SimEdgePresentation} in the {@link SimulatorOverviewPresentation}<br />
 * This class should be refactored such that code which are duplicated from {@link TagPresentation}
 * have its own base class.
 */
public class SimTagPresentation extends StackPane {

    private final static Color backgroundColor = Color.GREY;
    private final static Color.Intensity backgroundColorIntensity = Color.Intensity.I50;

    private final ObjectProperty<Component> component = new SimpleObjectProperty<>(null);
    private final ObjectProperty<LocationAware> locationAware = new SimpleObjectProperty<>(null);

    private LineTo l2;
    private LineTo l3;

    private final static double TAG_HEIGHT = 16; // ToDo NIELS: This should be changed to follow the same value as TagPresentation

    /**
     * Constructs the {@link SimTagPresentation}
     */
    public SimTagPresentation() {
        new EcdarFXMLLoader().loadAndGetController("SimTagPresentation.fxml", this);
        initializeShape();
        initializeLabel();
        initializeMouseTransparency();
    }

    private void initializeMouseTransparency() {
        mouseTransparentProperty().bind(opacityProperty().isEqualTo(0, 0.00f));
    }

    /**
     * Initializes the label which shows the property
     */
    private void initializeLabel() {
        final Label label = (Label) lookup("#label");
        final Path shape = (Path) lookup("#shape");

        final Insets insets = new Insets(0,2,0,2);
        label.setPadding(insets);

        final int padding = 0;

        label.layoutBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            double newWidth = Math.max(newBounds.getWidth(), 10);
            final double res = Ecdar.CANVAS_PADDING * 2 - (newWidth % (Ecdar.CANVAS_PADDING * 2));
            newWidth += res;

            l2.setX(newWidth + padding);
            l3.setX(newWidth + padding);

            setMinWidth(newWidth + padding);
            setMaxWidth(newWidth + padding);

            label.focusedProperty().addListener((observable, oldFocused, newFocused) -> {
                if (newFocused) {
                    shape.setTranslateY(2);
                    label.setTranslateY(2);
                }
            });

            if (getWidth() >= 1000) {
                setWidth(newWidth);
                setHeight(TAG_HEIGHT);
                shape.setTranslateY(-1);
            }

            // Fixes the jumping of the shape when the text field is empty
            if (label.getText().isEmpty()) {
                shape.setLayoutX(0);
            }
        });
    }

    /**
     * Initialize the shape which is around the property label
     */
    private void initializeShape() {
        final int WIDTH = 5000;
        final double HEIGHT = TAG_HEIGHT;

        final Path shape = (Path) lookup("#shape");

        final MoveTo start = new MoveTo(0, 0);

        l2 = new LineTo(WIDTH, 0);
        l3 = new LineTo(WIDTH, HEIGHT);
        final LineTo l4 = new LineTo(0, HEIGHT);
        final LineTo l6 = new LineTo(0, 0);

        shape.getElements().addAll(start, l2, l3, l4, l6);
        shape.setFill(backgroundColor.getColor(backgroundColorIntensity));
        shape.setStroke(backgroundColor.getColor(backgroundColorIntensity.next(4)));
    }

    public void bindToColor(final ObjectProperty<Color> color, final ObjectProperty<Color.Intensity> intensity) {
        bindToColor(color, intensity, false);
    }

    public void bindToColor(final ObjectProperty<Color> color, final ObjectProperty<Color.Intensity> intensity, final boolean doColorBackground) {
        final BiConsumer<Color, Color.Intensity> recolor = (newColor, newIntensity) -> {
            if (doColorBackground) {
                final Path shape = (Path) lookup("#shape");
                shape.setFill(newColor.getColor(newIntensity.next(-1)));
                shape.setStroke(newColor.getColor(newIntensity.next(-1).next(2)));
            }
        };

        color.addListener(observable -> recolor.accept(color.get(), intensity.get()));
        intensity.addListener(observable -> recolor.accept(color.get(), intensity.get()));
        recolor.accept(color.get(), intensity.get());
    }

    /**
     * Updates the label with the given string and binds updates from the label on the given {@link StringProperty}
     * @param string the string to set and bind to
     */
    public void setAndBindString(final StringProperty string) {
        final Label label = (Label) lookup("#label");

        label.textProperty().unbind();
        label.setText(string.get());
        string.bind(label.textProperty());
    }

    /**
     * Replaces spaces with underscores in the label
     */
/*    public void replaceSpace() {
        initializeTextAid();
    }*/

    public Component getComponent() {
        return component.get();
    }

    public void setComponent(final Component component) {
        this.component.set(component);
    }

    public ObjectProperty<Component> componentProperty() {
        return component;
    }

    public LocationAware getLocationAware() {
        return locationAware.get();
    }

    public ObjectProperty<LocationAware> locationAwareProperty() {
        return locationAware;
    }

    public void setLocationAware(LocationAware locationAware) {
        this.locationAware.set(locationAware);
    }

    /**
     * Sets the disabled property in the label
     * @param bool true ---  the label will be disabled
     *             false --- the label will be enabled
     */
    public void setDisabledText(boolean bool){
        final Label label = (Label) lookup("#label");
        label.setDisable(true);
    }
}
