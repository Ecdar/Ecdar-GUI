package ecdar.presentations;

import com.jfoenix.controls.JFXRippler;
import com.jfoenix.skins.ValidationPane;
import ecdar.controllers.CanvasController;
import ecdar.controllers.EcdarController;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.CanvasDragHelper;
import ecdar.utility.helpers.MouseTrackable;
import ecdar.utility.helpers.SelectHelper;
import ecdar.utility.mouse.MouseTracker;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

public class CanvasPresentation extends Pane implements MouseTrackable {
    public MouseTracker mouseTracker;

    private final DoubleProperty x = new SimpleDoubleProperty(0);
    private final DoubleProperty y = new SimpleDoubleProperty(0);

    // This is the value of the user option for grid on/off.
    // This is not whether the grid is actual visible or not.
    // E.g. the grid is hidden while taking snapshots.
    // But that does not affect the gridUiOn field.
    private final BooleanProperty gridUiOn = new SimpleBooleanProperty(false);

    private final Grid grid = new Grid();
    private final CanvasController controller;

    public CanvasPresentation() {
        mouseTracker = new MouseTracker(this);
        controller = new EcdarFXMLLoader().loadAndGetController("CanvasPresentation.fxml", this);

        initializeGrid();

        CanvasDragHelper.makeDraggable(this, mouseEvent -> mouseEvent.getButton().equals(MouseButton.SECONDARY));
        mouseTracker.registerOnMousePressedEventHandler(this::startDragBox);

        controller.zoomHelper.setGrid(this.grid);
        controller.zoomHelper.setCanvas(this);
    }

