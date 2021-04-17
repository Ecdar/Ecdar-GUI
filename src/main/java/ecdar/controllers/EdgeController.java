package ecdar.controllers;

import ecdar.abstractions.*;
import ecdar.code_analysis.Nearable;
import ecdar.model_canvas.arrow_heads.SimpleArrowHead;
import ecdar.presentations.*;
import ecdar.utility.Highlightable;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
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

import static ecdar.presentations.Grid.GRID_SIZE;

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

            // Invalidate the list of edges (to update UI and errors)
            newEdge.sourceCircularProperty().addListener(observable -> {
                getComponent().removeEdge(getEdge());
                getComponent().addEdge(getEdge());
            });

            // Invalidate the list of edges (to update UI and errors)
            newEdge.targetCircularProperty().addListener(observable -> {
                getComponent().removeEdge(getEdge());
                getComponent().addEdge(getEdge());
            });

            // When an edge updates highlight property,
            // we want to update the view to reflect current highlight property
            edge.get().isHighlightedProperty().addListener(v -> {
                if(edge.get().getIsHighlighted()) {
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
                BindingHelper.bind(link, simpleArrowHead, newEdge.getSourceCircular(), newComponent.getBox().getXProperty(), newComponent.getBox().getYProperty());
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
                    final NailPresentation newNailPresentation = new NailPresentation(newNail, newEdge, newComponent, this);
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
                    final Nail nail1 = new Nail(newSourceCircular.xProperty().add(4 * GRID_SIZE), newSourceCircular.yProperty().subtract(GRID_SIZE));
                    final Nail nail2 = new Nail(newSourceCircular.xProperty().add(4 * GRID_SIZE), newSourceCircular.yProperty().add(GRID_SIZE));

                    // Add the nails to the nails collection (will draw links between them)
                    nails.addAll(nail1, nail2);

                    // Find the new first link (updated by adding nails to the collection) and bind it from the last nail to the target location
                    final Link newFirstLink = links.get(0);
                    BindingHelper.bind(newFirstLink, simpleArrowHead, newSourceCircular, nail1);
                } else {
                    BindingHelper.bind(firstLink, simpleArrowHead, newEdge.getSourceCircular(), newEdge.getTargetCircular());
                }
            } else {
                BindingHelper.bind(firstLink, simpleArrowHead, newEdge.getSourceCircular(), nails.get(0));
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

                        final DropDownMenu dropDownMenu = new DropDownMenu(
                                dropDownMenuHelperCircle
                        );


                        dropDownMenu.addMenuElement(getChangeStatusMenuElement(dropDownMenu));

                        dropDownMenu.addSpacerElement();

                        addEdgePropertyRow(dropDownMenu, "Add Select", Edge.PropertyType.SELECTION, link);
                        addEdgePropertyRow(dropDownMenu, "Add Guard", Edge.PropertyType.GUARD, link);
                        addEdgePropertyRow(dropDownMenu, "Add Update", Edge.PropertyType.UPDATE, link);

                        dropDownMenu.addSpacerElement();

                        dropDownMenu.addClickableAndDisableableListElement("Add Nail", getEdge().getIsLocked(), mouseEvent -> {
                            final double nailX = Math.round((DropDownMenu.x - getComponent().getBox().getX()) / GRID_SIZE) * GRID_SIZE;
                            final double nailY = Math.round((DropDownMenu.y - getComponent().getBox().getY()) / GRID_SIZE) * GRID_SIZE;
                            final Nail newNail = new Nail(nailX, nailY);

                            UndoRedoStack.pushAndPerform(
                                    () -> getEdge().insertNailAt(newNail, links.indexOf(link)),
                                    () -> getEdge().removeNail(newNail),
                                    "Nail added",
                                    "add-circle"
                            );
                            dropDownMenu.hide();
                        });
                        dropDownMenu.addSpacerElement();

                        dropDownMenu.addClickableAndDisableableListElement("Delete",getEdge().getIsLocked(), mouseEvent -> {
                            UndoRedoStack.pushAndPerform(() -> { // Perform
                                getComponent().removeEdge(getEdge());
                            }, () -> { // Undo
                                getComponent().addEdge(getEdge());
                            }, "Deleted edge " + getEdge(), "delete");
                            dropDownMenu.hide();
                        });

                        DropDownMenu.x = EcdarController.getActiveCanvasPresentation().mouseTracker.getGridX();
                        DropDownMenu.y = EcdarController.getActiveCanvasPresentation().mouseTracker.getGridY();
                        dropDownMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, event.getX(), event.getY());

                    } else if ((event.isShiftDown() && event.isPrimaryButtonDown()) || event.isMiddleButtonDown()) {
                        final double nailX = EcdarController.getActiveCanvasPresentation().mouseTracker.gridXProperty().subtract(getComponent().getBox().getXProperty()).doubleValue();
                        final double nailY = EcdarController.getActiveCanvasPresentation().mouseTracker.gridYProperty().subtract(getComponent().getBox().getYProperty()).doubleValue();

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
        }).setDisableable(getEdge().getIsLocked());
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
            if (nail.getPropertyType().equals(Edge.PropertyType.SELECTION)) data[Edge.PropertyType.SELECTION.getI()] = i;
            if (nail.getPropertyType().equals(Edge.PropertyType.GUARD)) data[Edge.PropertyType.GUARD.getI()] = i;
            if (nail.getPropertyType().equals(Edge.PropertyType.SYNCHRONIZATION)) data[Edge.PropertyType.SYNCHRONIZATION.getI()] = i;
            if (nail.getPropertyType().equals(Edge.PropertyType.UPDATE)) data[Edge.PropertyType.UPDATE.getI()] = i;

            if ((getEdge().getIsLocked().getValue()) || nail.getPropertyType().equals(type)) {
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
            final double nailX = Math.round((DropDownMenu.x - getComponent().getBox().getX()) / GRID_SIZE) * GRID_SIZE;
            final double nailY = Math.round((DropDownMenu.y - getComponent().getBox().getY()) / GRID_SIZE) * GRID_SIZE;

            final Nail newNail = new Nail(nailX, nailY);
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

    public void edgeExited() {
        isHoveringEdge.set(false);
    }

    @FXML
    public void edgePressed(final MouseEvent event) {
        if (!event.isShiftDown()) {
            event.consume();

            if (event.isShortcutDown()) {
                SelectHelper.addToSelection(this);
            } else {
                SelectHelper.select(this);
            }
        }
    }

    public void edgeDragged(final MouseEvent event){
        //Check if the edge is selected to ensure that the drag is not targeting a select, guard, update, or sync node
        if(SelectHelper.getSelectedElements().get(0) == this){
            DisplayableEdge oldEdge = edge.get();
            Location source, target;

            //Get the coordinates of the source of the original edge
            if(oldEdge.getSourceLocation() != null) {
                source = oldEdge.getSourceLocation();
            } else {
                return;
            }

            //Get the coordinates of the target of the original edge
            if(oldEdge.getTargetLocation() != null) {
                target = oldEdge.getTargetLocation();
            } else {
                return;
            }

            //Decide whether the source or the target of the edge should be updated
            boolean closestToTarget = (Math.abs(event.getX() - target.getX()) < Math.abs(event.getX() - source.getX())) &&
                    (Math.abs(event.getY() - target.getY()) < Math.abs(event.getY() - source.getY()));

            //Handle drag close to nails
            if(edge.get().getNails().size() > 0){
                if(!closestToTarget){
                    //Check if the drag is closer to the first nail than to source
                    Nail firstNail = edge.get().getNails().get(0);
                    boolean closestToFirstNail = getDistance(event.getX(), event.getY(), firstNail.getX(), firstNail.getY()) < getDistance(event.getX(), event.getY(), source.getX(), source.getY());

                    //If the drag is closest to the first node, no drag should be initiated
                    if(closestToFirstNail){
                        return;
                    }
                } else {
                    //Check if the drag is closer to the last nail than to target
                    Nail lastNail = edge.get().getNails().get(edge.get().getNails().size() - 1);
                    boolean closestToLastNail = getDistance(event.getX(), event.getY(), lastNail.getX(), lastNail.getY()) < getDistance(event.getX(), event.getY(), target.getX(), target.getY());

                    //If the drag is closest to the last node, no drag should be initiated
                    if(closestToLastNail){
                        return;
                    }
                }
            }

            if(closestToTarget){
                //The drag event occurred closest to the target, create a new edge
                final Edge newEdge;

                //Create the new edge with the same source as the old edge
                newEdge = new Edge(source, oldEdge.getStatus());

                //Add the new edge and remove the old (needed on top of thee push to the undo an redo stack)
                getComponent().addEdge(newEdge);
                getComponent().removeEdge(oldEdge);

                KeyboardTracker.registerKeybind(KeyboardTracker.ABANDON_EDGE, new Keybind(new KeyCodeCombination(KeyCode.ESCAPE), () -> {
                    getComponent().removeEdge(newEdge);
                    UndoRedoStack.forgetLast();
                }));

                UndoRedoStack.push(() -> { // Perform
                    //Add the new edge and remove the old
                    getComponent().addEdge(newEdge);
                    getComponent().removeEdge(oldEdge);

                }, () -> { // Undo
                    //Add the old edge back and remove the new
                    getComponent().addEdge(oldEdge);
                    getComponent().removeEdge(newEdge);
                }, "Updated edge", "add-circle");

                //Make the state of the new edge correspond with the state of the old
                newEdge.setColor(getColor());
                newEdge.setColorIntensity(getColorIntensity());
                newEdge.selectProperty().set(edge.get().getSelect());
                newEdge.guardProperty().set(edge.get().getGuard());
                newEdge.updateProperty().set(edge.get().getUpdate());

                if(edge.getValue() instanceof Edge) {
                    newEdge.syncProperty().set(((Edge) edge.get()).getSync());
                } else {
                    //ToDo: Handle setting synchronization on GroupedEdge
                }

                for (Nail n : edge.get().getNails()) {
                    newEdge.addNail(n);
                }
            } else {
                //The drag event occurred closest to the source
                final Edge newEdge;

                //Create the new edge with the same source as the old edge (is changed later)
                newEdge = new Edge(source, oldEdge.getStatus());

                //Set the source to a new MouseCircular, which will follow the mouse and handle setting the new source
                newEdge.sourceCircularProperty().set(new MouseCircular(newEdge, getComponent()));

                //Set the target to the same as the old edge
                newEdge.setTargetLocation(target);

                //Add the new edge and remove the old (needed on top of thee push to the undo an redo stack)
                getComponent().addEdge(newEdge);
                getComponent().removeEdge(oldEdge);

                KeyboardTracker.registerKeybind(KeyboardTracker.ABANDON_EDGE, new Keybind(new KeyCodeCombination(KeyCode.ESCAPE), () -> {
                    getComponent().removeEdge(newEdge);
                    UndoRedoStack.forgetLast();
                }));

                UndoRedoStack.push(() -> { // Perform
                    //Add the new edge and remove the old
                    getComponent().addEdge(newEdge);
                    getComponent().removeEdge(oldEdge);

                }, () -> { // Undo
                    //Add the old edge back and remove the new
                    getComponent().addEdge(oldEdge);
                    getComponent().removeEdge(newEdge);
                }, "Updated edge", "add-circle");

                //Make the state of the new edge correspond with the state of the old
                newEdge.setColor(getColor());
                newEdge.setColorIntensity(getColorIntensity());
                newEdge.selectProperty().set(edge.get().getSelect());
                newEdge.guardProperty().set(edge.get().getGuard());
                newEdge.updateProperty().set(edge.get().getUpdate());

                if(edge.getValue() instanceof Edge) {
                    newEdge.syncProperty().set(((Edge) edge.get()).getSync());
                } else {
                    //ToDo: Handle setting synchronization on GroupedEdge
                }

                for (Nail n : edge.get().getNails()) {
                    newEdge.addNail(n);
                }
            }
        }
    }

    @Override
    public void color(final Color color, final Color.Intensity intensity) {
        final DisplayableEdge edge = getEdge();

        // Set the color of the edge
        edge.setColorIntensity(intensity);
        edge.setColor(color);
    }

    @Override
    public Color getColor() {
        return getEdge().getColor();
    }

    @Override
    public Color.Intensity getColorIntensity() {
        return getEdge().getColorIntensity();
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
            if(node instanceof Highlightable) {
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
            if(node instanceof Highlightable) {
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

    private double getDistance(double x1, double y1, double x2, double y2){
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
}
