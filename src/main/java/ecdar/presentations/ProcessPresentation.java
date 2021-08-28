package ecdar.presentations;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.Location;
import ecdar.abstractions.Nail;
import ecdar.controllers.ModelController;
import ecdar.controllers.ProcessController;
import ecdar.utility.colors.Color;
import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiConsumer;

import static ecdar.presentations.Grid.GRID_SIZE;

/**
 * The presenter of a Process which is shown in {@link SimulatorOverviewPresentation}. <br />
 * This class have some of the same functionality as {@link ComponentPresentation} and could be refactored
 * into a base class.
 */
public class ProcessPresentation extends ModelPresentation {
    private final ProcessController controller;

    /**
     * Constructs a Process ready to go to the view.
     * @param component the component which the process should look like
     */
    public ProcessPresentation(final Component component){
        controller = new EcdarFXMLLoader().loadAndGetController("ProcessPresentation.fxml", this);
        controller.setComponent(component);
        super.initialize(component.getBox());
        // Initialize methods that is sensitive to width and height
        final Runnable onUpdateSize = () -> {
            initializeToolbar();
            initializeFrame();
            initializeBackground();
        };

        onUpdateSize.run();

        // Re run initialisation on update of width and height property
        component.getBox().getWidthProperty().addListener(observable -> onUpdateSize.run());
        component.getBox().getHeightProperty().addListener(observable -> onUpdateSize.run());
        setValueAreaStyle();
        setToggleValueButtonStyle();

        controller.getClocks().forEach(this::addValueToValueArea);
        controller.getVariables().forEach(this::addValueToValueArea);

        controller.getClocks().addListener((InvalidationListener) obs -> {
            controller.getClocks().forEach((s, bigDecimal) -> {
                final List<Node> filteredList = controller.valueArea.getChildren().filtered(node -> {
                    if (!(node instanceof Label))// we currently only want to look at labels
                        return false;
                    final String[] splitString = ((Label) node).getText().split("=");
                    return splitString[0].trim().equals(s);
                });
                controller.valueArea.getChildren().removeAll(filteredList);
                addValueToValueArea(s, bigDecimal);
            });
        });
        controller.getVariables().addListener((InvalidationListener) obs -> {
            controller.getVariables().forEach((s, bigDecimal) -> {
                final List<Node> filteredList = controller.valueArea.getChildren().filtered(node -> {
                    if (!(node instanceof Label))// we currently only want to look at labels
                        return false;
                    final String[] splitString = ((Label) node).getText().split("=");
                    return splitString[0].trim().equals(s);
                });
                controller.valueArea.getChildren().removeAll(filteredList);
                addValueToValueArea(s, bigDecimal);
            });
        });
    }

    /**
     * Sets the Icon and Icon size of the {@link ProcessController#toggleValueButtonIcon}
     */
    private void setToggleValueButtonStyle() {
        controller.toggleValueButtonIcon.setIconLiteral("gmi-code");
        controller.toggleValueButtonIcon.setIconSize(17);
    }

    /**
     * Set the style needed for the {@link ProcessController#valueArea}.
     * This include padding and background.
     */
    private void setValueAreaStyle() {
        // As for some reason the styling fail to be applied we need to set the styling again here
        controller.valueArea.setPadding(new Insets(16, 4, 16, 4));
        controller.valueArea.setBackground(new Background(
                new BackgroundFill(Paint.valueOf("rgba(242, 243, 244, 0.85)"),CornerRadii.EMPTY, Insets.EMPTY)));
    }

    private void addValueToValueArea(final String s, final BigDecimal bigDecimal) {
        final Label valueLabel = new Label();
        valueLabel.setText(s + " = " + bigDecimal);
        // As the styling sometimes are gone missing
        valueLabel.setFont(Font.font("Roboto Mono Medium",13));
        controller.valueArea.getChildren().add(valueLabel);
    }

