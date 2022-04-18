package ecdar.controllers;

import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.backend.BackendHelper;
import ecdar.code_analysis.CodeAnalysis;
import ecdar.presentations.*;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.helpers.*;
import ecdar.utility.mouse.MouseTracker;
import com.jfoenix.controls.JFXPopup;
import com.jfoenix.controls.JFXRippler;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
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

import static ecdar.presentations.Grid.GRID_SIZE;

public class ComponentController extends ModelController implements Initializable {
    private static final Map<Component, ListChangeListener<Location>> locationListChangeListenerMap = new HashMap<>();
    private static final Map<Component, Boolean> errorsAndWarningsInitialized = new HashMap<>();
    private static Location placingLocation = null;
    private final ObjectProperty<Component> component = new SimpleObjectProperty<>(null);
    private final Map<DisplayableEdge, EdgePresentation> edgePresentationMap = new HashMap<>();
    private final Map<Location, LocationPresentation> locationPresentationMap = new HashMap<>();

    public StyleClassedTextArea declarationTextArea;
    public JFXRippler toggleDeclarationButton;
    public Label x;
    public Label y;
    public Pane modelContainerSubComponent;
    public Pane modelContainerLocation;
    public Pane modelContainerEdge;

    public VBox outputSignatureContainer;
    public VBox inputSignatureContainer;

