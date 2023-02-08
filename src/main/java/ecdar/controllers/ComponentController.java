package ecdar.controllers;

import ecdar.Ecdar;
import ecdar.abstractions.*;
import ecdar.backend.BackendHelper;
import ecdar.code_analysis.CodeAnalysis;
import ecdar.presentations.*;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import ecdar.utility.helpers.*;
import com.jfoenix.controls.JFXPopup;
import com.jfoenix.controls.JFXRippler;
import ecdar.utility.keyboard.NudgeDirection;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.layout.*;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.util.Duration;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.net.URL;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ecdar.abstractions.Project.LOCATION;
import static ecdar.presentations.ModelPresentation.*;
import static ecdar.utility.helpers.LocationPlacer.ensureCorrectPlacementOfLocation;

public class ComponentController extends ModelController implements Initializable {
    private final List<BiConsumer<Color, Color.Intensity>> updateColorDelegates = new ArrayList<>();
    private static final Map<Component, ListChangeListener<Location>> locationListChangeListenerMap = new HashMap<>();
    private static final Map<Component, Boolean> errorsAndWarningsInitialized = new HashMap<>();
    private final ObjectProperty<Component> component = new SimpleObjectProperty<>(null);
    private final Map<DisplayableEdge, EdgePresentation> edgePresentationMap = new HashMap<>();
    private final Map<Location, LocationPresentation> locationPresentationMap = new HashMap<>();

    // View elements
    public StyleClassedTextArea declarationTextArea;
    public JFXRippler toggleDeclarationButton;
    public Label x;
    public Label y;
    public Pane modelContainerSubComponent;
    public Pane modelContainerLocation;
    public Pane modelContainerEdge;

    public VBox outputSignatureContainer;
    public VBox inputSignatureContainer;

    private DropDownMenu contextMenu;
    private DropDownMenu finishEdgeContextMenu;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        declarationTextArea.setParagraphGraphicFactory(LineNumberFactory.get(declarationTextArea));

        component.addListener((obs, oldComponent, newComponent) -> {
            super.initialize(newComponent.getBox());

            initializeFrame();
            initializeToolbar();
            initializeBackground();

            // Re-run initialisation on update of width and height property
            newComponent.getBox().getWidthProperty().addListener(observable -> initializeFrame());
            newComponent.getBox().getHeightProperty().addListener(observable -> initializeFrame());

            inputSignatureContainer.heightProperty().addListener((change) -> updateMaxHeight());
            outputSignatureContainer.heightProperty().addListener((change) -> updateMaxHeight());

            // Bind the declarations of the abstraction the view
            declarationTextArea.replaceText(0, declarationTextArea.getLength(), newComponent.getDeclarationsText());
            declarationTextArea.textProperty().addListener((observable, oldDeclaration, newDeclaration) -> newComponent.setDeclarationsText(newDeclaration));

            // Find the clocks in the decls
            newComponent.declarationsTextProperty().addListener((observable, oldValue, newValue) -> {
                final List<String> clocks = new ArrayList<>();

                final String strippedDecls = newValue.replaceAll("[\\r\\n]+", "");

                Pattern pattern = Pattern.compile("clock (?<CLOCKS>[^;]*);");
                Matcher matcher = pattern.matcher(strippedDecls);

                while (matcher.find()) {
                    final String[] clockStrings = matcher.group("CLOCKS").split(",");
                    for (String clockString : clockStrings) {
                        clocks.add(clockString.replaceAll("\\s", ""));
                    }
                }

                //TODO this logs the clocks System.out.println(clocks);
            });

            initializeEdgeHandling();
            initializeLocationHandling();
            initializeDeclarations();
            initializeSignature();
            initializeSignatureListeners();

            if (!errorsAndWarningsInitialized.containsKey(newComponent) || !errorsAndWarningsInitialized.get(newComponent)) {
                initializeNoIncomingEdgesWarning();
                errorsAndWarningsInitialized.put(newComponent, true);
            }
        });

        initializeContextMenu();

