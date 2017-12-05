package SW9.controllers;

import SW9.Ecdar;
import SW9.abstractions.*;
import SW9.backend.UPPAALDriver;
import SW9.code_analysis.CodeAnalysis;
import SW9.presentations.*;
import SW9.utility.UndoRedoStack;
import SW9.utility.helpers.BindingHelper;
import SW9.utility.helpers.Circular;
import SW9.utility.helpers.LocationAware;
import SW9.utility.helpers.SelectHelper;
import SW9.utility.mouse.MouseTracker;
import com.jfoenix.controls.JFXPopup;
import com.jfoenix.controls.JFXRippler;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.*;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static SW9.presentations.Grid.GRID_SIZE;

public class ComponentController extends ModelController implements Initializable {
    private static final Map<Component, ListChangeListener<Location>> locationListChangeListenerMap = new HashMap<>();
    private static final Map<Component, Boolean> errorsAndWarningsInitialized = new HashMap<>();
    private static Location placingLocation = null;
    private final ObjectProperty<Component> component = new SimpleObjectProperty<>(null);
    private final Map<Edge, EdgePresentation> edgePresentationMap = new HashMap<>();
    private final Map<Location, LocationPresentation> locationPresentationMap = new HashMap<>();

    public StyleClassedTextArea declarationTextArea;
    public JFXRippler toggleDeclarationButton;
    public Label x;
    public Label y;
    public Pane modelContainerLocation;
    public Pane modelContainerEdge;

    public VBox outputSignatureContainer;
    public VBox inputSignatureContainer;

    private MouseTracker mouseTracker;
    private DropDownMenu contextMenu;
    private DropDownMenu finishEdgeContextMenu;
    private Circle dropDownMenuHelperCircle;

    public static boolean isPlacingLocation() {
        return placingLocation != null;
    }

    public static void setPlacingLocation(final Location placingLocation) {
        ComponentController.placingLocation = placingLocation;
    }

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        declarationTextArea.setParagraphGraphicFactory(LineNumberFactory.get(declarationTextArea));

        component.addListener((obs, oldComponent, newComponent) -> {
            inputSignatureContainer.heightProperty().addListener((change) -> updateMaxHeight() );
            outputSignatureContainer.heightProperty().addListener((change) -> updateMaxHeight() );

            // Bind the declarations of the abstraction the the view
            declarationTextArea.replaceText(0, declarationTextArea.getLength(), newComponent.getDeclarationsText());
            declarationTextArea.textProperty().addListener((observable, oldDeclaration, newDeclaration) -> newComponent.setDeclarationsText(newDeclaration));

            // Find the clocks in the decls
            newComponent.declarationsTextProperty().addListener((observable, oldValue, newValue) -> {
                final List<String> clocks = new ArrayList<String>();

                final String strippedDecls = newValue.replaceAll("[\\r\\n]+", "");

                Pattern pattern = Pattern.compile("clock (?<CLOCKS>[^;]*);");
                Matcher matcher = pattern.matcher(strippedDecls);

                while (matcher.find()) {
                    final String clockStrings[] = matcher.group("CLOCKS").split(",");
                    for (String clockString : clockStrings) {
                        clocks.add(clockString.replaceAll("\\s",""));
                    }
                }

                //TODO this logs the clocks System.out.println(clocks);
            });

            initializeEdgeHandling(newComponent);
            initializeLocationHandling(newComponent);
            initializeDeclarations();
            initializeSignature(newComponent);
            initializeSignatureListeners(newComponent);
        });


        // The root view have been inflated, initialize the mouse tracker on it
        mouseTracker = new MouseTracker(root);

        initializeContextMenu();

