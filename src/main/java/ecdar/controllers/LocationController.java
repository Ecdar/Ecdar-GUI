package ecdar.controllers;

import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.backend.BackendHelper;
import ecdar.code_analysis.CodeAnalysis;
import ecdar.code_analysis.Nearable;
import ecdar.presentations.*;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.ItemDragHelper;
import ecdar.utility.helpers.SelectHelper;
import ecdar.utility.keyboard.Keybind;
import ecdar.utility.keyboard.KeyboardTracker;
import ecdar.utility.keyboard.NudgeDirection;
import ecdar.utility.keyboard.Nudgeable;
import com.jfoenix.controls.JFXPopup;
import javafx.beans.property.*;
import javafx.beans.value.ObservableDoubleValue;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

public class LocationController implements Initializable, SelectHelper.ItemSelectable, Nudgeable {

    private static final Map<Location, Boolean> invalidNameError = new HashMap<>();

    private final ObjectProperty<Location> location = new SimpleObjectProperty<>();
    private final ObjectProperty<Component> component = new SimpleObjectProperty<>();
    public LocationPresentation root;

    public Path notCommittedShape;
    public Rectangle committedShape;

    public Path notCommittedInitialIndicator;
    public Rectangle committedInitialIndicator;

    public Group shakeContent;
    public Circle reachabilityStatus;
    public Circle circle;
    public Circle circleShakeIndicator;
    public Group scaleContent;
    public TagPresentation nicknameTag;
    public TagPresentation invariantTag;

    public Label idLabel;
    public Line nameTagLine;
    public Line invariantTagLine;
    public Line prohibitedLocStrikeThrough;

    private DropDownMenu dropDownMenu;
    private boolean dropDownMenuInitialized = false;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        this.location.addListener((obsLocation, oldLocation, newLocation) -> {
            // The radius property on the abstraction must reflect the radius in the view
            newLocation.radiusProperty().bind(circle.radiusProperty());

            // The scale property on the abstraction must reflect the radius in the view
            newLocation.scaleProperty().bind(scaleContent.scaleXProperty());
        });

        // Scale x and y 1:1 (based on the x-scale)
        scaleContent.scaleYProperty().bind(scaleContent.scaleXProperty());

