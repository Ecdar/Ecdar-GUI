package ecdar.presentations;

import ecdar.utility.colors.Color;
import ecdar.utility.colors.EnabledColor;
import com.jfoenix.controls.JFXPopup;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Screen;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.swing.*;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static ecdar.utility.colors.EnabledColor.enabledColors;

/**
 * DropDownMenu is a {@link JFXPopup} which is used as the right-click menu and options menu on for instance
 * {@link QueryPresentation#actionButton}.
 * The DropDownMenu includes methods for adding elements to the menu itself.
 *
 * Batteries included
 */
public class DropDownMenu extends JFXPopup {
    public static double x = 0;
    public static double y = 0;
    private final Node source;

    /**
     * The {@link StackPane} with all the added content from the add methods
     */
    private final StackPane content;
    private final VBox list;

    /**
     * The width of the {@link DropDownMenu}.
     * the value 230 showed to be a good base for all {@link DropDownMenu}s.
     * In this way the {@link DropDownMenu} will always stay consistent
     */
    private final SimpleIntegerProperty dropDownMenuWidth = new SimpleIntegerProperty(230);

    /**
     * Defines if the {@link DropDownMenu} is hidden on the view
     */
    private final SimpleBooleanProperty isHidden = new SimpleBooleanProperty(true);

    /**
     * Defines if the {@link DropDownMenu} is getting hovered by the mouse
     */
    private final SimpleBooleanProperty isHoveringMenu = new SimpleBooleanProperty(false);

    /**
     * Defines if a sub menu (of type {@link DropDownMenu}) belonging to this {@link DropDownMenu} is showing
     */
    private final SimpleBooleanProperty isShowingASubMenu = new SimpleBooleanProperty(false);

    private final SimpleBooleanProperty shouldSubMenuBeHidden = new SimpleBooleanProperty(true);

    /**
     * Constructor for the {@link DropDownMenu}.
     * The {@link DropDownMenu} is places on top of all the views, and does even go outside of the window/screen for now.
     * The width is, when using this constructor, is set to be 230.
     * @param src The view where the {@link DropDownMenu} should be shown
     * @see DropDownMenu#DropDownMenu(Node, int)
     */
    public DropDownMenu(final Node src) {
        this(src, 230);
    }

    /**
     * Constructor for the {@link DropDownMenu}.
     * The {@link DropDownMenu} is places on top of all the views, and does even go outside of the window/screen for now.
     * @param src The view where the {@link DropDownMenu} should be shown
     * @param width The width of the {@link DropDownMenu}
     */
    public DropDownMenu(final Node src, final int width)  {
        dropDownMenuWidth.set(width);
        list = new VBox();
        list.setStyle("-fx-background-color: white; -fx-padding: 8 0 8 0;");
        list.setMaxHeight(1);
        StackPane.setAlignment(list, Pos.TOP_CENTER);

        content = new StackPane(list);
        content.setMinWidth(dropDownMenuWidth.get());
        content.setMaxWidth(dropDownMenuWidth.get());

        list.setOnMouseExited(event -> isHoveringMenu.set(false));
        list.setOnMouseEntered(event -> isHoveringMenu.set(true));

        this.setPopupContent(content);
        source = src;

        initializeClosingClock();
    }

    /**
     * Initializer for the closing clock.
     * Makes sure that the {@link DropDownMenu} closes/hides after a short time
     */
    private void initializeClosingClock() {
        final Runnable checkIfWeShouldClose = () -> {
            if (!isHoveringMenu.get() && !isShowingASubMenu.get()) {
                final Timer timer = new Timer(400, arg0 -> {
                        Platform.runLater(() -> {
                        if (!isHoveringMenu.get() && !isShowingASubMenu.get()) {
                            hide();
                        }
                    });
                });
                timer.setRepeats(false); // Only execute once
                timer.start(); // Go go go!
            }
        };
        isHoveringMenu.addListener(observable -> checkIfWeShouldClose.run());
        isShowingASubMenu.addListener(observable -> checkIfWeShouldClose.run());
    }

