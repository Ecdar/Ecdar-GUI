package SW9.presentations;

import SW9.utility.colors.Color;
import SW9.utility.colors.EnabledColor;
import com.jfoenix.controls.JFXPopup;
import com.jfoenix.controls.JFXRippler;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.swing.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static SW9.utility.colors.EnabledColor.enabledColors;
import static javafx.scene.paint.Color.TRANSPARENT;

public class DropDownMenu extends JFXPopup {
    public static double x = 0;
    public static double y = 0;
    private final Node source;
    private final int width = 230;
    private final StackPane content;
    private final VBox list;

    private final SimpleBooleanProperty isHidden = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty isHoveringMenu = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty isHoveringSubMenu = new SimpleBooleanProperty(false);
    private final SimpleBooleanProperty canIShowSubMenu = new SimpleBooleanProperty(false);

    public DropDownMenu(final Node src) {
        popup = new JFXPopup();

    public DropDownMenu(final Node src) {
        list = new VBox();
        list.setStyle("-fx-background-color: white; -fx-padding: 8 0 8 0;");
        list.setMaxHeight(1);
        StackPane.setAlignment(list, Pos.TOP_CENTER);

        content = new StackPane(list);
        content.setMinWidth(width);
        content.setMaxWidth(width);

        list.setOnMouseExited(event -> isHoveringMenu.set(false));
        list.setOnMouseEntered(event -> isHoveringMenu.set(true));

        this.setPopupContent(content);
        source = src;

        initializeClosingClock();
    }

    /**
     * Initializer for the closing clock.
     * Makes sure that the DropDownMenu closes/hides after a short time
     */
    private void initializeClosingClock() {
        final Runnable checkIfWeShouldClose = () -> {
            if (!isHoveringMenu.get() && !isHoveringSubMenu.get()) {
                final Timer timer = new Timer(400, arg0 -> {
                        Platform.runLater(() -> {
                        if (!isHoveringMenu.get() && !isHoveringSubMenu.get()) {
                            hide();
                        }
                    });
                });
                timer.setRepeats(false); // Only execute once
                timer.start(); // Go go go!
            }
        };
        isHoveringMenu.addListener(observable -> checkIfWeShouldClose.run());
        isHoveringSubMenu.addListener(observable -> checkIfWeShouldClose.run());
    }

    public void show(final JFXPopup.PopupVPosition vAlign, final JFXPopup.PopupHPosition hAlign, final double initOffsetX, final double initOffsetY) {
        this.isHidden.set(false);
        super.show(source, vAlign, hAlign, initOffsetX, initOffsetY);
    }

    public void addListElement(final String s) {
        final MenuElement element = new MenuElement(s);
        addHideListener(element);
        list.getChildren().add(element.getItem());
    }

    public void addClickableListElement(final String s, final Consumer<MouseEvent> mouseEventConsumer) {
        final MenuElement element = new MenuElement(s, mouseEventConsumer);
        addHideListener(element);
        list.getChildren().add(element.getItem());
    }

    public void addToggleableListElement(final String s, final ObservableBooleanValue isToggled, final Consumer<MouseEvent> mouseEventConsumer) {
        final MenuElement element = new MenuElement(s, "gmi-done", mouseEventConsumer);
        addHideListener(element);
        element.setToggleable(isToggled);
        list.getChildren().add(element.getItem());
    }

    public void addToggleableAndDisableableListElement(final String s, final ObservableBooleanValue isToggled, final ObservableBooleanValue isDisabled, final Consumer<MouseEvent> mouseEventConsumer) {
        final MenuElement element = new MenuElement(s, "gmi-done", mouseEventConsumer);
        addHideListener(element);
        element.setToggleable(isToggled);
        element.setDisableable(isDisabled);
        list.getChildren().add(element.getItem());
    }

    public void addClickableAndDisableableListElement(final String s, final ObservableBooleanValue isDisabled, final Consumer<MouseEvent> mouseEventConsumer) {
        final MenuElement element = new MenuElement(s, mouseEventConsumer);
        addHideListener(element);
        element.setDisableable(isDisabled);
        list.getChildren().add(element.getItem());
    }

    /**
     * Add a custom menu element
     * @param element the constructed menu element to be added
     */
    public void addMenuElement(final MenuElement element) {
        addHideListener(element);
        list.getChildren().add(element.getItem());
    }

    /**
     * Adds a spacing element to the DropDownMenu
     */
    public void addSpacerElement() {
        final Region space1 = new Region();
        space1.setMinHeight(8);
        list.getChildren().add(space1);

        final Line sep = new Line(0, 0, width - 1, 0);
        sep.setStroke(Color.GREY.getColor(Color.Intensity.I300));
        list.getChildren().add(sep);

        final Region space2 = new Region();
        space2.setMinHeight(8);
        list.getChildren().add(space2);

        space1.setOnMouseEntered(event -> canIShowSubMenu.set(false));
        space2.setOnMouseEntered(event -> canIShowSubMenu.set(false));
    }

    /**
     * Adds a color picker to the DropDownMenu with the title color
     * @param hasColor the current color
     * @param consumer A consumer for the color property
     */
    public void addColorPicker(final HasColor hasColor, final BiConsumer<Color, Color.Intensity> consumer) {
        addListElement("Color");

        final FlowPane flowPane = new FlowPane();
        flowPane.setStyle("-fx-padding: 0 8 0 8");

        for (final EnabledColor color : enabledColors) {
            final Circle circle = new Circle(16, color.color.getColor(color.intensity));
            circle.setStroke(color.color.getColor(color.intensity.next(2)));
            circle.setStrokeWidth(1);

            final FontIcon icon = new FontIcon();
            icon.setIconLiteral("gmi-done");
            icon.setFill(color.color.getTextColor(color.intensity));
            icon.setIconSize(20);
            icon.visibleProperty().bind(new When(hasColor.colorProperty().isEqualTo(color.color)).then(true).otherwise(false));

            final StackPane child = new StackPane(circle, icon);
            child.setMinSize(40, 40);
            child.setMaxSize(40, 40);

            child.setOnMouseEntered(event -> {
                final ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(100), circle);
                scaleTransition.setFromX(circle.getScaleX());
                scaleTransition.setFromY(circle.getScaleY());
                scaleTransition.setToX(1.1);
                scaleTransition.setToY(1.1);
                scaleTransition.play();
            });
            child.setOnMouseExited(event -> {
                final ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(100), circle);
                scaleTransition.setFromX(circle.getScaleX());
                scaleTransition.setFromY(circle.getScaleY());
                scaleTransition.setToX(1.0);
                scaleTransition.setToY(1.0);
                scaleTransition.play();
            });

            child.setOnMouseClicked(event -> {
                event.consume();

                // Only color the subject if the user chooses a new color
                if (hasColor.colorProperty().get().equals(color.color)) return;

                consumer.accept(color.color, color.intensity);
            });

            flowPane.getChildren().add(child);
        }
        flowPane.setOnMouseEntered(event -> canIShowSubMenu.set(false));

