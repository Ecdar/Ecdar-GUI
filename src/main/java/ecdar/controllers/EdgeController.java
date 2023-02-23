package ecdar.controllers;

import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.code_analysis.Nearable;
import ecdar.model_canvas.arrow_heads.SimpleArrowHead;
import ecdar.presentations.*;
import ecdar.utility.Highlightable;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.helpers.*;
import ecdar.utility.keyboard.Keybind;
import ecdar.utility.keyboard.KeyboardTracker;
import com.jfoenix.controls.JFXPopup;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class EdgeController implements Initializable, SelectHelper.ItemSelectable, Highlightable {
    private final ObservableList<Link> links = FXCollections.observableArrayList();
    private final ObjectProperty<DisplayableEdge> edge = new SimpleObjectProperty<>();
    private final ObjectProperty<Component> component = new SimpleObjectProperty<>();
    private final SimpleArrowHead simpleArrowHead = new SimpleArrowHead();
    private final SimpleBooleanProperty isHoveringEdge = new SimpleBooleanProperty(false);
    private final SimpleIntegerProperty timeHoveringEdge = new SimpleIntegerProperty(0);
    private final Map<Nail, NailPresentation> nailNailPresentationMap = new HashMap<>();
    public Group edgeRoot;
    private Runnable collapseNail;
    private Thread runningThread;
    private Consumer<Nail> enlargeNail;
    private Consumer<Nail> shrinkNail;
    private Circle dropDownMenuHelperCircle;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        initializeNailCollapse();

        edge.addListener((obsEdge, oldEdge, newEdge) -> {
            newEdge.sourceCircularProperty().addListener(getNewSourceCircularListener(newEdge));
            newEdge.targetCircularProperty().addListener(getNewTargetCircularListener(newEdge));
            component.addListener(getComponentChangeListener(newEdge));

            // When an edge updates highlight property,
            // we want to update the view to reflect current highlight property
            edge.get().isHighlightedProperty().addListener(v -> {
                if (edge.get().getIsHighlighted()) {
                    this.highlight();
                } else {
                    this.unhighlight();
                }
            });
        });

        initializeLinksListener();
        ensureNailsInFront();
        initializeSelectListener();
    }

    private void initializeSelectListener() {
        SelectHelper.elementsToBeSelected.addListener(new ListChangeListener<Nearable>() {
            @Override
            public void onChanged(final Change<? extends Nearable> c) {
                while (c.next()) {
                    if (c.getAddedSize() == 0) return;

                    for (final Nearable nearable : SelectHelper.elementsToBeSelected) {
                        if (nearable instanceof Edge) {
                            if (nearable.equals(getEdge())) {
                                SelectHelper.addToSelection(EdgeController.this);
                                edge.get().getNails().forEach(n -> SelectHelper.addToSelection(nailNailPresentationMap.get(n).getController()));
                                break;
                            }
                        }
                    }
                }
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

    private ChangeListener<Component> getComponentChangeListener(final DisplayableEdge newEdge) {
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
                BindingHelper.bind(link, simpleArrowHead, newEdge.getSourceCircular(), new MouseCircular(newEdge.getSourceCircular()));
            } else if (newEdge.getTargetCircular() != null) {
                edgeRoot.getChildren().add(simpleArrowHead);

                final Circular[] previous = {newEdge.getSourceCircular()};

                newEdge.getNails().forEach(nail -> {
                    final Link link = new Link();
                    if (newEdge.getStatus() == EdgeStatus.OUTPUT) link.makeDashed();
                    links.add(link);

                    final NailPresentation nailPresentation = new NailPresentation(nail, newEdge, getComponent(), this);
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

    private ListChangeListener<Nail> getNailsChangeListener(final DisplayableEdge newEdge, final Component newComponent) {
        return change -> {
            while (change.next()) {
                // There were added some nails
                change.getAddedSubList().forEach(newNail -> {
                    // Create a new nail presentation based on the abstraction added to the list
                    NailPresentation newNailPresentation = new NailPresentation(newNail, newEdge, newComponent, this);
                    nailNailPresentationMap.put(newNail, newNailPresentation);
                    edgeRoot.getChildren().addAll(newNailPresentation);

                    if (getComponent().getUnfinishedEdge() == null) {
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
                        if (getComponent().isAnyEdgeWithoutSource()) {
                            // The previous last link must end in the new nail
                            final Link firstLink = links.get(0);

                            // If the nail is the last in the list, bind it to the target location
                            // otherwise, bind it to the next nail
                            final int nailIndex = edge.get().getNails().indexOf(newNail);
                            if (nailIndex == edge.get().getNails().size() - 1) {
                                BindingHelper.bind(firstLink, newNail, newEdge.getTargetCircular());
                            } else {
                                final Nail nextNail = edge.get().getNails().get(nailIndex + 1);
                                BindingHelper.bind(firstLink, newNail, nextNail);
                            }

                            // Create a new link that will bind from the new nail to the mouse
                            final Link newLink = new Link();
                            if (newEdge.getStatus() == EdgeStatus.OUTPUT) newLink.makeDashed();
                            links.add(0, newLink);
                            BindingHelper.bind(newLink, newNail, new MouseCircular(newNail));
                            edgeRoot.getChildren().add(newLink);
                        } else {
                            // The previous last link must end in the new nail
                            final Link lastLink = links.get(links.size() - 1);

                            // If the nail is the first in the list, bind it to the source location
                            // otherwise, bind it to the previous nail
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
                            BindingHelper.bind(newLink, simpleArrowHead, newNail, new MouseCircular(newNail));
                            edgeRoot.getChildren().add(newLink);
                        }
                    }
                });

                change.getRemoved().forEach(removedNail -> {
                    final int removedIndex = change.getFrom();
                    final NailPresentation removedNailPresentation = nailNailPresentationMap.remove(removedNail);
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

    private ChangeListener<Circular> getNewSourceCircularListener(final DisplayableEdge newEdge) {
        // When the source location is set, finish drawing the edge
        return (obsSourceLocation, oldSourceCircular, newSourceCircular) -> {
            // If the nails list is empty, directly connect the source and target locations
            // otherwise, bind the line from the source to the first nail
            final Link firstLink = links.get(0);
            final ObservableList<Nail> nails = getEdge().getNails();
            if (nails.size() == 0) {
                // Check if the source and target locations are the same, if they are, add proper amount of nails
                if (newEdge.getTargetCircular().equals(newSourceCircular)) {
                    final Nail nail1 = new Nail(newSourceCircular.xProperty().add(Ecdar.CANVAS_PADDING * 4), newSourceCircular.yProperty().subtract(Ecdar.CANVAS_PADDING));
                    final Nail nail2 = new Nail(newSourceCircular.xProperty().add(Ecdar.CANVAS_PADDING * 4), newSourceCircular.yProperty().add(Ecdar.CANVAS_PADDING));

                    // Add the nails to the nails collection (will draw links between them)
                    nails.addAll(nail1, nail2);

                    // Find the new first link (updated by adding nails to the collection) and bind it from the last nail to the target location
                    final Link newFirstLink = links.get(0);
                    BindingHelper.bind(newFirstLink, newSourceCircular, nail1);
                } else {
                    BindingHelper.bind(firstLink, newEdge.getSourceCircular(), newEdge.getTargetCircular());
                }
            } else {
                BindingHelper.bind(firstLink, newEdge.getSourceCircular(), nails.get(0));
            }

            KeyboardTracker.unregisterKeybind(KeyboardTracker.ABANDON_EDGE);

            // When the source location is set the
            edgeRoot.setMouseTransparent(false);
        };
    }

    private ChangeListener<Circular> getNewTargetCircularListener(final DisplayableEdge newEdge) {
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

    private void initializeLinksListener() {
        // Tape
        dropDownMenuHelperCircle = new Circle(5);
        dropDownMenuHelperCircle.setOpacity(0);
        dropDownMenuHelperCircle.setMouseTransparent(true);
        edgeRoot.getChildren().add(dropDownMenuHelperCircle);

        links.addListener(new ListChangeListener<Link>() {
            @Override
            public void onChanged(final Change<? extends Link> c) {
                links.forEach((link) -> link.setOnMousePressed(event -> {
                    if (event.isSecondaryButtonDown() && getComponent().getUnfinishedEdge() == null) {
                        event.consume();

                        final DropDownMenu dropDownMenu = new DropDownMenu(dropDownMenuHelperCircle);

                        dropDownMenu.addMenuElement(getChangeStatusMenuElement(dropDownMenu));

                        dropDownMenu.addSpacerElement();

                        addEdgePropertyRow(dropDownMenu, "Add Select", Edge.PropertyType.SELECTION, link);
                        addEdgePropertyRow(dropDownMenu, "Add Guard", Edge.PropertyType.GUARD, link);
                        addEdgePropertyRow(dropDownMenu, "Add Update", Edge.PropertyType.UPDATE, link);

                        dropDownMenu.addSpacerElement();
                        dropDownMenu.addClickableAndDisableableListElement("Add Nail", getEdge().getIsLockedProperty(), mouseEvent -> {
                            final Nail newNail = getNewNailBasedOnDropdownPosition();

                            UndoRedoStack.pushAndPerform(
                                    () -> getEdge().insertNailAt(newNail, links.indexOf(link)),
                                    () -> getEdge().removeNail(newNail),
                                    "Nail added",
                                    "add-circle"
                            );
                            dropDownMenu.hide();
                        });
                        dropDownMenu.addSpacerElement();

                        dropDownMenu.addClickableAndDisableableListElement("Delete", getEdge().getIsLockedProperty(), mouseEvent -> {
                            UndoRedoStack.pushAndPerform(() -> { // Perform
                                getComponent().removeEdge(getEdge());
                            }, () -> { // Undo
                                getComponent().addEdge(getEdge());
                            }, "Deleted edge " + getEdge(), "delete");
                            dropDownMenu.hide();
                        });
                        
                        dropDownMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, event.getX() * EcdarController.getActiveCanvasZoomFactor().get(), event.getY() * EcdarController.getActiveCanvasZoomFactor().get());

                    } else if ((event.isShiftDown() && event.isPrimaryButtonDown()) || event.isMiddleButtonDown()) {
                        final double nailX = EcdarController.getActiveCanvasPresentation().mouseTracker.xProperty().subtract(getComponent().getBox().getXProperty()).doubleValue();
                        final double nailY = EcdarController.getActiveCanvasPresentation().mouseTracker.yProperty().subtract(getComponent().getBox().getYProperty()).doubleValue();

                        final Nail newNail = new Nail(nailX, nailY);

                        UndoRedoStack.pushAndPerform(
                                () -> getEdge().insertNailAt(newNail, links.indexOf(link)),
                                () -> getEdge().removeNail(newNail),
                                "Nail added",
                                "add-circle"
                        );
                    }
                }));
            }
        });
    }

    public MenuElement getChangeStatusMenuElement(DropDownMenu dropDownMenu) {
        // Switch between input and output edge
        final String status;
        if (getEdge().getStatus() == EdgeStatus.INPUT) {
            status = "Change to output edge";
        } else {
            status = "Change to input edge";
        }

        return new MenuElement(status, mouseEvent -> {
            UndoRedoStack.pushAndPerform(
                    () -> switchEdgeStatus(),
                    () -> switchEdgeStatus(),
                    "Switch edge status",
                    "switch"
            );
            dropDownMenu.hide();
        }).setDisableable(getEdge().getIsLockedProperty());
    }

    public MenuElement getMultiSyncEdgeMenuElement(DropDownMenu dropDownMenu) {
        return new MenuElement("Make edge multi-sync", mouseEvent -> {
            Nail syncNail = getEdge().getNails().stream().filter((nail) -> nail.getPropertyType() == DisplayableEdge.PropertyType.SYNCHRONIZATION).findFirst().get();
            TagPresentation previousTagPresentation = nailNailPresentationMap.get(syncNail).getController().propertyTag;

            final Edge singleEdge = (Edge) getEdge();
            final GroupedEdge multiEdge = new GroupedEdge((Edge) getEdge());

            UndoRedoStack.pushAndPerform(
                    () -> {
                        component.get().removeEdge(singleEdge);
                        component.get().addEdge(multiEdge);
                        nailNailPresentationMap.get(syncNail).getController().propertyTag = new MultiSyncTagPresentation(multiEdge,
                                () -> updateSyncLabelOnNail(nailNailPresentationMap.get(syncNail), previousTagPresentation));
                    },
                    () -> {
                        component.get().removeEdge(multiEdge);
                        component.get().addEdge(singleEdge);
                        nailNailPresentationMap.get(syncNail).getController().propertyTag = previousTagPresentation;
                    },
                    "Turned edge into multi-sync edge",
                    "switch"
            );
            dropDownMenu.hide();
        }).setDisableable(getEdge().getIsLockedProperty());
    }

    /**
     * Updates the synchronization label and tag.
     * The update depends on the edge I/O status.
     *
     * @param nailPresentation NailPresentation to update label of
     * @param propertyTag      Property tag to update
     */
    private void updateSyncLabelOnNail(final NailPresentation nailPresentation, final TagPresentation propertyTag) {
        final Label propertyLabel = nailPresentation.getController().propertyLabel;

        // Show ? or ! dependent on edge I/O status
        if (getEdge().ioStatus.get().equals(EdgeStatus.INPUT)) {
            propertyLabel.setText("?");
            propertyTag.setPlaceholder("Input");
        } else {
            propertyLabel.setText("!");
            propertyTag.setPlaceholder("Output");
        }
    }

    private void switchEdgeStatus() {
        getEdge().switchStatus();

        // Update link
        if (getEdge().getStatus() == EdgeStatus.INPUT) {
            links.forEach(Link::makeSolid);
        } else {
            links.forEach(Link::makeDashed);
        }
    }

    private void addEdgePropertyRow(final DropDownMenu dropDownMenu, final String rowTitle, final Edge.PropertyType type, final Link link) {
        final SimpleBooleanProperty isDisabled = new SimpleBooleanProperty(false);

        final int[] data = {-1, -1, -1, -1};

        int i = 0;
        for (final Nail nail : getEdge().getNails()) {
            if (nail.getPropertyType().equals(Edge.PropertyType.SELECTION))
                data[Edge.PropertyType.SELECTION.getI()] = i;
            if (nail.getPropertyType().equals(Edge.PropertyType.GUARD)) data[Edge.PropertyType.GUARD.getI()] = i;
            if (nail.getPropertyType().equals(Edge.PropertyType.SYNCHRONIZATION))
                data[Edge.PropertyType.SYNCHRONIZATION.getI()] = i;
            if (nail.getPropertyType().equals(Edge.PropertyType.UPDATE)) data[Edge.PropertyType.UPDATE.getI()] = i;

            if ((getEdge().getIsLockedProperty().getValue()) || nail.getPropertyType().equals(type)) {
                isDisabled.set(true);
            }

            i++;
        }

        final SimpleIntegerProperty insertAt = new SimpleIntegerProperty(links.indexOf(link));
        final int clickedLinkedIndex = links.indexOf(link);

        // Check the elements before me, and ensure that I am placed after these
        for (int i1 = type.getI() - 1; i1 >= 0; i1--) {
            if (data[i1] != -1 && data[i1] >= clickedLinkedIndex) {
                insertAt.set(data[i1] + 1);
            }
        }

        // Check the elements after me, and ensure that I am placed before these
        for (int i1 = type.getI() + 1; i1 < data.length; i1++) {
            if (data[i1] != -1 && data[i1] < clickedLinkedIndex) {
                insertAt.set(data[i1]);
            }
        }

        dropDownMenu.addClickableAndDisableableListElement(rowTitle, isDisabled, event -> {
            final Nail newNail = getNewNailBasedOnDropdownPosition();
            newNail.setPropertyType(type);

            UndoRedoStack.pushAndPerform(
                    () -> getEdge().insertNailAt(newNail, insertAt.get()),
                    () -> getEdge().removeNail(newNail),
                    "Nail property added (" + type + ")",
                    "add-circle"
            );
            dropDownMenu.hide();
        });
    }

    private Nail getNewNailBasedOnDropdownPosition() {
        final double nailX = DropDownMenu.x / EcdarController.getActiveCanvasZoomFactor().get();
        final double nailY = DropDownMenu.y / EcdarController.getActiveCanvasZoomFactor().get();
        return new Nail(nailX, nailY);
    }

    public DisplayableEdge getEdge() {
        return edge.get();
    }

    public void setEdge(final DisplayableEdge edge) {
        this.edge.set(edge);
    }

    public ObjectProperty<DisplayableEdge> edgeProperty() {
        return edge;
    }

    public Component getComponent() {
        return component.get();
    }

    public void setComponent(final Component component) {
        this.component.set(component);
    }

    public Map<Nail, NailPresentation> getNailNailPresentationMap() {
        return nailNailPresentationMap;
    }

    public ObjectProperty<Component> componentProperty() {
        return component;
    }

    public void edgeEntered() {
        isHoveringEdge.set(true);
        if ((runningThread != null && runningThread.isAlive())) return; // Do not re-animate

        timeHoveringEdge.set(500);
        runningThread = new Thread(collapseNail);
        runningThread.start();

        getEdge().getNails().forEach(enlargeNail);
    }

    @FXML
    public void edgeExited() {
        isHoveringEdge.set(false);
    }

    @FXML
    public void edgePressed(final MouseEvent event) {
        if (!event.isShiftDown()) {
            event.consume();

            if (event.isShortcutDown()) {
                if (SelectHelper.getSelectedElements().contains(this)) {
                    SelectHelper.deselect(this);
                    edge.get().getNails().forEach(n -> SelectHelper.deselect(nailNailPresentationMap.get(n).getController()));
                } else {
                    SelectHelper.addToSelection(this);
                    edge.get().getNails().forEach(n -> SelectHelper.addToSelection(nailNailPresentationMap.get(n).getController()));
                }
            } else {
                SelectHelper.select(this);
                edge.get().getNails().forEach(n -> SelectHelper.addToSelection(nailNailPresentationMap.get(n).getController()));
            }
        }
    }

    @FXML
    public void edgeDragged(final MouseEvent event) {
        if (event.getTarget() instanceof TagPresentation) return;
        // Check if the edge is selected to ensure that the drag is not targeting a nail
        if (SelectHelper.getSelectedElements().size() == 0 || SelectHelper.getSelectedElements().get(0) == this) {
            DisplayableEdge draggedEdge = edge.get();
            Location source, target;

            if (draggedEdge.getSourceLocation() != null) {
                source = draggedEdge.getSourceLocation();
            } else {
                return;
            }

            if (draggedEdge.getTargetLocation() != null) {
                target = draggedEdge.getTargetLocation();
            } else {
                return;
            }

            // Decide whether the source or the target of the edge should be updated
            boolean closestToTarget = getDistance(event.getX(), event.getY(), target.getX(), target.getY()) < getDistance(event.getX(), event.getY(), source.getX(), source.getY());

            // Handle drag closer to nails than locations
            if (edge.get().getNails().size() > 0) {
                boolean dragIsCloserToNail;

                if (!closestToTarget) {
                    // Check if the drag is closer to the first nail than to source
                    Nail firstNail = edge.get().getNails().get(0);
                    dragIsCloserToNail = getDistance(event.getX(), event.getY(), firstNail.getX(), firstNail.getY()) < getDistance(event.getX(), event.getY(), source.getX(), source.getY());
                } else {
                    // Check if the drag is closer to the last nail than to target
                    Nail lastNail = edge.get().getNails().get(edge.get().getNails().size() - 1);
                    dragIsCloserToNail = getDistance(event.getX(), event.getY(), lastNail.getX(), lastNail.getY()) < getDistance(event.getX(), event.getY(), target.getX(), target.getY());
                }

                // If the drag is closer to a nail than the locations, no drag should be initiated
                if (dragIsCloserToNail) {
                    return;
                }
            }

            if (!closestToTarget) {
                getComponent().previousLocationForDraggedEdge = source;
                draggedEdge.setSourceLocation(null);

                KeyboardTracker.registerKeybind(KeyboardTracker.ABANDON_EDGE,
                        new Keybind(new KeyCodeCombination(KeyCode.ESCAPE), () -> draggedEdge.setSourceLocation(getComponent().previousLocationForDraggedEdge)));
            } else {
                getComponent().previousLocationForDraggedEdge = target;
                draggedEdge.setTargetLocation(null);

                KeyboardTracker.registerKeybind(KeyboardTracker.ABANDON_EDGE,
                        new Keybind(new KeyCodeCombination(KeyCode.ESCAPE), () -> draggedEdge.setTargetLocation(getComponent().previousLocationForDraggedEdge)));
            }
        }
    }

    @Override
    public void color(final EnabledColor color) {
        final DisplayableEdge edge = getEdge();

        // Set the color of the edge
        edge.setColor(color);
    }

    @Override
    public EnabledColor getColor() {
        return getEdge().getColor();
    }

    @Override
    public ItemDragHelper.DragBounds getDragBounds() {
        return ItemDragHelper.DragBounds.generateLooseDragBounds();
    }

    @Override
    public void select() {
        edgeRoot.getChildren().forEach(node -> {
            if (node instanceof SelectHelper.Selectable) {
                ((SelectHelper.Selectable) node).select();
            }
        });
    }

    @Override
    public void deselect() {
        edgeRoot.getChildren().forEach(node -> {
            if (node instanceof SelectHelper.Selectable) {
                ((SelectHelper.Selectable) node).deselect();
            }
        });
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

    @Override
    public DoubleProperty xProperty() {
        return edgeRoot.layoutXProperty();
    }

    @Override
    public DoubleProperty yProperty() {
        return edgeRoot.layoutYProperty();
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

    private double getDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
}