    /**
     * Shows the DropDownMenu with the added content, gotten from the add* methods
     * Remember to call {@link #hide()} when you want to hide the {@link DropDownMenu}
     * @param vAlign Vertically alignment
     * @param hAlign Horizontal alignment
     * @param initOffsetX Offset on the X-axis of the view
     * @param initOffsetY Offset on the Y-axis of the view
     */
    public void show(final JFXPopup.PopupVPosition vAlign, final JFXPopup.PopupHPosition hAlign, final double initOffsetX, final double initOffsetY) {
        //Needed to ensure correct placement of submenu
        super.show(this.source, vAlign, hAlign, initOffsetX, initOffsetY);
        super.hide();

        //Check if the dropdown will appear outside the screen and change the offset accordingly
        double offsetX = initOffsetX;
        double offsetY = initOffsetY;
        double distEdgeX = Screen.getPrimary().getBounds().getWidth() - (source.getLayoutX() + offsetX);
        double distEdgeY = Screen.getPrimary().getBounds().getHeight() - (source.getLayoutY() + offsetY);

        //The additional 20 is added for margin
        if(distEdgeX < dropDownMenuWidth.get() + 20){
            offsetX -= (dropDownMenuWidth.get() + 20) - distEdgeX;
        }

        if(distEdgeY < list.getHeight() + 20){
            offsetY -= (list.getHeight() + 20) - distEdgeY;
        }

        //Need final temporary variables for lambda
        final double finalOffsetX = offsetX;
        final double finalOffsetY = offsetY;

        //Set the x and y of the dropdown to ensure that locations etc. are added correctly
        x = this.source.getLayoutX() + finalOffsetX;
        y = this.source.getLayoutY() + finalOffsetY;

        this.isHidden.set(false);

        Platform.runLater( () -> super.show(this.source, vAlign, hAlign, finalOffsetX, finalOffsetY));
    }

    /**
     * Adds a {@link MenuElement} to the {@link DropDownMenu}
     * @param s The title of the element
     */
    public void addListElement(final String s) {
        final MenuElement element = new MenuElement(s);
        addHideListener(element);
        element.getItem().setOnMouseEntered(event -> shouldSubMenuBeHidden.set(true));
        list.getChildren().add(element.getItem());
    }

    /**
     * Adds a clickable {@link MenuElement} to the {@link DropDownMenu}
     * @param s The title of the element
     * @param mouseEventConsumer The event that is triggered when clicked
     */
    public void addClickableListElement(final String s, final Consumer<MouseEvent> mouseEventConsumer) {
        final MenuElement element = new MenuElement(s, mouseEventConsumer);
        addHideListener(element);
        list.getChildren().add(element.getItem());
    }

    /**
     * Adds a toggleable {@link MenuElement} to the {@link DropDownMenu}
     * @param s The title of the element
     * @param isToggled Defines if the element should be disabled of enabled.
     *                  Enabled if it is true, disabled if false
     * @param mouseEventConsumer The event that is triggered when clicked
     */
    public void addToggleableListElement(final String s, final ObservableBooleanValue isToggled, final Consumer<MouseEvent> mouseEventConsumer) {
        final MenuElement element = new MenuElement(s, "gmi-done", mouseEventConsumer);
        addHideListener(element);
        element.setToggleable(isToggled);
        list.getChildren().add(element.getItem());
    }

    /**
     * Adds a toggleable and disableable {@link MenuElement} to {@link #content}
     * @param s The title of the element
     * @param isToggled Defines if the element should be disabled of enabled.
     *                  Enabled if it is true, disabled if false
     * @param isDisabled Determines whether it is currently disabled or enabled.
     *                      True means disabled, false means enabled
     * @param mouseEventConsumer The event that is triggered when clicked
     */
    public void addToggleableAndDisableableListElement(final String s, final ObservableBooleanValue isToggled, final ObservableBooleanValue isDisabled, final Consumer<MouseEvent> mouseEventConsumer) {
        final MenuElement element = new MenuElement(s, "gmi-done", mouseEventConsumer);
        addHideListener(element);
        element.setToggleable(isToggled);
        element.setDisableable(isDisabled);
        list.getChildren().add(element.getItem());
    }

