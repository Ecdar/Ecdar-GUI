package ecdar.controllers;

import com.jfoenix.controls.JFXRippler;
import com.jfoenix.skins.ValidationPane;
import ecdar.abstractions.*;
import ecdar.presentations.*;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.SelectHelper;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ResourceBundle;

public class CanvasShellController implements Initializable {
    public Pane root;

    public StackPane toolbar;
    public JFXRippler zoomIn;
    public JFXRippler zoomOut;
    public JFXRippler zoomToFit;
    public JFXRippler resetZoom;

    public CanvasPresentation canvasPresentation;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        canvasPresentation.getController().activeComponentProperty().addListener(((observable, oldValue, newValue) -> {
            toolbar.setVisible(newValue instanceof Component);
            canvasPresentation.getController().zoomHelper.setActive(newValue instanceof Component);
        }));
    }
    @FXML
    private void zoomInClicked() {
        canvasPresentation.getController().zoomHelper.zoomIn();
    }

    @FXML
    private void zoomOutClicked() {
        canvasPresentation.getController().zoomHelper.zoomOut();
    }

    @FXML
    private void zoomToFitClicked() {
        canvasPresentation.getController().zoomHelper.zoomToFit();
    }

    @FXML
    private void resetZoomClicked() {
        canvasPresentation.getController().zoomHelper.resetZoom();
    }

    @FXML
    public void startDragBox(final MouseEvent event) {
        // If the target is selectable, we are probably trying to drag the selection, so do not update the selection
        if(!event.isPrimaryButtonDown() || event.getTarget() instanceof SelectHelper.ItemSelectable) return;

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

        canvasPresentation.setOnMouseDragged(e -> {
            selectionRectangle.setX(Math.min(e.getX(), mouseDownX));
            selectionRectangle.setWidth(Math.abs(e.getX() - mouseDownX));
            selectionRectangle.setY(Math.min(e.getY(), mouseDownY));
            selectionRectangle.setHeight(Math.abs(e.getY() - mouseDownY));

            SelectHelper.clearSelectedElements();

            // Recursively find all relevant ItemSelectable Nodes in the scene
            updateSelection((Parent) canvasPresentation.getChildren().get(1), selectionRectangle);
        });

        canvasPresentation.setOnMouseReleased(e -> canvasPresentation.getChildren().remove(selectionRectangle));
        canvasPresentation.getChildren().add(selectionRectangle);
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