        initializeSelectListener();
        initializeMouseControls();
    }

    private void initializeSelectListener() {
        SelectHelper.elementsToBeSelected.addListener(new ListChangeListener<Nearable>() {
            @Override
            public void onChanged(final Change<? extends Nearable> c) {
                while (c.next()) {
                    if (c.getAddedSize() == 0) return;

                    for (final Nearable nearable : SelectHelper.elementsToBeSelected) {
                        if (nearable instanceof Location) {
                            if (nearable.equals(getLocation())) {
                                SelectHelper.addToSelection(LocationController.this);
                                break;
                            }
                        }
                    }
                }
            }
        });
    }

    public void initializeDropDownMenu() {
        dropDownMenu = new DropDownMenu(root);

        dropDownMenu.addClickableAndDisableableListElement("Draw Edge", getLocation().getIsLocked(),
                (event) -> {
                        final Edge newEdge = new Edge(getLocation(), EcdarController.getGlobalEdgeStatus());

                        KeyboardTracker.registerKeybind(KeyboardTracker.ABANDON_EDGE, new Keybind(new KeyCodeCombination(KeyCode.ESCAPE), () -> {
                            getComponent().removeEdge(newEdge);
                        }));
                        getComponent().addEdge(newEdge);
                        dropDownMenu.hide();
                    }
                );

        dropDownMenu.addClickableAndDisableableListElement("Add Nickname",
                getLocation().nicknameProperty().isNotEmpty().or(nicknameTag.textFieldFocusProperty()),
                event -> {
                    nicknameTag.setOpacity(1);
                    nicknameTag.requestTextFieldFocus();
                    nicknameTag.requestTextFieldFocus(); // Requesting it twice is needed for some reason
                    dropDownMenu.hide();
                }
        );

        if (invariantTag.getOpacity() == 0) {
            dropDownMenu.addClickableAndDisableableListElement("Add Invariant",
                    getLocation().invariantProperty().isNotEmpty().or(invariantTag.textFieldFocusProperty()).or(getLocation().getIsLocked()),
                    event -> {
                        invariantTag.setOpacity(1);
                        invariantTag.requestTextFieldFocus();
                        invariantTag.requestTextFieldFocus(); // Requesting it twice is needed for some reason
                        dropDownMenu.hide();
                    }
            );
        } else {
            dropDownMenu.addClickableAndDisableableListElement("Remove Invariant",
                    getLocation().invariantProperty().isEmpty().or(getLocation().getIsLocked()),
                    event -> {
                        invariantTag.clear();
                        invariantTag.setOpacity(0);
                        dropDownMenu.hide();
                    }
            );
        }

        // For when non-initial
        dropDownMenu.addClickableAndDisableableListElement("Make Initial",
                getLocation().typeProperty().isEqualTo(Location.Type.INITIAL), // disable if already initial
                event -> {
                    final Location previousInitLoc = getComponent().getInitialLocation();

                    UndoRedoStack.pushAndPerform(() -> { // Perform
                        getComponent().setInitialLocation(getLocation());
                    }, () -> { // Undo
                        getComponent().setInitialLocation(previousInitLoc);
                    }, String.format("Made %s initial", location), "initial");
                    dropDownMenu.hide();
                }
        );

        dropDownMenu.addSpacerElement();
        final BooleanProperty isUrgent = new SimpleBooleanProperty(false);
        isUrgent.bind(getLocation().urgencyProperty().isEqualTo(Location.Urgency.URGENT));
        dropDownMenu.addToggleableAndDisableableListElement("Urgent", isUrgent, getLocation().getIsLocked(), event -> {
            if (isUrgent.get()) {
                getLocation().setUrgency(Location.Urgency.NORMAL);
            } else {
                getLocation().setUrgency(Location.Urgency.URGENT);
            }

            dropDownMenu.hide();
        });

        final BooleanProperty isProhibited = new SimpleBooleanProperty(false);
        isProhibited.bind(getLocation().urgencyProperty().isEqualTo(Location.Urgency.PROHIBITED));
        dropDownMenu.addToggleableAndDisableableListElement("Prohibited", isProhibited, getLocation().getIsLocked(), event -> {
            if (isProhibited.get()) {
                getLocation().setUrgency(Location.Urgency.NORMAL);
            } else {
                getLocation().setUrgency(Location.Urgency.PROHIBITED);
            }

            dropDownMenu.hide();
        });

        dropDownMenu.addSpacerElement();

        dropDownMenu.addClickableListElement("Is " + getLocation().getId() + " reachable?", event -> {
            dropDownMenu.hide();
            // Generate the query from the backend
            final String reachabilityQuery = BackendHelper.getLocationReachableQuery(getLocation(), getComponent());

            // Add proper comment
            final String reachabilityComment = "Is " + getLocation().getMostDescriptiveIdentifier() + " reachable?";

            // Add new query for this location
            final Query query = new Query(reachabilityQuery, reachabilityComment, QueryState.UNKNOWN);
            query.setType(QueryType.REACHABILITY);
            Ecdar.getProject().getQueries().add(query);
            query.run();
            dropDownMenu.hide();
        });

        dropDownMenu.addSpacerElement();

        dropDownMenu.addColorPicker(getLocation(), (color, intensity) -> {
            getLocation().setColorIntensity(intensity);
            getLocation().setColor(color);
        });

        dropDownMenu.addSpacerElement();

        dropDownMenu.addClickableListElement("Delete", event -> {
            tryDelete();
            dropDownMenu.hide();
        });
    }

    /**
     * Deletes this location.
     * You are not allowed to delete the initial location.
     * If this is an initial location, shake and give an error message instead.
     */
    public void tryDelete() {
        if (getLocation().getType() == Location.Type.INITIAL) {
            // You are not allowed to delete an initial location
            root.shake();
            Ecdar.showToast("You cannot delete the initial location.");
        } else {
            final Component component = getComponent();
            final Location location = getLocation();

            final List<DisplayableEdge> relatedEdges = component.getRelatedEdges(location);

            UndoRedoStack.pushAndPerform(() -> { // Perform
                // Remove the location
                component.getLocations().remove(location);
                relatedEdges.forEach(component::removeEdge);
            }, () -> { // Undo
                // Re-all the location
                component.getLocations().add(location);
                relatedEdges.forEach(component::addEdge);
            }, String.format("Deleted %s", location), "delete");
        }
    }

    public void initializeInvalidNameError() {
        final Location location = getLocation();
        if (invalidNameError.containsKey(location)) return;
        invalidNameError.put(location, true);

        final CodeAnalysis.Message invalidNickName = new CodeAnalysis.Message("Nicknames for locations must be alpha-numeric", CodeAnalysis.MessageType.ERROR, location);

        final Consumer<String> updateNickNameCheck = (nickname) -> {
            if (!nickname.matches("[A-Za-z0-9_-]*$")) {
                // Invalidate the list (will update the UI with the new name)
                invalidNickName.getNearables().remove(location);
                invalidNickName.getNearables().add(location);
                CodeAnalysis.addMessage(getComponent(), invalidNickName);
            } else {
                CodeAnalysis.removeMessage(getComponent(), invalidNickName);
            }
        };

        location.nicknameProperty().addListener((obs, oldNickName, newNickName) -> {
            updateNickNameCheck.accept(newNickName);
        });
        updateNickNameCheck.accept(location.getNickname());
    }

    public Location getLocation() {
        return location.get();
    }

    public void setLocation(final Location location) {
        this.location.set(location);

        if (ComponentController.isPlacingLocation()) {
            root.layoutXProperty().bindBidirectional(location.xProperty());
            root.layoutYProperty().bindBidirectional(location.yProperty());
        } else {
            root.setLayoutX(location.getX());
            root.setLayoutY(location.getY());
            location.xProperty().bindBidirectional(root.layoutXProperty());
            location.yProperty().bindBidirectional(root.layoutYProperty());
            root.setPlaced(true);
        }
    }

    public ObjectProperty<Location> locationProperty() {
        return location;
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

    @FXML
    private void locationEntered() {
        root.animateLocationEntered();
    }

    @FXML
    private void locationExited() {
        root.animateLocationExited();
    }

    @FXML
    private void mouseEntered() {
        if(!this.root.isInteractable()) return;
        circle.setCursor(Cursor.HAND);
        this.root.animateHoverEntered();

        // Keybind for making location urgent
        KeyboardTracker.registerKeybind(KeyboardTracker.MAKE_LOCATION_URGENT, new Keybind(new KeyCodeCombination(KeyCode.U), () -> {
            final Location.Urgency previousUrgency = location.get().getUrgency();

            if (previousUrgency.equals(Location.Urgency.URGENT)) {
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    getLocation().setUrgency(Location.Urgency.NORMAL);
                }, () -> { // Undo
                    getLocation().setUrgency(previousUrgency);
                }, "Made location " + getLocation().getNickname() + " urgent", "hourglass-full");
            } else {
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    getLocation().setUrgency(Location.Urgency.URGENT);
                }, () -> { // Undo
                    getLocation().setUrgency(previousUrgency);
                }, "Made location " + getLocation().getNickname() + " normal (back form urgent)", "hourglass-empty");
            }
        }));
    }

    @FXML
    private void mouseExited() {
        final LocationPresentation locationPresentation = this.root;
        if(!locationPresentation.isInteractable()) return;

        circle.setCursor(Cursor.DEFAULT);
        locationPresentation.animateHoverExited();
        KeyboardTracker.unregisterKeybind(KeyboardTracker.MAKE_LOCATION_URGENT);
    }

    private void initializeMouseControls() {
        final Consumer<MouseEvent> mouseClicked = (event) -> {
            // Make sure that we are not targeting one of the tags
            if (!(event.getTarget().equals(notCommittedShape) || event.getTarget().equals(committedShape))) return;
            event.consume();

            final Component component = getComponent();

            if (component.isAnyEdgeWithoutSource()) {
                final DisplayableEdge unfinishedEdge = component.getUnfinishedEdge();
                // Make self-loop pretty if needed
                if (unfinishedEdge.getTargetLocation().equals(getLocation()) && unfinishedEdge.getNails().size() == 1) {
                    final Nail nail = new Nail(unfinishedEdge.getNails().get(0).getX(), unfinishedEdge.getNails().get(0).getY() + Ecdar.CANVAS_PADDING * 2);
                    unfinishedEdge.addNail(nail);
                }
                unfinishedEdge.setSourceLocation(getLocation());

                final Location prevSourceLocation = component.previousLocationForDraggedEdge;
                UndoRedoStack.push(() -> { // Perform
                    unfinishedEdge.setSourceLocation(getLocation());
                }, () -> { // Undo
                    unfinishedEdge.setSourceLocation(prevSourceLocation);
                }, "Dragged edge source to be location " + getLocation().getNickname(), "add-circle");
                component.previousLocationForDraggedEdge = null;
                return;
            }

            if (root.isPlaced()) {
                final DisplayableEdge unfinishedEdge = component.getUnfinishedEdge();

                if (unfinishedEdge == null && event.getButton().equals(MouseButton.SECONDARY)) {
                    initializeDropDownMenu();
                    dropDownMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 0, 0);
                } else if (unfinishedEdge != null) {
                    unfinishedEdge.setTargetLocation(getLocation());

                    // If edge has no sync, add one
                    if (!unfinishedEdge.hasSyncNail()) {
                        // If self loop, make it pretty
                        if (getLocation().equals(unfinishedEdge.getSourceLocation())) {
                            final Nail nail = new Nail(getX() + Ecdar.CANVAS_PADDING * 4, getY() - Ecdar.CANVAS_PADDING);
                            nail.setPropertyType(Edge.PropertyType.SYNCHRONIZATION);
                            unfinishedEdge.addNail(nail);
                            final Nail nail2 = new Nail(getX() + Ecdar.CANVAS_PADDING * 4, getY() + Ecdar.CANVAS_PADDING);
                            unfinishedEdge.addNail(nail2);
                        } else {
                            unfinishedEdge.makeSyncNailBetweenLocations();
                        }
                    } else if (getLocation().equals(unfinishedEdge.getSourceLocation()) && unfinishedEdge.getNails().size() == 1) {
                        final Nail nail = new Nail(unfinishedEdge.getNails().get(0).getX(), unfinishedEdge.getNails().get(0).getY() + Ecdar.CANVAS_PADDING * 2);
                        unfinishedEdge.addNail(nail);
                    }

                    final Location previousTarget = component.previousLocationForDraggedEdge;
                    UndoRedoStack.push(() -> { // Perform
                        unfinishedEdge.setTargetLocation(getLocation());
                    }, () -> { // Undo
                        unfinishedEdge.setTargetLocation(previousTarget);
                    }, "Created edge starting from location " + getLocation().getNickname(), "add-circle");
                    component.previousLocationForDraggedEdge = null;
                } else {
                    // If shift is being held down, start drawing a new edge
                    if (canCreateEdgeShortcut(event)) {
                        final Edge newEdge = new Edge(getLocation(), EcdarController.getGlobalEdgeStatus());
                        KeyboardTracker.registerKeybind(KeyboardTracker.ABANDON_EDGE, new Keybind(new KeyCodeCombination(KeyCode.ESCAPE), () -> {
                            component.removeEdge(newEdge);
                        }));

                        component.addEdge(newEdge);
                    }
                    // Otherwise, select the location
                    else {
                        if(root.isInteractable()) {
                            if (event.isShortcutDown()) {
                                if (SelectHelper.getSelectedElements().contains(this)) {
                                    SelectHelper.deselect(this);
                                } else {
                                    SelectHelper.addToSelection(this);
                                }
                            } else {
                                SelectHelper.select(this);
                            }
                        }
                    }
                }
            } else {
                // Allowed x and y coordinates
                final double minX = Ecdar.CANVAS_PADDING * 2;
                final double maxX = getComponent().getBox().getWidth() - Ecdar.CANVAS_PADDING * 2;
                final double minY = Ecdar.CANVAS_PADDING * 4;
                final double maxY = getComponent().getBox().getHeight() - Ecdar.CANVAS_PADDING * 2;

                if(root.getLayoutX() >= minX && root.getLayoutX() <= maxX && root.getLayoutY() >= minY && root.getLayoutY() <= maxY) {
                    // Unbind presentation root x and y coordinates (bind the view properly to enable dragging)
                    root.layoutXProperty().unbind();
                    root.layoutYProperty().unbind();

                    // Bind the location to the presentation root x and y
                    getLocation().xProperty().bindBidirectional(root.layoutXProperty());
                    getLocation().yProperty().bindBidirectional(root.layoutYProperty());

                    // Notify that the location was placed
                    root.setPlaced(true);
                    ComponentController.setPlacingLocation(null);
                    KeyboardTracker.unregisterKeybind(KeyboardTracker.ABANDON_LOCATION);
                } else {
                    root.shake();
                }
            }
        };

        locationProperty().addListener((obs, oldLocation, newLocation) -> {
            if(newLocation == null) return;
            root.addEventHandler(MouseEvent.MOUSE_CLICKED, mouseClicked::accept);
            ItemDragHelper.makeDraggable(root, this::getDragBounds);
        });
    }

    @Override
    public void color(final Color color, final Color.Intensity intensity) {
        final Location location = getLocation();

        // Set the color of the location
        location.setColorIntensity(intensity);
        location.setColor(color);
    }

    @Override
    public Color getColor() {
        return getLocation().getColor();
    }

    @Override
    public Color.Intensity getColorIntensity() {
        return getLocation().getColorIntensity();
    }

    @Override
    public ItemDragHelper.DragBounds getDragBounds() {
        final int PADDING = 5;
        final ObservableDoubleValue minX = new SimpleDoubleProperty(location.get().getRadius() + PADDING);
        final ObservableDoubleValue maxX = getComponent().getBox().getWidthProperty().subtract(location.get().getRadius() + PADDING);
        final ObservableDoubleValue minY = new SimpleDoubleProperty(location.get().getRadius() + PADDING);
        final ObservableDoubleValue maxY = getComponent().getBox().getHeightProperty().subtract(location.get().getRadius() + PADDING);
        return new ItemDragHelper.DragBounds(minX, maxX, minY, maxY);
    }

    @Override
    public void select() {
        root.select();
    }

    @Override
    public void deselect() {
        root.deselect();
    }

    @Override
    public boolean nudge(final NudgeDirection direction) {
        final double oldX = root.getLayoutX();
        final double newX = getDragBounds().trimX(root.getLayoutX() + direction.getXOffset());
        root.layoutXProperty().set(newX);

        final double oldY = root.getLayoutY();
        final double newY = getDragBounds().trimY(root.getLayoutY() + direction.getYOffset());
        root.layoutYProperty().set(newY);

        return oldX != newX || oldY != newY;
    }

    /**'
     * Method which has the logical expression for the shortcut of adding a new edge,
     * checks whether the location is locked and the correct buttons are held down
     * @param event the mouse event
     * @return the result of the boolean expression, false if the location is locked or the incorrect buttons are held down,
     * true if the correct buttons are down
     */
    private boolean canCreateEdgeShortcut(MouseEvent event){
        return (!getLocation().getIsLocked().get() &&
                ((event.isShiftDown() && event.getButton().equals(MouseButton.PRIMARY)) || event.getButton().equals(MouseButton.MIDDLE)));
    }

    @Override
    public DoubleProperty xProperty() {
        return root.layoutXProperty();
    }

    @Override
    public DoubleProperty yProperty() {
        return root.layoutYProperty();
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
        return getLocation().getRadius() * 2;
    }

    @Override
    public double getSelectableHeight() {
        return getLocation().getRadius() * 2;
    }
}
