package ecdar.presentations;

import com.jfoenix.controls.JFXRippler;
import com.jfoenix.skins.ValidationPane;
import ecdar.controllers.CanvasController;
import ecdar.controllers.EcdarController;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.MouseTrackable;
import ecdar.utility.helpers.SelectHelper;
import ecdar.utility.mouse.MouseTracker;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

public class CanvasPresentation extends StackPane implements MouseTrackable {
    public MouseTracker mouseTracker;

    private final DoubleProperty x = new SimpleDoubleProperty(0);
    private final DoubleProperty y = new SimpleDoubleProperty(0);

    private final CanvasController controller;
    private final BooleanProperty gridToggle = new SimpleBooleanProperty(false);

    public CanvasPresentation() {
        mouseTracker = new MouseTracker(this);
        controller = new EcdarFXMLLoader().loadAndGetController("CanvasPresentation.fxml", this);
        mouseTracker.registerOnMousePressedEventHandler(this::startDragSelect);

        getController().root.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> EcdarController.setActiveCanvasPresentation(this));

        initializeModelDrag();
        initializeGrid();
        initializeToolbar();

        controller.allowGridProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && gridToggle.get()) showGrid();
            else hideGrid();
        });

        Platform.runLater(this::initializeZoomHelper);
        getStyleClass().add("canvas-presentation");
    }

    private void initializeModelDrag() {
        final double[] dragXOffset = {0d};
        final double[] dragYOffset = {0d};
        final double[] previousXTranslation = {0d};
        final double[] previousYTranslation = {0d};

        final BooleanProperty presWasAllowed = new SimpleBooleanProperty(false);
        final BooleanProperty isBeingDragged = new SimpleBooleanProperty(false);

        mouseTracker.registerOnMousePressedEventHandler(event -> {
            presWasAllowed.set(event.getButton().equals(MouseButton.SECONDARY));
            if (!presWasAllowed.get()) return;
            isBeingDragged.set(true);
            event.consume();

            dragXOffset[0] = event.getSceneX();
            dragYOffset[0] = event.getSceneY();
            previousXTranslation[0] = controller.modelPane.getTranslateX();
            previousYTranslation[0] = controller.modelPane.getTranslateY();

            controller.root.setCursor(Cursor.MOVE);
        });

        mouseTracker.registerOnMouseDraggedEventHandler(event -> {
            if (!presWasAllowed.get() || !isBeingDragged.get()) return;
            event.consume();

            final double dragDistanceX = Grid.snap(event.getSceneX() - dragXOffset[0]);
            final double dragDistanceY = Grid.snap(event.getSceneY() - dragYOffset[0]);
            final double newX = previousXTranslation[0] + dragDistanceX;
            final double newY = previousYTranslation[0] + dragDistanceY;

            controller.modelPane.setTranslateX(newX);
            controller.modelPane.setTranslateY(newY);
        });

        mouseTracker.registerOnMouseReleasedEventHandler(event -> {
            controller.root.setCursor(Cursor.DEFAULT);
            dragXOffset[0] = controller.modelPane.getTranslateX() - event.getSceneX();
            dragYOffset[0] = controller.modelPane.getTranslateY() - event.getSceneY();
            previousXTranslation[0] = controller.modelPane.getTranslateX();
            previousYTranslation[0] = controller.modelPane.getTranslateY();
            isBeingDragged.setValue(false);
        });
    }

    private void initializeGrid() {
        gridToggle.setValue(true);
        controller.allowGridProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && gridToggle.get()) showGrid();
            else hideGrid();
        });
    }

    private void initializeToolbar() {
        final Color color = Color.GREY_BLUE;
        final Color.Intensity intensity = Color.Intensity.I700;

        // Set the background for the top toolbar
        controller.toolbar.setBackground(
                new Background(new BackgroundFill(color.getColor(intensity),
                        CornerRadii.EMPTY,
                        Insets.EMPTY)
                ));

        initializeToolbarButton(controller.zoomIn);
        initializeToolbarButton(controller.zoomOut);
        initializeToolbarButton(controller.zoomToFit);
        initializeToolbarButton(controller.resetZoom);
    }

    private void initializeToolbarButton(final JFXRippler button) {
        final Color color = Color.GREY_BLUE;
        final Color.Intensity colorIntensity = Color.Intensity.I800;

        button.setMaskType(JFXRippler.RipplerMask.CIRCLE);
        button.setRipplerFill(color.getTextColor(colorIntensity));
        button.setPosition(JFXRippler.RipplerPos.BACK);
    }

    private void initializeZoomHelper() {
        controller.zoomHelper.setGrid(controller.grid);
        controller.zoomHelper.setCanvas(this);
    }

    /**
     * Toggles the user option for whether or not to show the grid on components and system views.
     * @return a Boolean property that is true if the grid has been turned on and false if the grid has been turned off
     */
    public BooleanProperty toggleGridUi() {
        if (gridToggle.get()) {
            if (controller.isGridAllowed()) hideGrid();

            gridToggle.setValue(false);
        } else {
            showGrid();
            gridToggle.setValue(true);
        }
        return gridToggle;
    }

    private void showGrid() {
        controller.grid.setOpacity(1);
    }

    private void hideGrid() {
        controller.grid.setOpacity(0);
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

    private void startDragSelect(final MouseEvent event) {
        if(event.isPrimaryButtonDown()) {
            SelectHelper.clearSelectedElements();

            double mouseDownX = event.getX();
            double mouseDownY = event.getY();

            Rectangle selectionRectangle = initializeRectangleForSelectionBox(mouseDownX, mouseDownY);

            mouseTracker.registerOnMouseDraggedEventHandler(e -> {
                // If the drag is left of the canvas, ignore x
                if (e.getX() >= 0) {
                    // Make sure that the rectangle does not grow beyond the horizontal limits of the canvas
                    selectionRectangle.setX(Math.max(Math.min(e.getX(), mouseDownX), 0));
                    double newWidth = Math.abs(e.getX() - mouseDownX);
                    selectionRectangle.setWidth(newWidth + selectionRectangle.getX() < getWidth() ? newWidth : getWidth() - mouseDownX);
                    // We need to update the translation coordinates to display the selection rectangle in the right place
                    selectionRectangle.setTranslateX(Math.max(Math.min(e.getX(), mouseDownX), 0));

                }

                // If the drag is above the canvas, ignore y
                if (e.getY() >= 0) {
                    // Make sure that the rectangle does not grow beyond the vertical limits of the canvas
                    selectionRectangle.setY(Math.max(Math.min(e.getY(), mouseDownY), 0));
                    double newHeight = Math.abs(e.getY() - mouseDownY);
                    selectionRectangle.setHeight(newHeight + selectionRectangle.getY() < getHeight() ? newHeight : getHeight() - mouseDownY);
                    // Make sure that the rectangle does not grow beyond the horizontal limits of the canvas
                    selectionRectangle.setTranslateY(Math.max(Math.min(e.getY(), mouseDownY), 0));
                }

                SelectHelper.clearSelectedElements();

                // Recursively find all relevant ItemSelectable Nodes in the scene
                updateSelection(this, selectionRectangle);
            });

            mouseTracker.registerOnMouseReleasedEventHandler(e -> Platform.runLater(() -> this.getChildren().remove(selectionRectangle)));
            this.getChildren().add(selectionRectangle);
        }
    }

    private Rectangle initializeRectangleForSelectionBox(double mouseDownX, double mouseDownY) {
        Rectangle selectionRectangle = new Rectangle();
        selectionRectangle.setStroke(SelectHelper.SELECT_COLOR.getColor(SelectHelper.SELECT_COLOR_INTENSITY_BORDER));
        javafx.scene.paint.Color fillColor = SelectHelper.SELECT_COLOR.getColor(Color.Intensity.I100);
        fillColor = fillColor.deriveColor(fillColor.getHue(), fillColor.getSaturation(), fillColor.getBrightness(), 0.5);
        selectionRectangle.setFill(fillColor);
        selectionRectangle.getStrokeDashArray().addAll(5.0, 5.0);

        selectionRectangle.setX(mouseDownX);
        selectionRectangle.setY(mouseDownY);
        selectionRectangle.setWidth(0);
        selectionRectangle.setHeight(0);

        return selectionRectangle;
    }

    private void updateSelection(Parent currentNode, Rectangle selectionRectangle) {
        // None of these nodes contain ItemSelectable nodes, so avoiding traversing these sub-trees improves performance
        if (currentNode instanceof VBox || currentNode instanceof ValidationPane || currentNode instanceof JFXRippler || currentNode instanceof BorderPane) {
            return;
        }

        if (currentNode instanceof LocationPresentation) {
            if (selectIfWithinSelectionBox(((LocationPresentation) currentNode).getController().circle, ((LocationPresentation) currentNode).getController(), selectionRectangle)) {
                SelectHelper.addToSelection(((LocationPresentation) currentNode).getController());
            }
        } else if (currentNode instanceof EdgePresentation) {
            ((EdgePresentation) currentNode).getController().getNailNailPresentationMap().values().forEach((nailPresentation -> {
                if (selectIfWithinSelectionBox(nailPresentation.getController().nailCircle, nailPresentation.getController(), selectionRectangle)) {
                    SelectHelper.addToSelection(nailPresentation.getController());
                }
            }));
        } else if (currentNode instanceof ComponentOperatorPresentation || currentNode instanceof ComponentInstancePresentation) {
            if (selectIfWithinSelectionBox(currentNode, (SelectHelper.ItemSelectable) currentNode, selectionRectangle)) {
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
     * Returns whether the item is within the selection box.
     * @param item the node to potentially be selected.
     * @param itemSelectable the ItemSelectable object related to the item (in order to get width and height for the checks).
     * @param selectionRectangle the selection box.
     * @return whether the item is within the bounds of the selectionRectangle
     */
    private boolean selectIfWithinSelectionBox(Node item, SelectHelper.ItemSelectable itemSelectable, Rectangle selectionRectangle) {
        Bounds itemCoordinates = item.localToScreen(item.getLayoutBounds());
        Bounds selectionBoxCoordinates = selectionRectangle.localToScreen(selectionRectangle.getLayoutBounds());

        if (selectionBoxCoordinates == null || itemCoordinates == null) return false;

        return selectionBoxCoordinates.getMinX() < itemCoordinates.getMinX() + itemSelectable.getSelectableWidth() * getScaleX() / 2 &&
                itemCoordinates.getMinX() + itemSelectable.getSelectableWidth() * getScaleX() / 2 < selectionBoxCoordinates.getMinX() + selectionRectangle.getWidth() * getScaleX() &&
                selectionBoxCoordinates.getMinY() < itemCoordinates.getMinY() + itemSelectable.getSelectableHeight() * getScaleY() / 2 &&
                itemCoordinates.getMinY() + itemSelectable.getSelectableHeight() * getScaleY() / 2 < selectionBoxCoordinates.getMinY() + selectionRectangle.getHeight() * getScaleY();
    }
}
