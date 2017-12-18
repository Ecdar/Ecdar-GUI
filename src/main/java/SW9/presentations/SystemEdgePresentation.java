package SW9.presentations;

import SW9.abstractions.EcdarSystemEdge;
import SW9.abstractions.Edge;
import SW9.abstractions.Location;
import SW9.abstractions.Nail;
import SW9.controllers.CanvasController;
import SW9.controllers.SystemEdgeController;
import SW9.model_canvas.arrow_heads.SimpleArrowHead;
import SW9.utility.UndoRedoStack;
import SW9.utility.colors.Color;
import SW9.utility.helpers.BindingHelper;
import SW9.utility.helpers.ItemDragHelper;
import SW9.utility.helpers.SelectHelper;
import com.jfoenix.controls.JFXPopup;
import com.uppaal.model.system.SystemEdge;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.input.MouseEvent;

public class SystemEdgePresentation extends Group implements SelectHelper.ItemSelectable {
    private final SystemEdgeController controller;

    private final ObservableList<Link> links = FXCollections.observableArrayList();

    public SystemEdgePresentation(final EcdarSystemEdge edge) {
        controller = new EcdarFXMLLoader().loadAndGetController("SystemEdgePresentation", this);
        controller.setEdge(edge);

        if (edge.getNails().isEmpty()) {
            final Link link = new Link();
            links.add(link);

            // Add the link and its arrowhead to the view
            controller.root.getChildren().add(link);

            link.startXProperty().bind(edge.getSource().getXProperty());
            link.startYProperty().bind(edge.getSource().getYProperty());
            link.endXProperty().bind(CanvasPresentation.mouseTracker.gridXProperty());
            link.endYProperty().bind(CanvasPresentation.mouseTracker.gridYProperty());
        }
    }

    /**
     * Does nothing, as this cannot change color.
     * @param color not used
     * @param intensity not used
     */
    @Override
    public void color(final Color color, final Color.Intensity intensity) {

    }

    /**
     * Returns null, as this cannot change color.
     * @return null
     */
    @Override
    public Color getColor() {
        return null;
    }

    /**
     * Returns null, as this cannot change color.
     * @return null
     */
    @Override
    public Color.Intensity getColorIntensity() {
        return null;
    }

    /**
     * Returns null, as this cannot change color.
     * @return null
     */
    @Override
    public ItemDragHelper.DragBounds getDragBounds() {
        return null;
    }

    @Override
    public DoubleProperty xProperty() {
        return layoutXProperty();
    }

    @Override
    public DoubleProperty yProperty() {
        return layoutYProperty();
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
    public void select() {
        getChildren().forEach(node -> {
            if (node instanceof SelectHelper.Selectable) {
                ((SelectHelper.Selectable) node).select();
            }
        });
    }

    @Override
    public void deselect() {
        getChildren().forEach(node -> {
            if (node instanceof SelectHelper.Selectable) {
                ((SelectHelper.Selectable) node).deselect();
            }
        });
    }
}