    /**
     * Adds a clickable and disableable {@link MenuElement} to {@link #content}
     * @param s The title of the element
     * @param isDisabled Determines whether it is currently disabled or enabled.
     *                      True means disabled, false means enabled
     * @param mouseEventConsumer The event that is triggered when clicked
     */
    public void addClickableAndDisableableListElement(final String s, final ObservableBooleanValue isDisabled, final Consumer<MouseEvent> mouseEventConsumer) {
        final MenuElement element = new MenuElement(s, mouseEventConsumer);
        addHideListener(element);
        element.setDisableable(isDisabled);
        list.getChildren().add(element.getItem());
    }

    /**
     * Adds a custom {@link MenuElement} to {@link #content}
     * @param element The constructed menu element to be added
     */
    public void addMenuElement(final MenuElement element) {
        addHideListener(element);
        list.getChildren().add(element.getItem());
    }

    /**
     * Adds a spacing element to the {@link DropDownMenu}
     */
    public void addSpacerElement() {
        makeSpacerElement(8);
    }

    /**
     * Adds a smaller spacing element to the {@link DropDownMenu}
     */
    public void addSmallSpacerElement() {
        makeSpacerElement(2);
    }

    /**
     * Adds a color picker to the {@link DropDownMenu}s to {@link #content} with the title color
     * @param hasColor The current color
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
        flowPane.setOnMouseEntered(event -> shouldSubMenuBeHidden.set(true));

        addCustomElement(flowPane);
    }


    /***
     * Makes the elements of the {@link DropDownMenu} listen to the {@link #isHidden} property,
     * such that they can react when the menu is hidden/shown
     * @param element The {@link MenuElement} that should {@link #hide()} when {@link #isHidden} is changed
     */
    private void addHideListener(MenuElement element) {
        final Consumer<Boolean> updateHide = (hidden) -> { if(hidden) element.hide(); };
        this.isHidden.addListener((obs, oldValue, newValue) -> updateHide.accept(newValue));
        updateHide.accept(this.isHidden.get());
    }

    /**
     * An overridden method that lets us set {@link #isHidden} when the {@link DropDownMenu} is hidden
     * It is called manually when we want to close {@link DropDownMenu},
     * but also automatically for example when changing window and the {@link DropDownMenu} is open
     */
    @Override
    public void hide(){
        if (this.isShowing()) {
            this.isHidden.set(true);
            Platform.runLater(super::hide);
        }
    }

    /**
     * Adds a custom element to the {@link DropDownMenu}.
     * @param element The element to add to the {@link DropDownMenu}.
     */
    public void addCustomElement(final Node element) {
        list.getChildren().add(element);
    }

