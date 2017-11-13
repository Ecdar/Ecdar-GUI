package SW9.presentations;

import SW9.utility.colors.Color;
import com.jfoenix.controls.JFXRippler;
import javafx.beans.property.BooleanProperty;
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

public class MenuItem {

    private final SimpleBooleanProperty canIShowSubMenu = new SimpleBooleanProperty(false);
    StackPane clickListenerFix;

    private Node item;
    private Label label;
    private JFXRippler rippler;
    FontIcon icon;
    ObservableBooleanValue isDisabled = new SimpleBooleanProperty(true);

    public MenuItem(final String s, final Consumer<MouseEvent> mouseEventConsumer, final int width) {
        createLabel(s, width);
        createRippler(mouseEventConsumer);
        item = rippler;
    }

    public MenuItem(final String s, final String iconString, final Consumer<MouseEvent> mouseEventConsumer, final int width){
        createLabel(s, width);
        addIcon(iconString);
        createRippler(mouseEventConsumer);
        item = rippler;
    }

    private void createRippler(final Consumer<MouseEvent> mouseEventConsumer){
        rippler = new JFXRippler(clickListenerFix);

        rippler.setOnMouseEntered(event -> {
            if (isDisabled.get()) return;

            // Set the background to a light grey
            clickListenerFix.setBackground(new Background(new BackgroundFill(
                    Color.GREY.getColor(Color.Intensity.I200),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));

            canIShowSubMenu.set(false);
        });

        rippler.setOnMouseExited(event -> {
            if (isDisabled.get()) return;

            // Set the background to be transparent
            clickListenerFix.setBackground(new Background(new BackgroundFill(
                    TRANSPARENT,
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));
        });

        // When the rippler is pressed, run the provided consumer.
        rippler.setOnMousePressed(event -> {
            if (isDisabled.get()) {
                event.consume();
                return;
            }

            // If we do not do this, the method below will be called twice
            if (!(event.getTarget() instanceof StackPane)) return;

            mouseEventConsumer.accept(event);
        });

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

        isDisabled.addListener((obs, oldDisabled, newDisabled) -> updateTransparency.accept(newDisabled));
        updateTransparency.accept(isDisabled.get());
    }

    private void createLabel(String s, int width){
        final Label label = new Label(s);
        label.getStyleClass().add("body2");

        final HBox container = new HBox();
        container.setStyle("-fx-padding: 8 16 8 16;");

        final Region spacer = new Region();
        spacer.setMinWidth(8);

        container.getChildren().addAll(spacer, label);

        clickListenerFix = new StackPane(container);
    }

    private void addIcon(String icon_string){
        icon = new FontIcon();
        icon.setIconLiteral(icon_string);
        icon.setFill(Color.GREY.getColor(Color.Intensity.I600));
        icon.setIconSize(20);
    }

    public Node getItem() {
        return item;
    }

    public void setDisableable(BooleanProperty bool) {
        isDisabled = bool;
    }

    public void setToogleable(BooleanProperty isToggled){
        icon.visibleProperty().bind(isToggled);
    }
}