        component.addListener((obs, old, component) -> {
            if (component == null) return;

            if (!errorsAndWarningsInitialized.containsKey(component) || !errorsAndWarningsInitialized.get(component)) {
                initializeNoIncomingEdgesWarning();
                errorsAndWarningsInitialized.put(component, true);
            }
        });
    }

    /***
     * Inserts the initial edges of the component to the input/output signature
     * @param newComponent The component that should be presented with its signature
     */
    private void initializeSignature(final Component newComponent) {
        newComponent.getOutputStrings().forEach((channel) -> insertSignatureArrow(channel, EdgeStatus.OUTPUT));
        newComponent.getInputStrings().forEach((channel) -> insertSignatureArrow(channel, EdgeStatus.INPUT));
    }

    /***
     * Initialize the listeners, that listen for changes in the input and output edges of the presented component.
     * The view is updated whenever an insert (deletions are also a type of insert) is reported
     * @param newComponent The component that should be presented with its signature
     */
    private void initializeSignatureListeners(final Component newComponent) {
        newComponent.getOutputStrings().addListener((ListChangeListener<String>) c -> {
            // By clearing the container we don't have to fiddle with which elements are removed and added
            outputSignatureContainer.getChildren().clear();
            while (c.next()){
                c.getAddedSubList().forEach((channel) -> insertSignatureArrow(channel, EdgeStatus.OUTPUT));
            }
        });

        newComponent.getInputStrings().addListener((ListChangeListener<String>) c -> {
            inputSignatureContainer.getChildren().clear();
            while (c.next()){
                c.getAddedSubList().forEach((channel) -> insertSignatureArrow(channel, EdgeStatus.INPUT));
            }
        });
    }

    /***
     * Inserts a new {@link SW9.presentations.SignatureArrow} in the containers for either input or output signature
     * @param channel A String with the channel name that should be shown with the arrow
     * @param status An EdgeStatus for the type of arrow to insert
     */
    private void insertSignatureArrow(final String channel, final EdgeStatus status) {
        SignatureArrow newArrow = new SignatureArrow(channel, status, component.get());
        if(status == EdgeStatus.INPUT) {
            inputSignatureContainer.getChildren().add(newArrow);
        } else {
            outputSignatureContainer.getChildren().add(newArrow);
        }
    }


    /***
     * Updates the component's height to match the input/output signature containers
     * if the component is smaller than either of them
     */
    private void updateMaxHeight() {
        // If input/outputsignature container is taller than the current component height
        // we update the component's height to be as tall as the container
        double maxHeight = findMaxHeight();
        if(maxHeight > component.get().getBox().getHeight()) {
            component.get().getBox().getHeightProperty().set(maxHeight);
        }
    }

    /***
     * Finds the max height of the input/output signature container and the component
     * @return a double of the largest height
     */

    private double findMaxHeight() {
        double inputHeight = inputSignatureContainer.getHeight();
        double outputHeight = outputSignatureContainer.getHeight();
        double componentHeight = component.get().getBox().getHeight();

        double maxSignatureHeight = Math.max(outputHeight, inputHeight);

        return Math.max(maxSignatureHeight, componentHeight);
    }

    private void initializeNoIncomingEdgesWarning() {
        final Map<Location, CodeAnalysis.Message> messages = new HashMap<>();

        final Function<Location, Boolean> hasIncomingEdges = location -> {
            if (!getComponent().getAllButInitialLocations().contains(location))
                return true; // Do now show messages for locations not in the set of locations

            for (final Edge edge : getComponent().getEdges()) {
                final Location targetLocation = edge.getTargetLocation();
                if (targetLocation != null && targetLocation.equals(location)) return true;
            }

            return false;
        };

        final Consumer<Component> checkLocations = (component) -> {
            final List<Location> ignored = new ArrayList<>();

            // Run through all of the locations we are currently displaying a warning for, checking if we should remove them
            final Set<Location> removeMessages = new HashSet<>();
            messages.keySet().forEach(location -> {
                // Check if the location has some incoming edges
                final boolean result = hasIncomingEdges.apply(location);

                // The location has at least one incoming edge
                if (result) {
                    CodeAnalysis.removeMessage(component, messages.get(location));
                    removeMessages.add(location);
                }

                // Ignore this location from now on (we already checked it)
                ignored.add(location);
            });
            removeMessages.forEach(messages::remove);

            // Run through all non-ignored locations
            for (final Location location : component.getAllButInitialLocations()) {
                if (ignored.contains(location)) continue; // Skip ignored
                if (messages.containsKey(location)) continue; // Skip locations that already have warnings associated

                // Check if the location has some incoming edges
                final boolean result = hasIncomingEdges.apply(location);

                // The location has no incoming edge
                if (!result) {
                    final CodeAnalysis.Message message = new CodeAnalysis.Message("Location has no incoming edges", CodeAnalysis.MessageType.WARNING, location);
                    messages.put(location, message);
                    CodeAnalysis.addMessage(component, message);
                }
            }
        };

        final Component component = getComponent();
        checkLocations.accept(component);

        // Check location whenever we get new edges
        component.getEdges().addListener(new ListChangeListener<Edge>() {
            @Override
            public void onChanged(final Change<? extends Edge> c) {
                while (c.next()) {
                    checkLocations.accept(component);
                }
            }
        });

        // Check location whenever we get new locations
        component.getLocations().addListener(new ListChangeListener<Location>() {
            @Override
            public void onChanged(final Change<? extends Location> c) {
                while (c.next()) {
                    checkLocations.accept(component);
                }
            }
        });
    }

    private void initializeContextMenu() {
        dropDownMenuHelperCircle = new Circle(5);
        dropDownMenuHelperCircle.setOpacity(0);
        dropDownMenuHelperCircle.setMouseTransparent(true);

        root.getChildren().add(dropDownMenuHelperCircle);

        final Consumer<Component> initializeDropDownMenu = (component) -> {
            if (component == null) {
                return;
            }

            contextMenu = new DropDownMenu(dropDownMenuHelperCircle);

            contextMenu.addClickableListElement("Add Location", event -> {

                final Location newLocation = new Location();

                double x = DropDownMenu.x - LocationPresentation.RADIUS / 2;
                x = Grid.snap(x);
                newLocation.setX(x);

                double y = DropDownMenu.y - LocationPresentation.RADIUS / 2;
                y = Grid.snap(y);
                newLocation.setY(y);

                newLocation.setColorIntensity(component.getColorIntensity());
                newLocation.setColor(component.getColor());

                // Add a new location
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    component.addLocation(newLocation);
                }, () -> { // Undo
                    component.removeLocation(newLocation);
                }, "Added location '" + newLocation.toString() + "' to component '" + component.getName() + "'", "add-circle");
            });

            // Adds the add universal location element to the drop down menu, this element adds an universal location and its required edges
            contextMenu.addClickableListElement("Add Universal Location", event -> {

                double x = DropDownMenu.x - LocationPresentation.RADIUS / 2;
                double y = DropDownMenu.y - LocationPresentation.RADIUS / 2;

                final Location newLocation = new Location(component, Location.Type.UNIVERSAL, x, y);

                final Edge inputEdge = newLocation.addLeftEdge("*", EdgeStatus.INPUT);
                inputEdge.setIsLocked(true);

                final Edge outputEdge = newLocation.addRightEdge("*", EdgeStatus.OUTPUT);
                outputEdge.setIsLocked(true);

                // Add a new location
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    component.addLocation(newLocation);
                    component.addEdge(inputEdge);
                    component.addEdge(outputEdge);
                }, () -> { // Undo
                    component.removeLocation(newLocation);
                    component.removeEdge(inputEdge);
                    component.removeEdge(outputEdge);
                }, "Added universal location '" + newLocation.toString() + "' to component '" + component.getName() + "'", "add-circle");
            });

            // Adds the add inconsistent location element to the drop down menu, this element adds an inconsistent location
            contextMenu.addClickableListElement("Add Inconsistent Location", event -> {

                double x = DropDownMenu.x - LocationPresentation.RADIUS / 2;
                double y = DropDownMenu.y - LocationPresentation.RADIUS / 2;

                final Location newLocation = new Location(component, Location.Type.INCONSISTENT, x, y);

                // Add a new location
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    component.addLocation(newLocation);
                }, () -> { // Undo
                    component.removeLocation(newLocation);
                }, "Added inconsistent location '" + newLocation.toString() + "' to component '" + component.getName() + "'", "add-circle");
            });

            contextMenu.addSpacerElement();

            contextMenu.addClickableListElement("Contains deadlock?", event -> {

                // Generate the query
                final String deadlockQuery = UPPAALDriver.getExistDeadlockQuery(getComponent());

                // Add proper comment
                final String deadlockComment = "Does " + component.getName() + " contain a deadlock?";

                // Add new query for this component
                final Query query = new Query(deadlockQuery, deadlockComment, QueryState.UNKNOWN);
                Ecdar.getProject().getQueries().add(query);
                query.run();

            });

            contextMenu.addSpacerElement();
            contextMenu.addColorPicker(component, component::dye);
        };

        component.addListener((obs, oldComponent, newComponent) -> {
            initializeDropDownMenu.accept(newComponent);
        });

        Ecdar.getProject().getComponents().addListener(new ListChangeListener<Component>() {
            @Override
            public void onChanged(final Change<? extends Component> c) {
                initializeDropDownMenu.accept(getComponent());
            }
        });
        initializeDropDownMenu.accept(getComponent());
    }

    private void initializeFinishEdgeContextMenu(final Edge unfinishedEdge) {

        final Consumer<Component> initializeDropDownMenu = (component) -> {
            if (component == null) {
                return;
            }

            final Consumer<LocationAware> setCoordinates = (locationAware) -> {
                double x = DropDownMenu.x;
                x = Math.round(x / GRID_SIZE) * GRID_SIZE;

                double y = DropDownMenu.y;
                y = Math.round(y / GRID_SIZE) * GRID_SIZE;

                locationAware.xProperty().set(x);
                locationAware.yProperty().set(y);
            };

            finishEdgeContextMenu = new DropDownMenu(dropDownMenuHelperCircle);
            finishEdgeContextMenu.addListElement("Finish edge in a:");

            finishEdgeContextMenu.addClickableListElement("Location", event -> {
                final Location location = new Location();

                location.setColorIntensity(getComponent().getColorIntensity());
                location.setColor(getComponent().getColor());

                unfinishedEdge.setTargetLocation(location);

                setCoordinates.accept(location);

                // If edge has no sync, add one
                if (!unfinishedEdge.hasSyncNail()) unfinishedEdge.makeSyncNailBetweenLocations();

                getComponent().addLocation(location);

                // Add a new location
                UndoRedoStack.push(() -> { // Perform
                    getComponent().addLocation(location);
                    getComponent().addEdge(unfinishedEdge);
                }, () -> { // Undo
                    getComponent().removeLocation(location);
                    getComponent().removeEdge(unfinishedEdge);
                }, "Finished edge '" + unfinishedEdge + "' by adding '" + location + "' to component '" + component.getName() + "'", "add-circle");
            });


            finishEdgeContextMenu.addClickableListElement("Universal Location", event -> {

                double x = DropDownMenu.x - LocationPresentation.RADIUS / 2;
                double y = DropDownMenu.y - LocationPresentation.RADIUS / 2;

                final Location newLocation = new Location(component, Location.Type.UNIVERSAL, x, y);

                final Edge inputEdge = newLocation.addLeftEdge("*", EdgeStatus.INPUT);
                inputEdge.setIsLocked(true);

                final Edge outputEdge = newLocation.addRightEdge("*", EdgeStatus.OUTPUT);
                outputEdge.setIsLocked(true);

                unfinishedEdge.setTargetLocation(newLocation);

                setCoordinates.accept(newLocation);

                // If edge has no sync, add one
                if (!unfinishedEdge.hasSyncNail()) unfinishedEdge.makeSyncNailBetweenLocations();

                getComponent().addLocation(newLocation);
                getComponent().addEdge(inputEdge);
                getComponent().addEdge(outputEdge);

                // Add a new location
                UndoRedoStack.push(() -> { // Perform
                    getComponent().addLocation(newLocation);
                    getComponent().addEdge(inputEdge);
                    getComponent().addEdge(outputEdge);
                    getComponent().addEdge(unfinishedEdge);
                }, () -> { // Undo
                    getComponent().removeLocation(newLocation);
                    getComponent().removeEdge(inputEdge);
                    getComponent().removeEdge(outputEdge);
                    getComponent().removeEdge(unfinishedEdge);
                }, "Finished edge '" + unfinishedEdge + "' by adding '" + newLocation + "' to component '" + component.getName() + "'", "add-circle");
            });

            finishEdgeContextMenu.addClickableListElement("Inconsistent Location", event -> {
                double x = DropDownMenu.x - LocationPresentation.RADIUS / 2;
                double y = DropDownMenu.y - LocationPresentation.RADIUS / 2;

                final Location newLocation = new Location(component, Location.Type.INCONSISTENT, x, y);

                unfinishedEdge.setTargetLocation(newLocation);

                setCoordinates.accept(newLocation);

                // If edge has no sync, add one
                if (!unfinishedEdge.hasSyncNail()) unfinishedEdge.makeSyncNailBetweenLocations();

                getComponent().addLocation(newLocation);

                UndoRedoStack.push(() -> { // Redo
                    getComponent().addLocation(newLocation);
                    getComponent().addEdge(unfinishedEdge);
                }, () -> { // Undo
                    getComponent().removeLocation(newLocation);
                    getComponent().removeEdge(unfinishedEdge);
                }, "Finished edge '" + unfinishedEdge + "' by adding '" + newLocation + "' to component '" + component.getName() + "'", "add-circle");
            });

        };

        component.addListener((obs, oldComponent, newComponent) -> {
            initializeDropDownMenu.accept(newComponent);
        });

        initializeDropDownMenu.accept(getComponent());
    }

    private void initializeLocationHandling(final Component newComponent) {
        final Consumer<Location> handleAddedLocation = (loc) -> {
            // Create a new presentation, and register it on the map
            final LocationPresentation newLocationPresentation = new LocationPresentation(loc, newComponent);
            locationPresentationMap.put(loc, newLocationPresentation);

            // Add it to the view
            modelContainerLocation.getChildren().add(newLocationPresentation);

            // Bind the newly created location to the mouse and tell the ui that it is not placed yet
            if (loc.getX() == 0) {
                newLocationPresentation.setPlaced(false);
                BindingHelper.bind(loc, newComponent.getBox().getXProperty(), newComponent.getBox().getYProperty());
            }
        };

        if(locationListChangeListenerMap.containsKey(newComponent)) {
            newComponent.getLocations().removeListener(locationListChangeListenerMap.get(newComponent));
        }
        final ListChangeListener<Location> locationListChangeListener = c -> {
            if (c.next()) {
                // Locations are added to the component
                c.getAddedSubList().forEach(handleAddedLocation::accept);

                // Locations are removed from the component
                c.getRemoved().forEach(location -> {
                    final LocationPresentation locationPresentation = locationPresentationMap.get(location);
                    modelContainerLocation.getChildren().remove(locationPresentation);
                    locationPresentationMap.remove(location);
                });
            }
        };
        newComponent.getLocations().addListener(locationListChangeListener);
        locationListChangeListenerMap.put(newComponent, locationListChangeListener);

        newComponent.getLocations().forEach(handleAddedLocation);
    }

    private void initializeEdgeHandling(final Component newComponent) {
        final Consumer<Edge> handleAddedEdge = edge -> {
            final EdgePresentation edgePresentation = new EdgePresentation(edge, newComponent);
            edgePresentationMap.put(edge, edgePresentation);
            modelContainerEdge.getChildren().add(edgePresentation);

            final Consumer<Circular> updateMouseTransparency = (newCircular) -> {
                if (newCircular == null) {
                    edgePresentation.setMouseTransparent(true);
                } else {
                    edgePresentation.setMouseTransparent(false);
                }
            };

            edge.targetCircularProperty().addListener((obs1, oldTarget, newTarget) -> updateMouseTransparency.accept(newTarget));
            updateMouseTransparency.accept(edge.getTargetCircular());
        };

        // React on addition of edges to the component
        newComponent.getEdges().addListener(new ListChangeListener<Edge>() {
            @Override
            public void onChanged(final Change<? extends Edge> c) {
                if (c.next()) {
                    // Edges are added to the component
                    c.getAddedSubList().forEach(handleAddedEdge::accept);

                    // Edges are removed from the component
                    c.getRemoved().forEach(edge -> {
                        final EdgePresentation edgePresentation = edgePresentationMap.get(edge);
                        modelContainerEdge.getChildren().remove(edgePresentation);
                        edgePresentationMap.remove(edge);
                    });
                }
            }
        });
        newComponent.getEdges().forEach(handleAddedEdge);
    }

    private void initializeDeclarations() {
        // Initially style the declarations
        declarationTextArea.setStyleSpans(0, ComponentPresentation.computeHighlighting(getComponent().getDeclarationsText()));

        final Circle circle = new Circle(0);
        if (getComponent().isDeclarationOpen()) {
            circle.setRadius(1000);
        }
        final ObjectProperty<Node> clip = new SimpleObjectProperty<>(circle);
        declarationTextArea.clipProperty().bind(clip);
        clip.set(circle);
    }

    public void toggleDeclaration(final MouseEvent mouseEvent) {
        final Circle circle = new Circle(0);
        circle.setCenterX(component.get().getBox().getWidth() - (toggleDeclarationButton.getWidth() - mouseEvent.getX()));
        circle.setCenterY(-1 * mouseEvent.getY());

        final ObjectProperty<Node> clip = new SimpleObjectProperty<>(circle);
        declarationTextArea.clipProperty().bind(clip);

        final Transition rippleEffect = new Transition() {
            private final double maxRadius = Math.sqrt(Math.pow(getComponent().getBox().getWidth(), 2) + Math.pow(getComponent().getBox().getHeight(), 2));
            {
                setCycleDuration(Duration.millis(500));
            }

            protected void interpolate(final double fraction) {
                if (getComponent().isDeclarationOpen()) {
                    circle.setRadius(fraction * maxRadius);
                } else {
                    circle.setRadius(maxRadius - fraction * maxRadius);
                }
                clip.set(circle);
            }
        };

        final Interpolator interpolator = Interpolator.SPLINE(0.785, 0.135, 0.15, 0.86);
        rippleEffect.setInterpolator(interpolator);

        rippleEffect.play();
        getComponent().declarationOpenProperty().set(!getComponent().isDeclarationOpen());
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
    private void modelContainerPressed(final MouseEvent event) {
        event.consume();
        CanvasController.leaveTextAreas();

        final Edge unfinishedEdge = getComponent().getUnfinishedEdge();

        if ((event.isShiftDown() && event.isPrimaryButtonDown()) || event.isMiddleButtonDown()) {
            final Location location = new Location();

            location.setX(Grid.snap(event.getX()));
            location.setY(Grid.snap(event.getY()));

            location.setColorIntensity(getComponent().getColorIntensity());
            location.setColor(getComponent().getColor());

            if (unfinishedEdge != null) {
                unfinishedEdge.setTargetLocation(location);

                // If no sync nail, add one
                if (!unfinishedEdge.hasSyncNail()) unfinishedEdge.makeSyncNailBetweenLocations();
            }

            getComponent().addLocation(location);

            // Add the new location
            UndoRedoStack.push(() -> { // Perform
                getComponent().addLocation(location);
                if (unfinishedEdge != null) {
                    getComponent().addEdge(unfinishedEdge);
                }
            }, () -> { // Undo
                getComponent().removeLocation(location);
                if (unfinishedEdge != null) {
                    getComponent().removeEdge(unfinishedEdge);
                }
            }, "Finished edge '" + unfinishedEdge + "' by adding '" + location + "' to component '" + component.getName() + "'", "add-circle");


        } else if (event.isSecondaryButtonDown()) {
            dropDownMenuHelperCircle.setLayoutX(event.getX());
            dropDownMenuHelperCircle.setLayoutY(event.getY());
            DropDownMenu.x = event.getX();
            DropDownMenu.y = event.getY();

            if (unfinishedEdge == null) {
                contextMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 0, 0);
            } else {
                initializeFinishEdgeContextMenu(unfinishedEdge);
                finishEdgeContextMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, 0, 0);
            }
        } else if(event.isPrimaryButtonDown()) {
            // We are drawing an edge
            if (unfinishedEdge != null) {
                // Calculate the position for the new nail (based on the component position and the canvas mouse tracker)
                final DoubleBinding x = CanvasPresentation.mouseTracker.gridXProperty().subtract(getComponent().getBox().getXProperty());
                final DoubleBinding y = CanvasPresentation.mouseTracker.gridYProperty().subtract(getComponent().getBox().getYProperty());

                // Create the abstraction for the new nail and add it to the unfinished edge
                final Nail newNail = new Nail(x, y);

                // Make sync nail if edge has none
                if (!unfinishedEdge.hasSyncNail()) {
                    newNail.setPropertyType(Edge.PropertyType.SYNCHRONIZATION);
                }

                unfinishedEdge.addNail(newNail);
            } else {
                SelectHelper.clearSelectedElements();
            }
        }
    }
    public MouseTracker getMouseTracker() {
        return mouseTracker;
    }

    @Override
    public HighLevelModelObject getModel() {
        return getComponent();
    }
}
