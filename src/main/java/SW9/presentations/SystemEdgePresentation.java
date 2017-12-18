package SW9.presentations;

import SW9.abstractions.EcdarSystemEdge;
import SW9.abstractions.SystemModel;
import SW9.controllers.SystemEdgeController;
import SW9.utility.colors.Color;
import SW9.utility.helpers.ItemDragHelper;
import SW9.utility.helpers.SelectHelper;
import javafx.beans.property.DoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;

public class SystemEdgePresentation extends Group implements SelectHelper.ItemSelectable {
    private final SystemEdgeController controller;

    private final ObservableList<Link> links = FXCollections.observableArrayList();

    public SystemEdgePresentation(final EcdarSystemEdge edge, final SystemModel system) {
        controller = new EcdarFXMLLoader().loadAndGetController("SystemEdgePresentation.fxml", this);
        controller.setEdge(edge);
        controller.setSystem(system);

        if (edge.getNails().isEmpty()) {
            final Link link = new Link();
            links.add(link);

            // Add the link to the view
            controller.root.getChildren().add(link);

            // Bind the link to the the edge source and the mouse position (snapped to the grid)
            link.startXProperty().bind(edge.getSource().getEdgeX());
            link.startYProperty().bind(edge.getSource().getEdgeY());

            // If target exists, bind to it
            // Else, bind to mouse position (snapped to the grid)
            if (edge.getTarget() != null) {
                link.endXProperty().bind(edge.getTarget().getEdgeX());
                link.endYProperty().bind(edge.getTarget().getEdgeY());
            } else {
                link.endXProperty().bind(CanvasPresentation.mouseTracker.gridXProperty());
                link.endYProperty().bind(CanvasPresentation.mouseTracker.gridYProperty());
            }
        }

        // When edge target changes, bind the last link to the new target
        edge.getTargetProperty().addListener(((observable, oldValue, newValue) -> {
            links.get(links.size() - 1).endXProperty().bind(newValue.getEdgeX());
            links.get(links.size() - 1).endYProperty().bind(newValue.getEdgeY());
        }));
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