        addCustomElement(flowPane);
    }


    /***
     * Makes the elements of the menu listen to the isHidden property,
     * so they can react to when the menu is hidden/shown
     * @param element The MenuElement that should hide() when isHidden is changed
     */
    private void addHideListener(MenuElement element){
        final Consumer<Boolean> updateHide = (hidden) -> { if(hidden) element.hide(); };

        this.isHidden.addListener((obs, oldValue, newValue) -> updateHide.accept(newValue));
        updateHide.accept(this.isHidden.get());
    }

    /***
     * An overridden method that lets us set the isHidden property when the menu is hidden
     * It's called manually when we want to close the DropDownMenu,
     * but also automatically for example when changing window and the menu is open
     */
    @Override
    public void hide(){
        if (this.isShowing()) {
            this.isHidden.set(true);
            super.hide();
        }
    }

    public void addCustomElement(final Node element) {
        list.getChildren().add(element);
    }

    /**
     * Adds a sub menu.
     * @param s the text of the label use for showing the sub menu
     * @param subMenu the sub menu
     * @param offset the vertical offset of the sub menu content
     */
    public void addSubMenu(final String s, final DropDownMenu subMenu, final int offset) {
        final Label label = new Label(s);
        final ObjectProperty<Boolean> isHoveringLabel = new SimpleObjectProperty<>(false);

        label.setStyle("-fx-padding: 8 16 8 16;");
        label.getStyleClass().add("body2");
        label.setMinWidth(width);

        final StackPane subMenuContent = subMenu.content;
        subMenuContent.setStyle("-fx-padding: 0 0 0 2;");
        subMenuContent.setMinWidth(subMenuContent.getMinWidth() + 1);
        subMenuContent.setMaxWidth(subMenuContent.getMinWidth() + 1);
        subMenuContent.setTranslateX(width);
        subMenuContent.setTranslateY(offset);
        subMenuContent.setOpacity(0);

        final Runnable show = () -> {
            // Set the background to a light grey
            label.setBackground(new Background(new BackgroundFill(
                    Color.GREY.getColor(Color.Intensity.I200),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));

            subMenuContent.setOpacity(1);
        };

        final Runnable hide = () -> {
            // Set the background to be transparent
            label.setBackground(new Background(new BackgroundFill(
                    TRANSPARENT,
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));

            subMenuContent.setOpacity(0);
        };

        // Set properties in order to prevent closing when hovering sub menu
        subMenuContent.setOnMouseEntered(event -> {
            isHoveringSubMenu.set(true);
            show.run();
        });

        subMenuContent.setOnMouseExited(event -> {
            isHoveringSubMenu.set(false);

            if (!isHoveringLabel.get()) {
                hide.run();
            }
        });

        this.content.getChildren().add(subMenuContent);

        final JFXRippler rippler = new JFXRippler(label);
        rippler.setRipplerFill(Color.GREY_BLUE.getColor(Color.Intensity.I300));

        rippler.setOnMouseEntered(event -> {
            isHoveringLabel.set(true);

            show.run();
        });

        rippler.setOnMouseExited(event -> {
            isHoveringLabel.set(false);

            if (!isHoveringSubMenu.get()) {
                hide.run();
            }
        });

        final FontIcon icon = new FontIcon();
        icon.setIconLiteral("gmi-chevron-right");
        icon.setFill(Color.GREY.getColor(Color.Intensity.I600));
        icon.setIconSize(20);

        final StackPane iconContainer = new StackPane(icon);
        iconContainer.setMaxWidth(20);
        iconContainer.setMaxHeight(20);
        iconContainer.setStyle("-fx-padding: 8;");
        iconContainer.setMouseTransparent(true);

        rippler.getChildren().add(iconContainer);
        StackPane.setAlignment(iconContainer, Pos.CENTER_RIGHT);

        list.getChildren().add(rippler);
    }

    public interface HasColor {
        ObjectProperty<Color> colorProperty();

        ObjectProperty<Color.Intensity> colorIntensityProperty();
    }
}