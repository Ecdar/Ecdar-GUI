package SW9.presentations;

import SW9.utility.colors.Color;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import org.kordamp.ikonli.javafx.FontIcon;


import java.util.function.Consumer;

import static javafx.scene.paint.Color.TRANSPARENT;
import static javafx.scene.paint.Color.WHITE;

/**
 * Represents an element of the dropdown menu, excluding spacer and the colour palette element.
 */
public class MenuElement {
    private StackPane clickListenerFix;
    private Node item;
    private Label label;
    private ReleaseRippler rippler;
    private Region spacer;
    private HBox container;
    private FontIcon icon;
    private boolean hasExited = false;

    private ObservableBooleanValue isDisabled = new SimpleBooleanProperty(false);

    /**
     * Constructor creates an element that has no mouse event
     * @param s string to be shown in the menu
     */
    public MenuElement(final String s){
        createLabel(s);
        container.getChildren().addAll(spacer, label);
        item = container;
    }

    /**
     * Creates an element that has a label and a mouse event
     * @param s string to be shown in the menu
     * @param mouseEventConsumer event to be triggered if clicked
     */
    public MenuElement(final String s, final Consumer<MouseEvent> mouseEventConsumer) {
        createLabel(s);

        container.getChildren().addAll(spacer, label);

        spacer.setMinWidth(28);
        setClickable(mouseEventConsumer);
    }

    /**
     * Set this element to be clickable.
     * @param mouseEvent event to be run when clicked
     * @return this element
     */
    public MenuElement setClickable(final Runnable mouseEvent) {
        setClickable(event -> mouseEvent.run());
        return this;
    }

    /**
     * Set this elements to be clickable.
     * @param mouseEventConsumer event to be run when clicked
     */
    private void setClickable(final Consumer<MouseEvent> mouseEventConsumer) {
        clickListenerFix = new StackPane(container);

        createRippler(mouseEventConsumer);
        item = rippler;
    }

    /**
     * Creates an element that has a label, icon and mouse event
     * @param s string to be shown in the menu
     * @param iconString string that represents the icon to be used
     * @param mouseEventConsumer event to be triggered if clicked
     */
    public MenuElement(final String s, final String iconString, final Consumer<MouseEvent> mouseEventConsumer){
        createLabel(s);
        addIcon(iconString);

        container.getChildren().addAll(icon ,spacer, label);

        clickListenerFix = new StackPane(container);

        createRippler(mouseEventConsumer);

        item = rippler;
    }

    /**
     * Creates a rippler that triggers when the element is clicked
     * @param mouseEventConsumer the event to be triggered along the ripple effect
     */
    private void createRippler(final Consumer<MouseEvent> mouseEventConsumer){
        rippler = new ReleaseRippler(clickListenerFix);
        rippler.setRipplerFill(javafx.scene.paint.Color.TRANSPARENT);

        rippler.setOnMouseEntered(event -> {
            if (isDisabled.get()) return;

            // Set the background to a light grey
            clickListenerFix.setBackground(new Background(new BackgroundFill(
                    Color.GREY.getColor(Color.Intensity.I200),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));
            hasExited = false;
        });

        rippler.setOnMouseExited(event -> {
            if (isDisabled.get()) return;

            if (rippler.isPressed()) {
                rippler.release();
            }
            hasExited = true;

            // Set the background to be transparent
            setHideColor();
        });

        // When the rippler is pressed, run the provided consumer.
        rippler.setOnMousePressed(event -> {
            if (isDisabled.get()) {
                event.consume();
            }
        });

        rippler.setOnMouseReleased(event -> {
            if (isDisabled.get()) {
                event.consume();
                return;
            }
            if (rippler.isPressed() || !hasExited) {
                mouseEventConsumer.accept(event);
            }
            hasExited = false;
        });
    }

    /**
     * Creates a label with the text from the given string
     * @param s string to be shown in the label
     */
    private void createLabel(String s){
        label = new Label(s);
        label.getStyleClass().add("body2");

        container = new HBox();
        container.setStyle("-fx-padding: 8 16 8 16;");

        spacer = new Region();
        spacer.setMinWidth(8);
    }

    /**
     * Adds an icon to the element
     * @param icon_string string to retrieve the icon
     */
    private void addIcon(String icon_string){
        icon = new FontIcon();
        icon.setIconLiteral(icon_string);
        icon.setFill(Color.GREY.getColor(Color.Intensity.I600));
        icon.setIconSize(20);
    }

    /**
     * Gets the elements item
     * @return the elements item
     */
    public Node getItem() {
        return item;
    }

    /**
     * allows the element to be disabled
     * @param isDisabled boolean value that determines wheter it is currently disabled or enabled, true means disabled, false means enabled
     * @return Returns the element itself
     */
    public MenuElement setDisableable(ObservableBooleanValue isDisabled) { // TODO should also create the rippler if not alraedy created (e.g. when calling this method before setClickable)

        this.isDisabled = isDisabled;
        final Consumer<Boolean> updateTransparency = (disabled) -> {
            if (disabled) {
                rippler.setRipplerFill(WHITE);
                clickListenerFix.setOpacity(0.5);
            } else {
                rippler.setOpacity(1);
                rippler.setRipplerFill(Color.GREY_BLUE.getColor(Color.Intensity.I300));
                clickListenerFix.setOpacity(1);
            }
        };

        this.isDisabled.addListener((obs, oldDisabled, newDisabled) -> updateTransparency.accept(newDisabled));
        updateTransparency.accept(this.isDisabled.get());

        return this;
    }

    /**
     * allows the element to be a toggled element, the icon is visible when on and invisible when off
     * @param isToggled boolean value that determines whether it is currently disabled or enabled, true means toggle on, false means toggled off
     * @return Returns the element itself
     */
    public MenuElement setToggleable(ObservableBooleanValue isToggled){
        icon.visibleProperty().bind(isToggled);
        return this;
    }

    public void setHideColor() {
        if (clickListenerFix == null) return;

        clickListenerFix.setBackground(new Background(new BackgroundFill(
                TRANSPARENT,
                CornerRadii.EMPTY,
                Insets.EMPTY)));
    }

    public void hide() {
        setHideColor();
        if (rippler == null) return;
        rippler.release();
    }
}
