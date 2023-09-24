package ecdar.controllers;

import ecdar.abstractions.Component;
import ecdar.abstractions.Edge;
import ecdar.abstractions.EdgeStatus;
import ecdar.abstractions.Nail;
import ecdar.model_canvas.arrow_heads.SimpleArrowHead;
import ecdar.presentations.Link;
import ecdar.presentations.NailPresentation;
import ecdar.presentations.SimNailPresentation;
import ecdar.presentations.SimulationPresentation;
import ecdar.utility.Highlightable;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.helpers.BindingHelper;
import ecdar.utility.helpers.Circular;
import ecdar.utility.helpers.ItemDragHelper;
import ecdar.utility.helpers.SelectHelper;
import ecdar.utility.keyboard.KeyboardTracker;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.util.Duration;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * The controller for the edge shown in the {@link SimulationPresentation}
 */
public class SimEdgeController implements Initializable, Highlightable {
    private final ObservableList<Link> links = FXCollections.observableArrayList();
    private final ObjectProperty<Edge> edge = new SimpleObjectProperty<>();
    private final ObjectProperty<Component> component = new SimpleObjectProperty<>();
    private final SimpleArrowHead simpleArrowHead = new SimpleArrowHead();
    private final SimpleBooleanProperty isHoveringEdge = new SimpleBooleanProperty(false);
    private final SimpleIntegerProperty timeHoveringEdge = new SimpleIntegerProperty(0);
    private final Map<Nail, SimNailPresentation> nailNailPresentationMap = new HashMap<>();
    public Group edgeRoot;
    private Runnable collapseNail;
    private Consumer<Nail> enlargeNail;
    private Consumer<Nail> shrinkNail;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        initializeNailCollapse();

        edge.addListener((obsEdge, oldEdge, newEdge) -> {
            newEdge.targetCircularProperty().addListener(getNewTargetCircularListener(newEdge));
            component.addListener(getComponentChangeListener(newEdge));

            // Invalidate the list of edges (to update UI and errors)
            newEdge.targetCircularProperty().addListener(observable -> {
                getComponent().removeEdge(getEdge());
                getComponent().addEdge(getEdge());
            });

            // When an edge updates highlight property,
            // we want to update the view to reflect current highlight property
            edge.get().isHighlightedProperty().addListener(v -> {
                if (edge.get().isHighlighted()) {
                    this.highlight();
                } else {
                    this.unhighlight();
                }
            });

            // When an edge updates highlight property,
            // we want to update the view to reflect current highlight property
            edge.get().isHighlightedForReachabilityProperty().addListener(v -> {
                if (edge.get().getIsHighlightedForReachability()) {
                    this.highlightSpecialColor();
                } else {
                    this.unhighlight();
                }
            });
        });

