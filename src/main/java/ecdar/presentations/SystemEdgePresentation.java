package ecdar.presentations;

import ecdar.abstractions.EcdarSystemEdge;
import ecdar.abstractions.EcdarSystem;
import ecdar.controllers.EcdarController;
import ecdar.controllers.SystemEdgeController;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.ItemDragHelper;
import ecdar.utility.helpers.SelectHelper;
import javafx.beans.property.DoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;

/**
 * Presentation for a system edge.
 */
public class SystemEdgePresentation extends Group implements SelectHelper.ItemSelectable {
    private final SystemEdgeController controller;

    private final ObservableList<Link> links = FXCollections.observableArrayList();

    /**
     * Constructor.
     * @param edge system edge to present
     * @param system system of the system edge
     */
    public SystemEdgePresentation(final EcdarSystemEdge edge, final EcdarSystem system) {
        controller = new EcdarFXMLLoader().loadAndGetController("SystemEdgePresentation.fxml", this);
        controller.setEdge(edge);
        controller.setSystem(system);
        controller.setSelectable(this);

        initializeBinding(edge);
    }

    private void initializeBinding(final EcdarSystemEdge edge) {
        final Link link = new Link();
        links.add(link);

        // Add the link to the view
        controller.root.getChildren().add(link);

        if (edge.isFinished()) {
            bindFinishedEdge(edge);
        } else {
            // Bind the link to the the edge source and the mouse position (snapped to the grid)
            link.startXProperty().bind(edge.getTempNode().getEdgeX());
            link.startYProperty().bind(edge.getTempNode().getEdgeY());

            // Bind to mouse position (snapped to the grid)
            link.endXProperty().bind(EcdarController.getActiveCanvasPresentation().mouseTracker.xProperty().subtract(5));
            link.endYProperty().bind(EcdarController.getActiveCanvasPresentation().mouseTracker.xProperty().subtract(5));
        }

        // If edge source and target changes, bind
        edge.getChildProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue != null && edge.getParent() != null) {
                bindFinishedEdge(edge);
            }
        }));
        edge.getParentProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue != null && edge.getChild() != null) {
                bindFinishedEdge(edge);
            }
        }));
    }

    /**
     * Use this for binding edge, when it is finished.
     * Binds the start of the first link to the child of the edge.
     * Binds the end of the last link to the parent of the edge.
     * @param edge edge to bind with
     */
    private void bindFinishedEdge(final EcdarSystemEdge edge) {
        final Link firstLink = links.get(0);
        final Link lastLink = links.get(links.size() - 1);

        firstLink.startXProperty().bind(edge.getChild().getEdgeX());
        firstLink.startYProperty().bind(edge.getChild().getEdgeY());

        lastLink.endXProperty().bind(edge.getParent().getEdgeX());
        lastLink.endYProperty().bind(edge.getParent().getEdgeY());
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
    public double getSelectableWidth() {
        return 0;
    }

    @Override
    public double getSelectableHeight() {
        return 0;
    }

    /**
     * Calls select on all selectable children.
     */
    @Override
    public void select() {
        getChildren().forEach(node -> {
            if (node instanceof SelectHelper.Selectable) {
                ((SelectHelper.Selectable) node).select();
            }
        });
    }

    /**
     * Calls deselect on all selectable children.
     */
    @Override
    public void deselect() {
        getChildren().forEach(node -> {
            if (node instanceof SelectHelper.Selectable) {
                ((SelectHelper.Selectable) node).deselect();
            }
        });
    }
}