        declarationTextArea.textProperty().addListener((obs, oldText, newText) ->
                declarationTextArea.setStyleSpans(0, UPPAALSyntaxHighlighter.computeHighlighting(newText)));
    }

    private void initializeToolbar() {
        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Set the background of the toolbar
            toolbar.setBackground(new Background(new BackgroundFill(
                    newColor.getColor(newIntensity),
                    CornerRadii.EMPTY,
                    Insets.EMPTY
            )));

            // Set the icon color and rippler color of the toggleDeclarationButton
            toggleDeclarationButton.setRipplerFill(newColor.getTextColor(newIntensity));

            toolbar.setPrefHeight(TOOLBAR_HEIGHT);
            toggleDeclarationButton.setBackground(Background.EMPTY);
        };

        updateColorDelegates.add(updateColor);

        getComponent().colorProperty().addListener(observable -> updateColor.accept(getComponent().getColor(), getComponent().getColorIntensity()));

        updateColor.accept(getComponent().getColor(), getComponent().getColorIntensity());

        // Set a hover effect for the controller.toggleDeclarationButton
        toggleDeclarationButton.setOnMouseEntered(event -> toggleDeclarationButton.setCursor(Cursor.HAND));
        toggleDeclarationButton.setOnMouseExited(event -> toggleDeclarationButton.setCursor(Cursor.DEFAULT));
        toggleDeclarationButton.setOnMousePressed(this::toggleDeclaration);
    }

    private void initializeFrame() {
        final Shape[] mask = new Shape[1];
        final Rectangle rectangle = new Rectangle(getComponent().getBox().getWidth(), getComponent().getBox().getHeight());

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Mask the parent of the frame (will also mask the background)
            mask[0] = Path.subtract(rectangle, TOP_LEFT_CORNER);
            frame.setClip(mask[0]);
            background.setClip(Path.union(mask[0], mask[0]));
            background.setOpacity(0.5);

            // Bind the missing lines that we cropped away
            topLeftLine.setStartX(CORNER_SIZE);
            topLeftLine.setStartY(0);
            topLeftLine.setEndX(0);
            topLeftLine.setEndY(CORNER_SIZE);
            topLeftLine.setStroke(newColor.getColor(newIntensity.next(2)));
            topLeftLine.setStrokeWidth(1.25);
            StackPane.setAlignment(topLeftLine, Pos.TOP_LEFT);

            // Set the stroke color to two shades darker
            frame.setBorder(new Border(new BorderStroke(
                    newColor.getColor(newIntensity.next(2)),
                    BorderStrokeStyle.SOLID,
                    CornerRadii.EMPTY,
                    new BorderWidths(1),
                    Insets.EMPTY
            )));
        };

        updateColorDelegates.add(updateColor);

        getComponent().colorProperty().addListener(observable -> {
            updateColor.accept(getComponent().getColor(), getComponent().getColorIntensity());
        });

        updateColor.accept(getComponent().getColor(), getComponent().getColorIntensity());
    }

    private void initializeBackground() {
        // Bind the background width and height to the values in the model
        background.widthProperty().bindBidirectional(getComponent().getBox().getWidthProperty());
        background.heightProperty().bindBidirectional(getComponent().getBox().getHeightProperty());

        final BiConsumer<Color, Color.Intensity> updateColor = (newColor, newIntensity) -> {
            // Set the background color to the lightest possible version of the color
            background.setFill(newColor.getColor(newIntensity.next(-10).next(2)));
        };

        updateColorDelegates.add(updateColor);

        getComponent().colorProperty().addListener(observable -> {
            updateColor.accept(getComponent().getColor(), getComponent().getColorIntensity());
        });

        updateColor.accept(getComponent().getColor(), getComponent().getColorIntensity());
    }

    /***
     * Inserts the initial edges of the component to the input/output signature
     */
    private void initializeSignature() {
        getComponent().getOutputStrings().forEach((channel) -> insertSignatureArrow(channel, EdgeStatus.OUTPUT));
        getComponent().getInputStrings().forEach((channel) -> insertSignatureArrow(channel, EdgeStatus.INPUT));
    }

    /***
     * Initialize the listeners, that listen for changes in the input and output edges of the presented component.
     * The view is updated whenever an insert (deletions are also a type of insert) is reported
     */
    private void initializeSignatureListeners() {
        getComponent().getOutputStrings().addListener((ListChangeListener<String>) c -> {
            // By clearing the container we don't have to fiddle with which elements are removed and added
            outputSignatureContainer.getChildren().clear();
            while (c.next()) {
                c.getAddedSubList().forEach((channel) -> insertSignatureArrow(channel, EdgeStatus.OUTPUT));
            }
        });

        getComponent().getInputStrings().addListener((ListChangeListener<String>) c -> {
            inputSignatureContainer.getChildren().clear();
            while (c.next()) {
                c.getAddedSubList().forEach((channel) -> insertSignatureArrow(channel, EdgeStatus.INPUT));
            }
        });
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

        checkLocations.accept(getComponent());

        // Check location whenever we get new edges
        getComponent().getDisplayableEdges().addListener(new ListChangeListener<DisplayableEdge>() {
            @Override
            public void onChanged(final Change<? extends DisplayableEdge> c) {
                while (c.next()) {
                    checkLocations.accept(getComponent());
                }
            }
        });

        // Check location whenever we get new locations
        getComponent().getLocations().addListener(new ListChangeListener<Location>() {
            @Override
            public void onChanged(final Change<? extends Location> c) {
                while (c.next()) {
                    checkLocations.accept(getComponent());
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
                newLocation.initialize(getUniqueLocationId());

                double x = DropDownMenu.x - LocationPresentation.RADIUS / 2;
                newLocation.setX(x);

                double y = DropDownMenu.y - LocationPresentation.RADIUS / 2;
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

            // Adds the add universal location element to the drop-down menu, this element adds an universal location and its required edges
            contextMenu.addClickableListElement("Add Universal Location", event -> {
                contextMenu.hide();
                double x = DropDownMenu.x - LocationPresentation.RADIUS / 2;
                double y = DropDownMenu.y - LocationPresentation.RADIUS / 2;

                final Location newLocation = new Location(component, Location.Type.UNIVERSAL, getUniqueLocationId(), x, y);

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

            // Adds the add inconsistent location element to the dropdown menu, this element adds an inconsistent location
            contextMenu.addClickableListElement("Add Inconsistent Location", event -> {
                contextMenu.hide();
                double x = DropDownMenu.x - LocationPresentation.RADIUS / 2;
                double y = DropDownMenu.y - LocationPresentation.RADIUS / 2;

                final Location newLocation = new Location(component, Location.Type.INCONSISTENT, getUniqueLocationId(), x, y);

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
                Ecdar.getQueryExecutor().executeQuery(query);
                contextMenu.hide();
            });

            contextMenu.addSpacerElement();
            contextMenu.addColorPicker(component, component::dye);
        };

        component.addListener((obs, oldComponent, newComponent) -> {
            initializeDropDownMenu.accept(newComponent);
        });

        Platform.runLater(() -> Ecdar.getProject().getComponents().addListener((ListChangeListener<Component>) c -> initializeDropDownMenu.accept(getComponent())));

        initializeDropDownMenu.accept(getComponent());
    }

    private String getUniqueLocationId() {
        final var currentIds = getComponent().getLocations().stream().map(Location::getId).collect(Collectors.toSet());
        for (int counter = 1; ; counter++) {
            if (currentIds.contains(LOCATION + counter)) {
                return LOCATION + counter;
            }
        }
    }

    private void initializeFinishEdgeContextMenu(final DisplayableEdge unfinishedEdge) {
        final Consumer<Component> initializeDropDownMenu = (component) -> {
            if (component == null) {
                return;
            }

            final Consumer<LocationAware> setCoordinates = (locationAware) -> {
                double x = DropDownMenu.x;
                double y = DropDownMenu.y;

                locationAware.xProperty().set(x);
                locationAware.yProperty().set(y);
            };

            finishEdgeContextMenu = new DropDownMenu(root);
            finishEdgeContextMenu.addListElement("Finish edge in a:");

            finishEdgeContextMenu.addClickableListElement("Location", event -> {
                finishEdgeContextMenu.hide();
                final Location location = new Location();
                location.initialize(getUniqueLocationId());

                location.setColorIntensity(getComponent().getColorIntensity());
                location.setColor(getComponent().getColor());

                if (component.isAnyEdgeWithoutSource()) {
                    unfinishedEdge.setSourceLocation(location);
                } else {
                    unfinishedEdge.setTargetLocation(location);
                }

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

                final Location newLocation = new Location(component, Location.Type.UNIVERSAL, getUniqueLocationId(), x, y);

                final Edge inputEdge = newLocation.addLeftEdge("*", EdgeStatus.INPUT);
                inputEdge.setIsLocked(true);

                final Edge outputEdge = newLocation.addRightEdge("*", EdgeStatus.OUTPUT);
                outputEdge.setIsLocked(true);

                if (component.isAnyEdgeWithoutSource()) {
                    unfinishedEdge.setSourceLocation(newLocation);
                } else {
                    unfinishedEdge.setTargetLocation(newLocation);
                }

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

                final Location newLocation = new Location(component, Location.Type.INCONSISTENT, getUniqueLocationId(), x, y);

                if (component.isAnyEdgeWithoutSource()) {
                    unfinishedEdge.setSourceLocation(newLocation);
                } else {
                    unfinishedEdge.setTargetLocation(newLocation);
                }

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

    private void initializeLocationHandling() {
        final Consumer<LocationPresentation> removeLocationPresentation = locationPresentation -> {
            modelContainerLocation.getChildren().remove(locationPresentation);
            locationPresentationMap.remove(locationPresentation.getController().locationProperty().getValue());
            getComponent().getLocations().remove(locationPresentation.getController().locationProperty().getValue());
            Ecdar.showToast("Please select an empty space for the new location");
        };

        final ListChangeListener<Location> locationListChangeListener = c -> {
            if (c.next()) {
                // Locations are added to the component
                c.getAddedSubList().forEach((loc) -> {
                    // Check related to undo/redo stack
                    if (!locationPresentationMap.containsKey(loc)) {
                        addLocation(removeLocationPresentation, loc);
                    }

                    LocationPresentation locationPresentation = locationPresentationMap.get(loc);

                    //Ensure that the location is inside the bounds of the component
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

        getComponent().getLocations().addListener(locationListChangeListener);

        if (!locationListChangeListenerMap.containsKey(getComponent())) {
            locationListChangeListenerMap.put(getComponent(), locationListChangeListener);
        }

        getComponent().getLocations().forEach(loc -> {
            addLocation(removeLocationPresentation, loc);
        });
    }

    /***
     * Handles the addition of a new location
     * @param removeLocationPresentation A consumer used to remove the location in case it is impossible to place it in the component
     * @param loc The location to add to the component
     */
    private void addLocation(Consumer<LocationPresentation> removeLocationPresentation, Location loc) {
        LocationPresentation locationPresentation = ensureCorrectPlacementOfLocation(getComponent(), locationPresentationMap.values(), loc, removeLocationPresentation);
        locationPresentationMap.put(loc, locationPresentation);
        modelContainerLocation.getChildren().add(locationPresentation);

        // Bind the newly created location to the mouse and tell the ui that it is not placed yet
        if (loc.getX() == 0) {
            locationPresentation.setPlaced(false);
            BindingHelper.bind(loc, getComponent().getBox().getXProperty(), getComponent().getBox().getYProperty());
        }
    }

    private void initializeEdgeHandling() {
        final Consumer<DisplayableEdge> handleAddedEdge = edge -> {
            final EdgePresentation edgePresentation = new EdgePresentation(edge, getComponent());
            edgePresentationMap.put(edge, edgePresentation);
            modelContainerEdge.getChildren().add(edgePresentation);

            final Consumer<Circular> updateMouseTransparency = (newCircular) -> {
                edgePresentation.setMouseTransparent(newCircular == null || newCircular instanceof MouseCircular);
            };

            edge.targetCircularProperty().addListener((obs1, oldTarget, newTarget) -> updateMouseTransparency.accept(newTarget));
            updateMouseTransparency.accept(edge.getTargetCircular());
        };

        // React on addition of edges to the component
        getComponent().getDisplayableEdges().addListener((ListChangeListener<DisplayableEdge>) c -> {
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
        });

        getComponent().getDisplayableEdges().forEach(handleAddedEdge);
    }

    private void initializeDeclarations() {
        // Initially style the declarations
        declarationTextArea.setStyleSpans(0, UPPAALSyntaxHighlighter.computeHighlighting(getComponent().getDeclarationsText()));
        declarationTextArea.getStyleClass().add("component-declaration");

        final Circle circle = new Circle(0);
        if (getComponent().isDeclarationOpen()) {
            circle.setRadius(1000);
        }
        final ObjectProperty<Node> clip = new SimpleObjectProperty<>(circle);
        declarationTextArea.clipProperty().bind(clip);
        clip.set(circle);
    }

    /***
     * Inserts a new {@link ecdar.presentations.SignatureArrow} in the containers for either input or output signature
     * @param channel A String with the channel name that should be shown with the arrow
     * @param status An EdgeStatus for the type of arrow to insert
     */
    private void insertSignatureArrow(final String channel, final EdgeStatus status) {
        SignatureArrow newArrow = new SignatureArrow(channel, status, getComponent());
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
        double maxHeight = getMaxHeight();
        if (maxHeight > getComponent().getBox().getHeight()) {
            getComponent().getBox().getHeightProperty().set(maxHeight);
        }
    }

    /***
     * Finds the max height of the input/output signature container and the component
     * @return a double of the largest height
     */
    private double getMaxHeight() {
        double inputHeight = inputSignatureContainer.getHeight();
        double outputHeight = outputSignatureContainer.getHeight();
        double componentHeight = getComponent().getBox().getHeight();

        double maxSignatureHeight = Math.max(outputHeight, inputHeight);

        return Math.max(maxSignatureHeight, componentHeight);
    }

    /***
     * Toggle the declaration of the component with a ripple effect originating from the MouseEvent
     * @param mouseEvent to use for the origin of the ripple effect
     */
    private void toggleDeclaration(final MouseEvent mouseEvent) {
        final Circle circle = new Circle(0);
        circle.setCenterX(getComponent().getBox().getWidth() - (toggleDeclarationButton.getWidth() - mouseEvent.getX()));
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

    /***
     * Mark the component as selected in the view
     */
    public void componentSelected() {
        updateColorDelegates.forEach(colorConsumer -> colorConsumer.accept(SelectHelper.SELECT_COLOR, SelectHelper.SELECT_COLOR_INTENSITY_NORMAL));
    }

    /***
     * Mark the component as not selected in the view
     */
    public void componentUnselected() {
        updateColorDelegates.forEach(colorConsumer -> colorConsumer.accept(getComponent().getColor(), getComponent().getColorIntensity()));
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
     * Moves all nodes left.
     */
    public void moveAllNodesLeft() {
        getComponent().getLocations().forEach(loc -> loc.setX(loc.getX() + NudgeDirection.LEFT.getXOffset()));
        getComponent().getDisplayableEdges().forEach(edge -> edge.getNails().forEach(nail -> nail.setX(nail.getX() + NudgeDirection.LEFT.getXOffset())));
    }

    /**
     * Moves all nodes right.
     */
    public void moveAllNodesRight() {
        getComponent().getLocations().forEach(loc -> loc.setX(loc.getX() + NudgeDirection.RIGHT.getXOffset()));
        getComponent().getDisplayableEdges().forEach(edge -> edge.getNails().forEach(nail -> nail.setX(nail.getX() + NudgeDirection.RIGHT.getXOffset())));
    }

    /**
     * Moves all nodes down.
     */
    public void moveAllNodesDown() {
        getComponent().getLocations().forEach(loc -> loc.setY(loc.getY() + NudgeDirection.DOWN.getYOffset()));
        getComponent().getDisplayableEdges().forEach(edge -> edge.getNails().forEach(nail -> nail.setY(nail.getY() + NudgeDirection.DOWN.getYOffset())));
    }

    /**
     * Moves all nodes up.
     */
    public void moveAllNodesUp() {
        getComponent().getLocations().forEach(loc -> loc.setY(loc.getY() + NudgeDirection.UP.getYOffset()));
        getComponent().getDisplayableEdges().forEach(edge -> edge.getNails().forEach(nail -> nail.setY(nail.getY() + NudgeDirection.UP.getYOffset())));
    }

    /***
     * Handle the component being pressed based on the mouse button and hotkeys
     * @param event to use for handling the action
     */
    @FXML
    private void modelContainerPressed(final MouseEvent event) {
        EcdarController.getActiveCanvasPresentation().getController().leaveTextAreas();
        final DisplayableEdge unfinishedEdge = getComponent().getUnfinishedEdge();

        if ((event.isShiftDown() && event.isPrimaryButtonDown()) || event.isMiddleButtonDown()) {
            final Location location = new Location();
            location.initialize(getUniqueLocationId());

            location.setX(event.getX());
            location.setY(event.getY());

            location.setColorIntensity(getComponent().getColorIntensity());
            location.setColor(getComponent().getColor());

            if (unfinishedEdge != null) {
                if (getComponent().isAnyEdgeWithoutSource()) {
                    unfinishedEdge.setSourceLocation(location);
                } else {
                    unfinishedEdge.setTargetLocation(location);
                }

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
            });
        } else if (event.isSecondaryButtonDown()) {
            if (unfinishedEdge == null) {
                contextMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, event.getX() * EcdarController.getActiveCanvasZoomFactor().get(), event.getY() * EcdarController.getActiveCanvasZoomFactor().get());
            } else {
                initializeFinishEdgeContextMenu(unfinishedEdge);
                finishEdgeContextMenu.show(JFXPopup.PopupVPosition.TOP, JFXPopup.PopupHPosition.LEFT, event.getX() * EcdarController.getActiveCanvasZoomFactor().get(), event.getY() * EcdarController.getActiveCanvasZoomFactor().get());
            }
        } else if (event.isPrimaryButtonDown()) {
            // We are drawing an edge
            if (unfinishedEdge != null) {
                // Create the abstraction for the new nail and add it to the unfinished edge
                final Nail newNail = new Nail(event.getX(), event.getY());

                // Make sync nail if edge has none
                if (!unfinishedEdge.hasSyncNail()) {
                    newNail.setPropertyType(Edge.PropertyType.SYNCHRONIZATION);
                }

                if (getComponent().isAnyEdgeWithoutSource()) {
                    unfinishedEdge.getNails().add(0, newNail);
                } else {
                    unfinishedEdge.addNail(newNail);
                }
            } else {
                contextMenu.hide();
            }
        }
    }

    @FXML
    private void modelContainerDragged() {
        contextMenu.hide();
    }

    @Override
    public HighLevelModel getModel() {
        return getComponent();
    }

    /**
     * Gets the minimum possible height when dragging the anchor.
     * The height is based on the y coordinate of locations, nails and the signature arrows
     *
     * @return the minimum possible height.
     */
    @Override
    double getDragAnchorMinWidth() {
        double minWidth = 10 * Ecdar.CANVAS_PADDING;

        for (final Location location : getComponent().getLocations()) {
            minWidth = Math.max(minWidth, location.getX() + Ecdar.CANVAS_PADDING * 2);
        }

        for (final Edge edge : getComponent().getEdges()) {
            for (final Nail nail : edge.getNails()) {
                minWidth = Math.max(minWidth, nail.getX() + Ecdar.CANVAS_PADDING);
            }
        }

        return minWidth;
    }

    /**
     * Gets the minimum possible height when dragging the anchor.
     * The height is based on the y coordinate of locations, nails and the signature arrows
     *
     * @return the minimum possible height.
     */
    @Override
    double getDragAnchorMinHeight() {
        double minHeight = 10 * Ecdar.CANVAS_PADDING;

        for (final Location location : getComponent().getLocations()) {
            minHeight = Math.max(minHeight, location.getY() + Ecdar.CANVAS_PADDING * 2);
        }

        for (final Edge edge : getComponent().getEdges()) {
            for (final Nail nail : edge.getNails()) {
                minHeight = Math.max(minHeight, nail.getY() + Ecdar.CANVAS_PADDING);
            }
        }

        //Component should not get smaller than the height of the input/output signature containers
        minHeight = Math.max(inputSignatureContainer.getHeight(), minHeight);
        minHeight = Math.max(outputSignatureContainer.getHeight(), minHeight);

        return minHeight;
    }
}