        ensureNailsInFront();
    }

    public void highlightSpecialColor() {
        edgeRoot.getChildren().forEach(node -> {
            if (node instanceof Highlightable) {
                ((Highlightable) node).highlightPurple();
            }
        });
    }

    private void ensureNailsInFront() {
        // When ever changes happens to the children of the edge root force nails in front and other elements to back
        edgeRoot.getChildren().addListener((ListChangeListener<? super Node>) c -> {
            while (c.next()) {
                for (int i = 0; i < c.getAddedSize(); i++) {
                    final Node node = c.getAddedSubList().get(i);
                    if (node instanceof NailPresentation) {
                        node.toFront();
                    } else {
                        node.toBack();
                    }
                }
            }
        });
    }

    private ChangeListener<Component> getComponentChangeListener(final Edge newEdge) {
        return (obsComponent, oldComponent, newComponent) -> {
            // Draw new edge from a location
            if (newEdge.getNails().isEmpty() && newEdge.getTargetCircular() == null) {
                final Link link = new Link();
                // Make dashed line, if output edge
                if (newEdge.getStatus() == EdgeStatus.OUTPUT) link.makeDashed();
                links.add(link);

                // Add the link and its arrowhead to the view
                edgeRoot.getChildren().addAll(link, simpleArrowHead);

                // Bind the first link and the arrowhead from the source location to the mouse
                BindingHelper.bind(link, simpleArrowHead, newEdge.getSourceCircular(),
                        newComponent.getBox().getXProperty(), newComponent.getBox().getYProperty());
            } else if (newEdge.getTargetCircular() != null) {
                edgeRoot.getChildren().add(simpleArrowHead);

                final Circular[] previous = {newEdge.getSourceCircular()};

                newEdge.getNails().forEach(nail -> {
                    final Link link = new Link();
                    if (newEdge.getStatus() == EdgeStatus.OUTPUT) link.makeDashed();
                    links.add(link);

                    final SimNailPresentation nailPresentation = new SimNailPresentation(nail, newEdge, getComponent(), this);
                    nailNailPresentationMap.put(nail, nailPresentation);

                    edgeRoot.getChildren().addAll(link, nailPresentation);
                    BindingHelper.bind(link, previous[0], nail);

                    previous[0] = nail;
                });

                final Link link = new Link();
                if (newEdge.getStatus() == EdgeStatus.OUTPUT) link.makeDashed();
                links.add(link);

                edgeRoot.getChildren().add(link);
                BindingHelper.bind(link, simpleArrowHead, previous[0], newEdge.getTargetCircular());
            }

            // Changes are made to the nails list
            newEdge.getNails().addListener(getNailsChangeListener(newEdge, newComponent));
        };
    }

    private ListChangeListener<Nail> getNailsChangeListener(final Edge newEdge, final Component newComponent) {
        return change -> {
            while (change.next()) {
                // There were added some nails
                change.getAddedSubList().forEach(newNail -> {
                    // Create a new nail presentation based on the abstraction added to the list
                    final SimNailPresentation newNailPresentation = new SimNailPresentation(newNail, newEdge, newComponent, this);
                    nailNailPresentationMap.put(newNail, newNailPresentation);

                    edgeRoot.getChildren().addAll(newNailPresentation);

                    if (newEdge.getTargetCircular() != null) {
                        final int indexOfNewNail = edge.get().getNails().indexOf(newNail);

                        final Link newLink = new Link();
                        if (newEdge.getStatus() == EdgeStatus.OUTPUT) newLink.makeDashed();
                        final Link pressedLink = links.get(indexOfNewNail);
                        links.add(indexOfNewNail, newLink);

                        edgeRoot.getChildren().addAll(newLink);

                        Circular oldStart = getEdge().getSourceCircular();
                        Circular oldEnd = getEdge().getTargetCircular();

                        if (indexOfNewNail != 0) {
                            oldStart = getEdge().getNails().get(indexOfNewNail - 1);
                        }

                        if (indexOfNewNail != getEdge().getNails().size() - 1) {
                            oldEnd = getEdge().getNails().get(indexOfNewNail + 1);
                        }

                        BindingHelper.bind(newLink, oldStart, newNail);

                        if (oldEnd.equals(getEdge().getTargetCircular())) {
                            BindingHelper.bind(pressedLink, simpleArrowHead, newNail, oldEnd);
                        } else {
                            BindingHelper.bind(pressedLink, newNail, oldEnd);
                        }

                        if (isHoveringEdge.get()) {
                            enlargeNail.accept(newNail);
                        }
                    } else {
                        // The previous last link must end in the new nail
                        final Link lastLink = links.get(links.size() - 1);

                        // If the nail is the first in the list, bind it to the source location
                        // otherwise, bind it the the previous nail
                        final int nailIndex = edge.get().getNails().indexOf(newNail);
                        if (nailIndex == 0) {
                            BindingHelper.bind(lastLink, newEdge.getSourceCircular(), newNail);
                        } else {
                            final Nail previousNail = edge.get().getNails().get(nailIndex - 1);
                            BindingHelper.bind(lastLink, previousNail, newNail);
                        }

                        // Create a new link that will bind from the new nail to the mouse
                        final Link newLink = new Link();
                        if (newEdge.getStatus() == EdgeStatus.OUTPUT) newLink.makeDashed();
                        links.add(newLink);
                        BindingHelper.bind(newLink, simpleArrowHead, newNail, newComponent.getBox().getXProperty(), newComponent.getBox().getYProperty());
                        edgeRoot.getChildren().add(newLink);
                    }
                });

                change.getRemoved().forEach(removedNail -> {
                    final int removedIndex = change.getFrom();
                    final SimNailPresentation removedNailPresentation = nailNailPresentationMap.remove(removedNail);
                    final Link danglingLink = links.get(removedIndex + 1);
                    edgeRoot.getChildren().remove(removedNailPresentation);
                    edgeRoot.getChildren().remove(links.get(removedIndex));

                    Circular newFrom = getEdge().getSourceCircular();
                    Circular newTo = getEdge().getTargetCircular();

                    if (removedIndex > 0) {
                        newFrom = getEdge().getNails().get(removedIndex - 1);
                    }

                    if (removedIndex - 1 != getEdge().getNails().size() - 1) {
                        newTo = getEdge().getNails().get(removedIndex);
                    }

                    if (newTo.equals(getEdge().getTargetCircular())) {
                        BindingHelper.bind(danglingLink, simpleArrowHead, newFrom, newTo);
                    } else {
                        BindingHelper.bind(danglingLink, newFrom, newTo);
                    }
                    links.remove(removedIndex);
                });
            }
        };
    }

    private ChangeListener<Circular> getNewTargetCircularListener(final Edge newEdge) {
        // When the target location is set, finish drawing the edge
        return (obsTargetLocation, oldTargetCircular, newTargetCircular) -> {
            // If the nails list is empty, directly connect the source and target locations
            // otherwise, bind the line from the last nail to the target location
            final Link lastLink = links.get(links.size() - 1);
            final ObservableList<Nail> nails = getEdge().getNails();
            if (nails.size() == 0) {
                BindingHelper.bind(lastLink, simpleArrowHead, newEdge.getSourceCircular(), newEdge.getTargetCircular());
            } else {
                final Nail lastNail = nails.get(nails.size() - 1);
                BindingHelper.bind(lastLink, simpleArrowHead, lastNail, newEdge.getTargetCircular());
            }

            KeyboardTracker.unregisterKeybind(KeyboardTracker.ABANDON_EDGE);

            // When the target location is set the
            edgeRoot.setMouseTransparent(false);
        };
    }

    /**
     * Initializes functionality to enlarge, shirk, and collapse nails
     */
    private void initializeNailCollapse() {
        enlargeNail = nail -> {
            if (!nail.getPropertyType().equals(Edge.PropertyType.NONE)) return;
            final Timeline animation = new Timeline();

            final KeyValue radius0 = new KeyValue(nail.radiusProperty(), NailPresentation.COLLAPSED_RADIUS);
            final KeyValue radius2 = new KeyValue(nail.radiusProperty(), NailPresentation.HOVERED_RADIUS * 1.2);
            final KeyValue radius1 = new KeyValue(nail.radiusProperty(), NailPresentation.HOVERED_RADIUS);

            final KeyFrame kf1 = new KeyFrame(Duration.millis(0), radius0);
            final KeyFrame kf2 = new KeyFrame(Duration.millis(80), radius2);
            final KeyFrame kf3 = new KeyFrame(Duration.millis(100), radius1);

            animation.getKeyFrames().addAll(kf1, kf2, kf3);
            animation.play();
        };
        shrinkNail = nail -> {
            if (!nail.getPropertyType().equals(Edge.PropertyType.NONE)) return;
            final Timeline animation = new Timeline();

            final KeyValue radius0 = new KeyValue(nail.radiusProperty(), NailPresentation.COLLAPSED_RADIUS);
            final KeyValue radius1 = new KeyValue(nail.radiusProperty(), NailPresentation.HOVERED_RADIUS);

            final KeyFrame kf1 = new KeyFrame(Duration.millis(0), radius1);
            final KeyFrame kf2 = new KeyFrame(Duration.millis(100), radius0);

            animation.getKeyFrames().addAll(kf1, kf2);
            animation.play();
        };

        collapseNail = () -> {
            final int interval = 50;
            int previousValue = 1;

            try {
                while (true) {
                    Thread.sleep(interval);

                    if (isHoveringEdge.get()) {
                        // Do not let the timer go above this threshold
                        if (timeHoveringEdge.get() <= 500) {
                            timeHoveringEdge.set(timeHoveringEdge.get() + interval);
                        }
                    } else {
                        timeHoveringEdge.set(timeHoveringEdge.get() - interval);
                    }

                    if (previousValue >= 0 && timeHoveringEdge.get() < 0) {
                        // Run on UI thread
                        Platform.runLater(() -> {
                            // Collapse all nails
                            getEdge().getNails().forEach(shrinkNail);
                        });
                        break;
                    }
                    previousValue = timeHoveringEdge.get();
                }

            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        };
    }

    public Edge getEdge() {
        return edge.get();
    }

    public void setEdge(final Edge edge) {
        this.edge.set(edge);
    }

    public ObjectProperty<Edge> edgeProperty() {
        return edge;
    }

    public Component getComponent() {
        return component.get();
    }

    public void setComponent(final Component component) {
        this.component.set(component);
    }

    public ObjectProperty<Component> componentProperty() {
        return component;
    }

    /**
     * Colors the edge model
     *
     * @param color     the new color of the edge
     */
    public void color(final EnabledColor color) {
        final Edge edge = getEdge();

        // Set the color of the edge
        edge.setColor(color);
    }

    public EnabledColor getColor() {
        return getEdge().getColor();
    }

    public ItemDragHelper.DragBounds getDragBounds() {
        return ItemDragHelper.DragBounds.generateLooseDragBounds();
    }

    /***
     * Highlights the child nodes of the edge
     */
    @Override
    public void highlight() {
        // Clear the currently selected elements, so we don't have multiple things highlighted/selected
        SelectHelper.clearSelectedElements();
        edgeRoot.getChildren().forEach(node -> {
            if (node instanceof Highlightable) {
                ((Highlightable) node).highlight();
            }
        });
    }

    /***
     * Removes the highlight from child nodes
     */
    @Override
    public void unhighlight() {
        edgeRoot.getChildren().forEach(node -> {
            if (node instanceof Highlightable) {
                ((Highlightable) node).unhighlight();
            }
        });
    }

    public DoubleProperty xProperty() {
        return edgeRoot.layoutXProperty();
    }

    public DoubleProperty yProperty() {
        return edgeRoot.layoutYProperty();
    }

    public double getX() {
        return xProperty().get();
    }

    public double getY() {
        return yProperty().get();
    }
}