    /**
     * Initializes the frame around the body of the process.
     * It also updates the color if the color is changed
     */
    private void initializeFrame() {
        final Component component = controller.getComponent();

        final Shape[] mask = new Shape[1];
        final Rectangle rectangle = new Rectangle(component.getBox().getWidth(), component.getBox().getHeight());

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Mask the parent of the frame (will also mask the background)
            mask[0] = Path.subtract(rectangle, TOP_LEFT_CORNER);
            controller.frame.setClip(mask[0]);
            controller.background.setClip(Path.union(mask[0], mask[0]));
            controller.background.setOpacity(0.5);

            // Bind the missing lines that we cropped away
            controller.topLeftLine.setStartX(Grid.CORNER_SIZE);
            controller.topLeftLine.setStartY(0);
            controller.topLeftLine.setEndX(0);
            controller.topLeftLine.setEndY(Grid.CORNER_SIZE);
            controller.topLeftLine.setStroke(newColor.getColor(newIntensity.next(2)));
            controller.topLeftLine.setStrokeWidth(1.25);
            StackPane.setAlignment(controller.topLeftLine, Pos.TOP_LEFT);

            // Set the stroke color to two shades darker
            controller.frame.setBorder(new Border(new BorderStroke(
                    newColor.getColor(newIntensity.next(2)),
                    BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY,
                    new BorderWidths(1),
                    Insets.EMPTY
            )));
        };
        component.colorProperty().addListener(observable -> {
            updateColor.accept(component.getColor(), component.getColorIntensity());
        });
        updateColor.accept(component.getColor(), component.getColorIntensity());
    }

    /**
     * Initializes the background of the process with the right color
     * If the color is changed it will also update it self
     */
    private void initializeBackground() {
        final Component component = controller.getComponent();

        // Bind the background width and height to the values in the model
        controller.background.widthProperty().bind(component.getBox().getWidthProperty());
        controller.background.heightProperty().bind(component.getBox().getHeightProperty());
        controller.background.setFill(component.getColor().getColor(component.getColorIntensity().next(-10).next(2)));
        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Set the background color to the lightest possible version of the color
            controller.background.setFill(newColor.getColor(newIntensity.next(-10).next(2)));
        };
        component.colorProperty().addListener(observable -> {
            updateColor.accept(component.getColor(), component.getColorIntensity());
        });

        updateColor.accept(component.getColor(), component.getColorIntensity());
    }

    /**
     * Initialize the Toolbar of the process. <br />
     * The toolbar is where the {@link ProcessController#name} is placed.
     */
    private void initializeToolbar() {
        final Component component = controller.getComponent();

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Set the background of the toolbar
            controller.toolbar.setBackground(new Background(new BackgroundFill(
                    newColor.getColor(newIntensity),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));

            // Set the icon color and rippler color of the toggleDeclarationButton
            controller.toggleValuesButton.setRipplerFill(newColor.getTextColor(newIntensity));

            controller.toolbar.setPrefHeight(Grid.TOOL_BAR_HEIGHT);
            controller.toggleValuesButton.setBackground(Background.EMPTY);
        };
        controller.getComponent().colorProperty().addListener(observable -> updateColor.accept(component.getColor(), component.getColorIntensity()));

        updateColor.accept(component.getColor(), component.getColorIntensity());

        // Set a hover effect for the controller.toggleDeclarationButton
        controller.toggleValuesButton.setOnMouseEntered(event -> controller.toggleValuesButton.setCursor(Cursor.HAND));
        controller.toggleValuesButton.setOnMouseExited(event -> controller.toggleValuesButton.setCursor(Cursor.DEFAULT));
    }

    /**
     * Fades the process.
     * Used if it is not involved in a transition
     */
    public void showInactive() {
        setOpacity(0.5);
    }

    /**
     * Show the process as active.
     * Used to reset the effect of {@link ProcessPresentation#showInactive()}
     */
    public void showActive() {
        setOpacity(1.0);
    }

    @Override
    ModelController getModelController() {
        return controller;
    }

    /**
     * Gets the minimum possible width when dragging the anchor.
     * The width is based on the x coordinate of locations, nails and the signature arrows. <br />
     * This should be removed from {@link ModelPresentation} and made into an interface of its own
     * @return the minimum possible width.
     */
    @Override
    @Deprecated
    double getDragAnchorMinWidth() {
        final Component component = controller.getComponent();
        double minWidth = 10 * GRID_SIZE;

        for (final Location location : component.getLocations()) {
            minWidth = Math.max(minWidth, location.getX() + GRID_SIZE * 2);
        }

        for (final Edge edge : component.getEdges()) {
            for (final Nail nail : edge.getNails()) {
                minWidth = Math.max(minWidth, nail.getX() + GRID_SIZE);
            }
        }
        return minWidth;
    }

    /**
     * Gets the minimum possible height when dragging the anchor.
     * The height is based on the y coordinate of locations, nails and the signature arrows <br />
     * This should be removed from {@link ModelPresentation} and made into an interface of its own
     * @return the minimum possible height.
     */
    @Override
    @Deprecated
    double getDragAnchorMinHeight() {
        final Component component = controller.getComponent();
        double minHeight = 10 * GRID_SIZE;

        for (final Location location : component.getLocations()) {
            minHeight = Math.max(minHeight, location.getY() + GRID_SIZE * 2);
        }

        for (final Edge edge : component.getEdges()) {
            for (final Nail nail : edge.getNails()) {
                minHeight = Math.max(minHeight, nail.getY() + GRID_SIZE);
            }
        }

        return minHeight;
    }

    public ProcessController getController() {
        return controller;
    }
}