    private void initializeGrid() {
        getChildren().add(grid);
        grid.toBack();
        gridUiOn.setValue(true);

        controller.allowGridProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && gridUiOn.get()) showGrid();
            else hideGrid();
        });

        //When the translation coordinates are changed, make sure that it is handled for the grid as well, to ensure that the grid is still centered on screen
        this.translateXProperty().addListener(((observable, oldValue, newValue) -> grid.handleTranslateX(oldValue.doubleValue(), newValue.doubleValue(), this.scaleXProperty().doubleValue())));
        this.translateYProperty().addListener(((observable, oldValue, newValue) -> grid.handleTranslateY(oldValue.doubleValue(), newValue.doubleValue(), this.scaleYProperty().doubleValue())));
    }

    /**
     * Toggles the user option for whether or not to show the grid on components and system views.
     * @return a Boolean property that is true if the grid has been turned on and false if the grid has been turned off
     */
    public BooleanProperty toggleGridUi() {
        if (gridUiOn.get()) {
            if (controller.isGridAllowed()) hideGrid();

            gridUiOn.setValue(false);
        } else {
            showGrid();
            gridUiOn.setValue(true);
        }
        return gridUiOn;
    }

    /**
     * Shows the grid.
     */
    private void showGrid() {
        grid.setOpacity(1);
    }

    /**
     * Hides the grid.
     */
    private void hideGrid() {
        grid.setOpacity(0);
    }

    /**
     * Updates if views should show an inset behind the error view.
     * @param shouldShow true iff views should show an inset
     */
    public static void showBottomInset(final Boolean shouldShow) {
        EcdarController.getActiveCanvasPresentation().getController().updateOffset(shouldShow);
    }

    @Override
    public DoubleProperty xProperty() {
        return x;
    }

    @Override
    public DoubleProperty yProperty() {
        return y;
    }

    @Override
    public double getX() {
        return xProperty().get();
    }

    @Override
    public double getY() {
        return yProperty().get();
    }

    @Override
    public MouseTracker getMouseTracker() {
        return mouseTracker;
    }

    public CanvasController getController() {
        return controller;
    }

    private void startDragBox(final MouseEvent event) {
        if(event.isPrimaryButtonDown()) {
            SelectHelper.clearSelectedElements();

            Rectangle selectionRectangle = new Rectangle();
            selectionRectangle.setStroke(SelectHelper.SELECT_COLOR.getColor(SelectHelper.SELECT_COLOR_INTENSITY_BORDER));
            javafx.scene.paint.Color fillColor = SelectHelper.SELECT_COLOR.getColor(Color.Intensity.I100);
            fillColor = fillColor.deriveColor(fillColor.getHue(), fillColor.getSaturation(), fillColor.getBrightness(), 0.5);
            selectionRectangle.setFill(fillColor);
            selectionRectangle.getStrokeDashArray().addAll(5.0, 5.0);

            double mouseDownX = event.getX();
            double mouseDownY = event.getY();
            selectionRectangle.setX(mouseDownX);
            selectionRectangle.setY(mouseDownY);
            selectionRectangle.setWidth(0);
            selectionRectangle.setHeight(0);

            mouseTracker.registerOnMouseDraggedEventHandler(e -> {
                selectionRectangle.setX(Math.min(e.getX(), mouseDownX));
                selectionRectangle.setWidth(Math.abs(e.getX() - mouseDownX));
                selectionRectangle.setY(Math.min(e.getY(), mouseDownY));
                selectionRectangle.setHeight(Math.abs(e.getY() - mouseDownY));

                SelectHelper.clearSelectedElements();

                // Recursively find all relevant ItemSelectable Nodes in the scene
                updateSelection(this, selectionRectangle);
            });

            mouseTracker.registerOnMouseReleasedEventHandler(e -> this.getChildren().remove(selectionRectangle));
            this.getChildren().add(selectionRectangle);
        }
    }

    private void updateSelection(Parent currentNode, Rectangle selectionRectangle) {
        // None of these nodes contain ItemSelectable nodes, so avoiding traversing these sub-trees improves performance
        if (currentNode instanceof VBox || currentNode instanceof ValidationPane || currentNode instanceof JFXRippler || currentNode instanceof BorderPane) {
            return;
        }

        if (currentNode instanceof LocationPresentation) {
            if (selectIfWithinSelectionBox(currentNode, selectionRectangle)) {
                SelectHelper.addToSelection(((LocationPresentation) currentNode).getController());
            }
        } else if (currentNode instanceof EdgePresentation) {
            ((EdgePresentation) currentNode).getController().getNailNailPresentationMap().values().forEach((nailPresentation -> {
                if (selectIfWithinSelectionBox(nailPresentation, selectionRectangle)) {
                    SelectHelper.addToSelection(nailPresentation.getController());
                }
            }));
        } else if (currentNode instanceof ComponentOperatorPresentation || currentNode instanceof ComponentInstancePresentation) {
            if (selectIfWithinSelectionBox(currentNode, selectionRectangle)) {
                SelectHelper.addToSelection((SelectHelper.ItemSelectable) currentNode);
            }
        }

        currentNode.getChildrenUnmodifiable().forEach((node) -> {
            if (node instanceof Parent) {
                updateSelection((Parent) node, selectionRectangle);
            }
        });
    }

    /**
     * Returns whether the item is within the selection.
     * @param item the node to check.
     * @param selectionRectangle the selection box.
     */
    private boolean selectIfWithinSelectionBox(Node item, Rectangle selectionRectangle) {
        Bounds itemCoordinates = item.localToScreen(item.getLayoutBounds());
        Bounds selectionCoordinates = selectionRectangle.localToScreen(selectionRectangle.getLayoutBounds());

        if (selectionCoordinates == null || itemCoordinates == null) return false;

        return selectionCoordinates.getMinX() <
                itemCoordinates.getMinX() &&
                itemCoordinates.getMinX() + 10 <
                        selectionRectangle.getWidth() +
                                selectionCoordinates.getMinX() &&
                selectionCoordinates.getMinY() < itemCoordinates.getMinY() &&
                itemCoordinates.getMinY() + 10 < selectionRectangle.getHeight() + selectionCoordinates.getMinY();
    }
}