    private MouseTracker mouseTracker;
    private DropDownMenu contextMenu;
    private DropDownMenu finishEdgeContextMenu;

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
            inputSignatureContainer.heightProperty().addListener((change) -> updateMaxHeight());
            outputSignatureContainer.heightProperty().addListener((change) -> updateMaxHeight());

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
                        clocks.add(clockString.replaceAll("\\s", ""));
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
            while (c.next()) {
                c.getAddedSubList().forEach((channel) -> insertSignatureArrow(channel, EdgeStatus.OUTPUT));
            }
        });

        newComponent.getInputStrings().addListener((ListChangeListener<String>) c -> {
            inputSignatureContainer.getChildren().clear();
            while (c.next()) {
                c.getAddedSubList().forEach((channel) -> insertSignatureArrow(channel, EdgeStatus.INPUT));
            }
        });
    }

    /***
     * Inserts a new {@link ecdar.presentations.SignatureArrow} in the containers for either input or output signature
     * @param channel A String with the channel name that should be shown with the arrow
     * @param status An EdgeStatus for the type of arrow to insert
     */
    private void insertSignatureArrow(final String channel, final EdgeStatus status) {
        SignatureArrow newArrow = new SignatureArrow(channel, status, component.get());
        if (status == EdgeStatus.INPUT) {
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
        if (maxHeight > component.get().getBox().getHeight()) {
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

            // Run through all the locations we are currently displaying a warning for, checking if we should remove them
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
        component.getDisplayableEdges().addListener(new ListChangeListener<DisplayableEdge>() {
            @Override
            public void onChanged(final Change<? extends DisplayableEdge> c) {
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
        final Consumer<Component> initializeDropDownMenu = (component) -> {
            if (component == null) {
                return;
            }

            contextMenu = new DropDownMenu(root);

            contextMenu.addClickableListElement("Add Location", event -> {
                contextMenu.hide();
                final Location newLocation = new Location();
                newLocation.initialize();

                double x = DropDownMenu.x / EcdarController.getActiveCanvasPresentation().getController().zoomHelper.getZoomLevel() - LocationPresentation.RADIUS / 2;
                x = Grid.snap(x);
                newLocation.setX(x);

                double y = DropDownMenu.y / EcdarController.getActiveCanvasPresentation().getController().zoomHelper.getZoomLevel() - LocationPresentation.RADIUS / 2;
                y = Grid.snap(y);
                newLocation.setY(y);

                newLocation.setColorIntensity(component.getColorIntensity());
                newLocation.setColor(component.getColor());

                // Add a new location
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    component.addLocation(newLocation);
                }, () -> { // Undo
                    component.removeLocation(newLocation);
                }, "Added location '" + newLocation + "' to component '" + component.getName() + "'", "add-circle");
            });

            // Adds the add universal location element to the drop down menu, this element adds an universal location and its required edges
            contextMenu.addClickableListElement("Add Universal Location", event -> {
                contextMenu.hide();
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
                }, "Added universal location '" + newLocation + "' to component '" + component.getName() + "'", "add-circle");
            });

            // Adds the add inconsistent location element to the drop down menu, this element adds an inconsistent location
            contextMenu.addClickableListElement("Add Inconsistent Location", event -> {
                contextMenu.hide();
                double x = DropDownMenu.x - LocationPresentation.RADIUS / 2;
                double y = DropDownMenu.y - LocationPresentation.RADIUS / 2;

                final Location newLocation = new Location(component, Location.Type.INCONSISTENT, x, y);

                // Add a new location
                UndoRedoStack.pushAndPerform(() -> { // Perform
                    component.addLocation(newLocation);
                }, () -> { // Undo
                    component.removeLocation(newLocation);
                }, "Added inconsistent location '" + newLocation + "' to component '" + component.getName() + "'", "add-circle");
            });

            contextMenu.addSpacerElement();

            contextMenu.addClickableListElement("Contains deadlock?", event -> {

                // Generate the query
                final String deadlockQuery = BackendHelper.getExistDeadlockQuery(getComponent());

                // Add proper comment
                final String deadlockComment = "Does " + component.getName() + " contain a deadlock?";

                // Add new query for this component
                final Query query = new Query(deadlockQuery, deadlockComment, QueryState.UNKNOWN);
                query.setType(QueryType.REACHABILITY);
                Ecdar.getProject().getQueries().add(query);
                query.run();
                contextMenu.hide();
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

    private void initializeFinishEdgeContextMenu(final DisplayableEdge unfinishedEdge) {

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

            finishEdgeContextMenu = new DropDownMenu(root);
            finishEdgeContextMenu.addListElement("Finish edge in a:");

            finishEdgeContextMenu.addClickableListElement("Location", event -> {
                finishEdgeContextMenu.hide();
                final Location location = new Location();
                location.initialize();

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
                finishEdgeContextMenu.hide();
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
                finishEdgeContextMenu.hide();
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
            // Check related to undo/redo stack
            if (locationPresentationMap.containsKey(loc)) {
                return;
            }

            // Create a new presentation, and register it on the map
            final LocationPresentation newLocationPresentation = new LocationPresentation(loc, newComponent);

            final ChangeListener<Number> locationPlacementChangedListener = (observable, oldValue, newValue) -> {
                final double offset = newLocationPresentation.getController().circle.getRadius() * 2 + GRID_SIZE;
                boolean hit = false;
                ItemDragHelper.DragBounds componentBounds = newLocationPresentation.getController().getDragBounds();

                //Define the x and y coordinates for the initial and final locations
                final double initialLocationX = component.get().getBox().getX() + newLocationPresentation.getController().circle.getRadius() * 2,
                        initialLocationY = component.get().getBox().getY() + newLocationPresentation.getController().circle.getRadius() * 2,
                        finalLocationX = component.get().getBox().getX() + component.get().getBox().getWidth() - newLocationPresentation.getController().circle.getRadius() * 2,
                        finalLocationY = component.get().getBox().getY() + component.get().getBox().getHeight() - newLocationPresentation.getController().circle.getRadius() * 2;

                double latestHitRight = 0,
                        latestHitDown = 0,
                        latestHitLeft = 0,
                        latestHitUp = 0;

                //Check to see if the location is placed on top of the initial location
                if (Math.abs(initialLocationX - (newLocationPresentation.getLayoutX())) < offset &&
                        Math.abs(initialLocationY - (newLocationPresentation.getLayoutY())) < offset) {
                    hit = true;
                    latestHitRight = initialLocationX;
                    latestHitDown = initialLocationY;
                    latestHitLeft = initialLocationX;
                    latestHitUp = initialLocationY;
                }

                //Check to see if the location is placed on top of the final location
                else if (Math.abs(finalLocationX - (newLocationPresentation.getLayoutX())) < offset &&
                        Math.abs(finalLocationY - (newLocationPresentation.getLayoutY())) < offset) {
                    hit = true;
                    latestHitRight = finalLocationX;
                    latestHitDown = finalLocationY;
                    latestHitLeft = finalLocationX;
                    latestHitUp = finalLocationY;
                }

                //Check to see if the location is placed on top of another location
                else {
                    for (Map.Entry<Location, LocationPresentation> entry : locationPresentationMap.entrySet()) {
                        if (entry.getValue() != newLocationPresentation &&
                                Math.abs(entry.getValue().getLayoutX() - (newLocationPresentation.getLayoutX())) < offset &&
                                Math.abs(entry.getValue().getLayoutY() - (newLocationPresentation.getLayoutY())) < offset) {
                            hit = true;
                            latestHitRight = entry.getValue().getLayoutX();
                            latestHitDown = entry.getValue().getLayoutY();
                            latestHitLeft = entry.getValue().getLayoutX();
                            latestHitUp = entry.getValue().getLayoutY();
                            break;
                        }
                    }
                }

                //If the location is not placed on top of any other locations, do not do anything
                if (!hit) {
                    return;
                }
                hit = false;

                //Find an unoccupied space for the location
                for (int i = 1; i < component.get().getBox().getWidth() / offset; i++) {

                    //Check to see, if the location can be placed to the right of the existing locations
                    if (componentBounds.trimX(latestHitRight + offset) == latestHitRight + offset) {

                        //Check if the location would be placed on the final location
                        if (Math.abs(finalLocationX - (latestHitRight + offset)) < offset &&
                                Math.abs(finalLocationY - (newLocationPresentation.getLayoutY())) < offset) {
                            hit = true;
                            latestHitRight = finalLocationX;
                        } else {
                            for (Map.Entry<Location, LocationPresentation> entry : locationPresentationMap.entrySet()) {
                                if (entry.getValue() != newLocationPresentation &&
                                        Math.abs(entry.getValue().getLayoutX() - (latestHitRight + offset)) < offset &&
                                        Math.abs(entry.getValue().getLayoutY() - (newLocationPresentation.getLayoutY())) < offset) {
                                    hit = true;
                                    latestHitRight = entry.getValue().getLayoutX();
                                    break;
                                }
                            }
                        }

                        if (!hit) {
                            newLocationPresentation.setLayoutX(latestHitRight + offset);
                            return;
                        }
                    }
                    hit = false;

                    //Check to see, if the location can be placed below the existing locations
                    if (componentBounds.trimY(latestHitDown + offset) == latestHitDown + offset) {

                        //Check if the location would be placed on the final location
                        if (Math.abs(finalLocationX - (newLocationPresentation.getLayoutX())) < offset &&
                                Math.abs(finalLocationY - (latestHitDown + offset)) < offset) {
                            hit = true;
                            latestHitDown = finalLocationY;
                        } else {
                            for (Map.Entry<Location, LocationPresentation> entry : locationPresentationMap.entrySet()) {
                                if (entry.getValue() != newLocationPresentation &&
                                        Math.abs(entry.getValue().getLayoutX() - (newLocationPresentation.getLayoutX())) < offset &&
                                        Math.abs(entry.getValue().getLayoutY() - (latestHitDown + offset)) < offset) {
                                    hit = true;
                                    latestHitDown = entry.getValue().getLayoutY();
                                    break;
                                }
                            }
                        }
                        if (!hit) {
                            newLocationPresentation.setLayoutY(latestHitDown + offset);
                            return;
                        }
                    }
                    hit = false;

                    //Check to see, if the location can be placed to the left of the existing locations
                    if (componentBounds.trimX(latestHitLeft - offset) == latestHitLeft - offset) {

                        //Check if the location would be placed on the initial location
                        if (Math.abs(initialLocationX - (latestHitLeft - offset)) < offset &&
                                Math.abs(initialLocationY - (newLocationPresentation.getLayoutY())) < offset) {
                            hit = true;
                            latestHitLeft = initialLocationX;
                        } else {
                            for (Map.Entry<Location, LocationPresentation> entry : locationPresentationMap.entrySet()) {
                                if (entry.getValue() != newLocationPresentation &&
                                        Math.abs(entry.getValue().getLayoutX() - (latestHitLeft - offset)) < offset &&
                                        Math.abs(entry.getValue().getLayoutY() - (newLocationPresentation.getLayoutY())) < offset) {
                                    hit = true;
                                    latestHitLeft = entry.getValue().getLayoutX();
                                    break;
                                }
                            }
                        }
                        if (!hit) {
                            newLocationPresentation.setLayoutX(latestHitLeft - offset);
                            return;
                        }
                    }
                    hit = false;

                    //Check to see, if the location can be placed above the existing locations
                    if (componentBounds.trimY(latestHitUp - offset) == latestHitUp - offset) {

                        //Check if the location would be placed on the initial location
                        if (Math.abs(initialLocationX - (newLocationPresentation.getLayoutX())) < offset &&
                                Math.abs(initialLocationY - (latestHitUp - offset)) < offset) {
                            hit = true;
                            latestHitUp = initialLocationY;
                        } else {
                            for (Map.Entry<Location, LocationPresentation> entry : locationPresentationMap.entrySet()) {
                                if (entry.getValue() != newLocationPresentation &&
                                        Math.abs(entry.getValue().getLayoutX() - (newLocationPresentation.getLayoutX())) < offset &&
                                        Math.abs(entry.getValue().getLayoutY() - (latestHitUp - offset)) < offset) {
                                    hit = true;
                                    latestHitUp = entry.getValue().getLayoutY();
                                    break;
                                }
                            }
                        }
                        if (!hit) {
                            newLocationPresentation.setLayoutY(latestHitUp - offset);
                            return;
                        }
                    }
                    hit = false;
                }
                modelContainerLocation.getChildren().remove(newLocationPresentation);
                locationPresentationMap.remove(newLocationPresentation.getController().locationProperty().getValue());
                newComponent.getLocations().remove(newLocationPresentation.getController().getLocation());
                Ecdar.showToast("Please select an empty space for the new location");
            };

            newLocationPresentation.layoutXProperty().addListener(locationPlacementChangedListener);

            newLocationPresentation.layoutYProperty().addListener(locationPlacementChangedListener);

            locationPresentationMap.put(loc, newLocationPresentation);

            // Add it to the view
            modelContainerLocation.getChildren().add(newLocationPresentation);

            // Bind the newly created location to the mouse and tell the ui that it is not placed yet
            if (loc.getX() == 0) {
                newLocationPresentation.setPlaced(false);
                BindingHelper.bind(loc, newComponent.getBox().getXProperty(), newComponent.getBox().getYProperty());
            }
        };

        final ListChangeListener<Location> locationListChangeListener = c -> {
            if (c.next()) {
                // Locations are added to the component
                c.getAddedSubList().forEach((loc) -> {
                    handleAddedLocation.accept(loc);

                    LocationPresentation locationPresentation = locationPresentationMap.get(loc);

                    //Ensure that the component is inside the bounds of the component
                    locationPresentation.setLayoutX(locationPresentation.getController().getDragBounds().trimX(locationPresentation.getLayoutX()));
                    locationPresentation.setLayoutY(locationPresentation.getController().getDragBounds().trimY(locationPresentation.getLayoutY()));

                    //Change the layoutXProperty slightly to invoke listener and ensure distance to existing locations
                    locationPresentation.setLayoutX(locationPresentation.getLayoutX() + 0.00001);
                });

                // Locations are removed from the component
                c.getRemoved().forEach(location -> {
                    final LocationPresentation locationPresentation = locationPresentationMap.get(location);
                    modelContainerLocation.getChildren().remove(locationPresentation);
                    locationPresentationMap.remove(location);
                });
            }
        };
        newComponent.getLocations().addListener(locationListChangeListener);

        if (!locationListChangeListenerMap.containsKey(newComponent)) {
            locationListChangeListenerMap.put(newComponent, locationListChangeListener);
        }

        newComponent.getLocations().forEach(handleAddedLocation);
    }

    private void initializeEdgeHandling(final Component newComponent) {
        final Consumer<DisplayableEdge> handleAddedEdge = edge -> {
            final EdgePresentation edgePresentation = new EdgePresentation(edge, newComponent);
            edgePresentationMap.put(edge, edgePresentation);
            modelContainerEdge.getChildren().add(edgePresentation);

            final Consumer<Circular> updateMouseTransparency = (newCircular) -> {
                edgePresentation.setMouseTransparent(newCircular == null);
            };

            edge.targetCircularProperty().addListener((obs1, oldTarget, newTarget) -> updateMouseTransparency.accept(newTarget));
            updateMouseTransparency.accept(edge.getTargetCircular());
        };

        // React on addition of edges to the component
        newComponent.getDisplayableEdges().addListener(new ListChangeListener<DisplayableEdge>() {
            @Override
            public void onChanged(final Change<? extends DisplayableEdge> c) {
                if (c.next()) {
                    // Edges are added to the component
                    c.getAddedSubList().forEach(handleAddedEdge);

                    // Edges are removed from the component
                    c.getRemoved().forEach(edge -> {
                        final EdgePresentation edgePresentation = edgePresentationMap.get(edge);
                        modelContainerEdge.getChildren().remove(edgePresentation);
                        edgePresentationMap.remove(edge);
                    });
                }
            }
        });
        newComponent.getDisplayableEdges().forEach(handleAddedEdge);
    }

    private void initializeDeclarations() {
        // Initially style the declarations
        declarationTextArea.setStyleSpans(0, ComponentPresentation.computeHighlighting(getComponent().getDeclarationsText()));
        declarationTextArea.getStyleClass().add("component-declaration");

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
        EcdarController.getActiveCanvasPresentation().getController().leaveTextAreas();

        final DisplayableEdge unfinishedEdge = getComponent().getUnfinishedEdge();

        if ((event.isShiftDown() && event.isPrimaryButtonDown()) || event.isMiddleButtonDown()) {
            final Location location = new Location();
            location.initialize();

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

            // Run later to ensure that the location is added to the locationPresentationMap first
            Platform.runLater(() -> {
                LocationPresentation locPres = locationPresentationMap.get(location);

                // Add the new location
                UndoRedoStack.push(() -> { // Perform
                    // Adding the LocationPresentation this way is necessary for further changes to the location to be handled correctly in the stack
                    locationPresentationMap.put(location, locPres);
                    modelContainerLocation.getChildren().add(locPres);

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

                // If we have an edge without a source location set the new location as its source
                locationPresentationMap.get(location).getController().isAnyEdgeWithoutSource();
            });



        } else if (event.isSecondaryButtonDown()) {
            if (unfinishedEdge == null) {
                contextMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, event.getX() * EcdarController.getActiveCanvasPresentation().getScaleX(), event.getY() * EcdarController.getActiveCanvasPresentation().getScaleY());
            } else {
                initializeFinishEdgeContextMenu(unfinishedEdge);
                finishEdgeContextMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, event.getX() * EcdarController.getActiveCanvasPresentation().getScaleX(), event.getY() * EcdarController.getActiveCanvasPresentation().getScaleY());
            }
        } else if (event.isPrimaryButtonDown()) {
            // We are drawing an edge
            if (unfinishedEdge != null) {
                // Calculate the position for the new nail (based on the component position and the canvas mouse tracker)
                final DoubleBinding x = EcdarController.getActiveCanvasPresentation().mouseTracker.gridXProperty().subtract(getComponent().getBox().getXProperty());
                final DoubleBinding y = EcdarController.getActiveCanvasPresentation().mouseTracker.gridYProperty().subtract(getComponent().getBox().getYProperty());

                // Create the abstraction for the new nail and add it to the unfinished edge
                final Nail newNail = new Nail(x, y);

                // Make sync nail if edge has none
                if (!unfinishedEdge.hasSyncNail()) {
                    newNail.setPropertyType(Edge.PropertyType.SYNCHRONIZATION);
                }

                unfinishedEdge.addNail(newNail);
            } else {
                contextMenu.hide();
            }
        }
    }

    @FXML
    private void modelContainerDragged() {
        contextMenu.hide();
    }

    public MouseTracker getMouseTracker() {
        return mouseTracker;
    }

    @Override
    public HighLevelModelObject getModel() {
        return getComponent();
    }
}
