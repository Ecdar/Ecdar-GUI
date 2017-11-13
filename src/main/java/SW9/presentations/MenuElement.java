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

public class MenuElement {
    private StackPane clickListenerFix;
    private Node item;
    private Label label;
    private JFXRippler rippler;
    private Region spacer;
    private HBox container;
    private FontIcon icon;
    private ObservableBooleanValue isDisabled = new SimpleBooleanProperty(false);

    public MenuElement(final String s, final int width){
        createLabel(s, width);
        container.getChildren().addAll(spacer, label);
        item = container;
    }

    public MenuElement(final String s, final Consumer<MouseEvent> mouseEventConsumer, final int width) {
        createLabel(s, width);

        spacer.setMinWidth(28);
        container.getChildren().addAll(spacer, label);

        clickListenerFix = new StackPane(container);

        createRippler(mouseEventConsumer);

        item = rippler;
    }

    public MenuElement(final String s, final String iconString, final Consumer<MouseEvent> mouseEventConsumer, final int width){
        createLabel(s, width);
        addIcon(iconString);

        container.getChildren().addAll(icon ,spacer, label);

        clickListenerFix = new StackPane(container);

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
            if (!(((StackPane)(event.getTarget())).getChildren().get(0) instanceof HBox)) return;

            mouseEventConsumer.accept(event);
        });
    }

    private void createLabel(String s, int width){
        label = new Label(s);
        label.getStyleClass().add("body2");

        container = new HBox();
        container.setStyle("-fx-padding: 8 16 8 16;");

        spacer = new Region();
        spacer.setMinWidth(8);
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

    public MenuElement setDisableable(ObservableBooleanValue bool) {

        isDisabled = bool;
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

        return this;
    }

    public MenuElement setToggleable(ObservableBooleanValue isToggled){
        icon.visibleProperty().bind(isToggled);
        return this;
    }
}