    /**
     * Adds a sub menu to the {@link DropDownMenu}.
     * @param s The text of the label use for showing the sub menu
     * @param subMenu The sub menu
     * @param offsetY The vertical offset of the sub menu's content
     * @param offsetX The horizontal offset of the sub menu's content
     */
    public void addSubMenu(final String s, final DropDownMenu subMenu, final int offsetY, final int offsetX) {
        final Label label = new Label(s);
        final SimpleBooleanProperty isHoveringSubMenu = new SimpleBooleanProperty(false);
        final SimpleBooleanProperty canSubMenuBeClosed = new SimpleBooleanProperty(true);

        label.setStyle("-fx-padding: 8 16 8 16;");
        label.getStyleClass().add("body2");
        label.setMinWidth(dropDownMenuWidth.get());
        final ReleaseRippler rippler = new ReleaseRippler(label);

        final Runnable showSubMenu = () -> {
            if(subMenu.isHidden.get()) {
                if(Screen.getPrimary().getBounds().getWidth() - (super.getAnchorX() + dropDownMenuWidth.get()) < dropDownMenuWidth.get()){
                    subMenu.show(PopupVPosition.TOP, PopupHPosition.LEFT, -(dropDownMenuWidth.get() + offsetX), offsetY);
                } else {
                    subMenu.show(PopupVPosition.TOP, PopupHPosition.LEFT, dropDownMenuWidth.get() + offsetX, offsetY);
                }
            }
        };

        final Runnable hideSubMenu = () -> {
            if (!subMenu.isHidden.get()) {
                subMenu.hide();
            }
        };

        subMenu.isHoveringMenu.addListener((observable, oldValue, newValue) -> isHoveringSubMenu.set(newValue));
        subMenu.isHidden.addListener((observable, oldValue, newValue) -> {
            if (!newValue){
                shouldSubMenuBeHidden.set(false);
            } else {
                setRipplerColorAsDeselected(rippler);
            }
        });

        //Prevents flickering effect when opening the submenu
        final Timer closingTimer = new Timer(100, event -> {
            if (shouldSubMenuBeHidden.get()) {
                Platform.runLater(hideSubMenu);
            }
            canSubMenuBeClosed.set(true);
        });
        closingTimer.setRepeats(false);


        rippler.setRipplerFill(MenuElement.SELECTED_COLOR);

        rippler.setOnMouseEntered(event -> {
            setRipplerColorAsSelected(rippler);

            canSubMenuBeClosed.set(false);
            closingTimer.start();
            Platform.runLater(showSubMenu);
            isShowingASubMenu.set(true);
            shouldSubMenuBeHidden.set(false);
        });

        rippler.setOnMouseExited(event -> {
            if (canSubMenuBeClosed.get() && shouldSubMenuBeHidden.get()) {
                Platform.runLater(hideSubMenu);
                isShowingASubMenu.set(false);
                setRipplerColorAsDeselected(rippler);
            }
            if (rippler.isPressed()) {
                rippler.release();
            }
        });

        rippler.setOnMouseReleased(event -> Platform.runLater(showSubMenu));

        // To close the menu when we are entering other elements on the DropDownMenu
        shouldSubMenuBeHidden.addListener(((observable, oldValue, newValue) -> {
            if(newValue){
                Platform.runLater(hideSubMenu);
            }
        }));

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

        // To hide the submenu if the parent menu is hidden
        this.isHidden.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                hideSubMenu.run();
                setRipplerColorAsDeselected(rippler);
            }
        });
    }

    private void setRipplerColor(final ReleaseRippler rippler, final javafx.scene.paint.Color color) {
        rippler.setBackground(new Background(new BackgroundFill(
                color,
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));
    }

    private void setRipplerColorAsDeselected(final ReleaseRippler rippler) {
        setRipplerColor(rippler, MenuElement.DESELECTED_COLOR);
    }
    private void setRipplerColorAsSelected(final ReleaseRippler rippler) {
        setRipplerColor(rippler, MenuElement.SELECTED_COLOR);
    }


    /**
     * A method to handle the creation and addition of the spacing element to the {@link DropDownMenu}.
     * @param height The height of the spacing element
     */
    private void makeSpacerElement(final int height) {
        final Region space1 = new Region();
        space1.setMinHeight(height);
        list.getChildren().add(space1);

        final Line sep = new Line(0, 0, dropDownMenuWidth.get() - 1, 0);
        sep.setStroke(Color.GREY.getColor(Color.Intensity.I300));
        list.getChildren().add(sep);

        final Region space2 = new Region();
        space2.setMinHeight(height);
        list.getChildren().add(space2);

        sep.setOnMouseEntered(event -> shouldSubMenuBeHidden.set(true));
        space1.setOnMouseEntered(event -> shouldSubMenuBeHidden.set(true));
        space2.setOnMouseEntered(event -> shouldSubMenuBeHidden.set(true));
    }

    public interface HasColor {
        ObjectProperty<Color> colorProperty();

        ObjectProperty<Color.Intensity> colorIntensityProperty();
    }
}