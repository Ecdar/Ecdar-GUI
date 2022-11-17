package ecdar.abstractions;

import com.bpodgursky.jbool_expressions.*;
import com.bpodgursky.jbool_expressions.rules.RuleSet;
import ecdar.Ecdar;
import ecdar.controllers.EcdarController;
import ecdar.presentations.Grid;
import ecdar.utility.ExpressionHelper;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.helpers.Boxed;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import ecdar.utility.helpers.MouseCircular;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.util.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A component that models an I/O automata.
 */
public class Component extends HighLevelModelObject implements Boxed {
    private static final String COMPONENT = "Component";
    private static final String LOCATIONS = "locations";
    private static final String EDGES = "edges";
    private static final String INCLUDE_IN_PERIODIC_CHECK = "includeInPeriodicCheck";

    // Verification properties
    private final ObservableList<Location> locations = FXCollections.observableArrayList();
    private final ObservableList<DisplayableEdge> edges = FXCollections.observableArrayList();
    private final ObservableList<String> inputStrings = FXCollections.observableArrayList();
    private final ObservableList<String> outputStrings = FXCollections.observableArrayList();
    private final StringProperty description = new SimpleStringProperty("");
    private final StringProperty declarationsText = new SimpleStringProperty("");;

    // Background check
    private final BooleanProperty includeInPeriodicCheck = new SimpleBooleanProperty(true);

    // Styling properties
    private final Box box = new Box();
    private final BooleanProperty declarationOpen = new SimpleBooleanProperty(false);
    private final BooleanProperty firsTimeShown = new SimpleBooleanProperty(false);

    public Location previousLocationForDraggedEdge;

    /**
     * Constructs an empty component
     */
    public Component() {

    }

    /**
     * Creates a component with a specific name and a boolean value that chooses whether the colour for this component is chosen at random
     * @param doRandomColor boolean that is true if the component should choose a colour at random and false if not
     */
    public Component(final boolean doRandomColor) {
        setComponentName();

        if(doRandomColor) {
            setRandomColor();
        }

        // Make initial location
        final Location initialLocation = new Location();
        initialLocation.initialize();
        initialLocation.setType(Location.Type.INITIAL);
        initialLocation.setColorIntensity(getColorIntensity());
        initialLocation.setColor(getColor());

        // Place in center
        initialLocation.setX(box.getX() + box.getWidth() / 2);
        initialLocation.setY(box.getY() + box.getHeight() / 2);

        locations.add(initialLocation);
        initializeIOListeners();
        bindReachabilityAnalysis();
    }

    public Component(final JsonObject json) {
        setFirsTimeShown(true);

        deserialize(json);
        initializeIOListeners();
        updateIOList();
        bindReachabilityAnalysis();
    }


    /**
     * Creates a clone of another component.
     * Copies objects used for verification (e.g. locations, edges and the declarations).
     * Does not copy UI elements (sizes and positions).
     * It locations are cloned from the original component. Their ids are the same.
     * Does not initialize io listeners, but copies the input and output strings.
     * Reachability analysis binding is not initialized.
     * @return the clone
     */
    public Component cloneForVerification() {
        final Component clone = new Component();
        clone.addVerificationObjects(this);
        clone.setIncludeInPeriodicCheck(false);
        clone.inputStrings.addAll(getInputStrings());
        clone.outputStrings.addAll(getOutputStrings());
        clone.setName(getName());

        return clone;
    }


    /**
     * Adds objects used for verifications to this component.
     * @param original the component to add from
     */
    private void addVerificationObjects(final Component original) {
        for (final Location originalLoc : original.getLocations()) {
            addLocation(originalLoc.cloneForVerification());
        }

        getListOfEdgesFromDisplayableEdges(original.getDisplayableEdges()).forEach(edge -> addEdge((edge).cloneForVerification(this)));
        setDeclarationsText(original.getDeclarationsText());
    }

    /**
     * Applies angelic completion on this component.
     */
    public void applyAngelicCompletion() {
        // Cache input signature, since it could be updated when added edges
        final List<String> inputStrings = new ArrayList<>(getInputStrings());

        getLocations().forEach(location -> inputStrings.forEach(input -> {
            // Get outgoing input edges that has the chosen input
            final List<Edge> matchingEdges = getListOfEdgesFromDisplayableEdges(getOutgoingEdges(location)).stream().filter(
                    edge -> edge.getStatus().equals(EdgeStatus.INPUT) &&
                            edge.getSync().equals(input)).collect(Collectors.toList()
            );

            // If no such edges, add a self loop without a guard
            if (matchingEdges.isEmpty()) {
                final Edge edge = new Edge(location, EdgeStatus.INPUT);
                edge.setTargetLocation(location);
                edge.addSyncNail(input);
                addEdge(edge);
                return;
            }

            // If an edge has no guard and its target has no invariants, ignore.
            // Component is already input-enabled with respect to this location and input.
            if (matchingEdges.stream().anyMatch(edge -> edge.getGuard().isEmpty() &&
                    edge.getTargetLocation().getInvariant().isEmpty())) return;

            // Extract expression for which edges to create.
            // The expression is in DNF
            // We create self loops for each child expression in the disjunction.
            createAngelicSelfLoops(location, input, getNegatedEdgeExpression(matchingEdges));
        }));
    }

    /**
     * Extracts an expression that represents the negation of a list of edges.
     * Multiple edges are resolved as disjunctions.
     * There can be conjunction in guards.
     * We translate each edge to the conjunction of its guard and the invariant of its target location
     * (since it should also be satisfied).
     * The result is converted to disjunctive normal form.
     * @param edges the edges to extract from
     * @return the expression that represents the negation
     */
    private Expression<String> getNegatedEdgeExpression(final List<Edge> edges) {
        final List<String> clocks = getClocks();
        return ExpressionHelper.simplifyNegatedSimpleExpressions(
                RuleSet.toDNF(RuleSet.simplify(Not.of(Or.of(edges.stream()
                        .map(edge -> {
                                    final List<String> clocksToReset = ExpressionHelper.getUpdateSides(edge.getUpdate())
                                            .keySet().stream().filter(clocks::contains).collect(Collectors.toList());
                                    return And.of(
                                            ExpressionHelper.parseGuard(edge.getGuard()),
                                            ExpressionHelper.parseInvariantButIgnore(edge.getTargetLocation().getInvariant(), clocksToReset)
                                    );
                                }
                        ).collect(Collectors.toList()))
                ))));
    }

    /**
     * Creates self loops on a location to finish missing inputs with an angelic completion.
     * The guard expression should be in DNF without negations.
     * @param location the location to create self loops on
     * @param input the input action to use in the synchronization properties
     * @param guardExpression the expression that represents the guards of the self loops
     */
    private void createAngelicSelfLoops(final Location location, final String input, final Expression<String> guardExpression) {
        final Edge edge;

        switch (guardExpression.getExprType()) {
            case Literal.EXPR_TYPE:
                // If false, do not create any loops
                if (!((Literal<String>) guardExpression).getValue()) break;

                // It should never be true, since that should be handled before calling this method
                throw new RuntimeException("Type of expression " + guardExpression + " not accepted");
            case Variable.EXPR_TYPE:
                edge = new Edge(location, EdgeStatus.INPUT);
                edge.setTargetLocation(location);
                edge.addSyncNail(input);
                edge.addGuardNail(((Variable<String>) guardExpression).getValue());
                addEdge(edge);
                break;
            case And.EXPR_TYPE:
                edge = new Edge(location, EdgeStatus.INPUT);
                edge.setTargetLocation(location);
                edge.addSyncNail(input);
                edge.addGuardNail(String.join("&&",
                        ((And<String>) guardExpression).getChildren().stream()
                                .map(child -> {
                                    if (!child.getExprType().equals(Variable.EXPR_TYPE))
                                        throw new RuntimeException("Child " + child + " of type " +
                                                child.getExprType() + " in and expression " +
                                                guardExpression + " should be a variable");

                                    return ((Variable<String>) child).getValue();
                                })
                                .collect(Collectors.toList())
                ));
                addEdge(edge);
                break;
            case Or.EXPR_TYPE:
                guardExpression.getChildren().forEach(child -> createAngelicSelfLoops(location, input, child));
                break;
            default:
                throw new RuntimeException("Type of expression " + guardExpression + " not accepted");
        }
    }

    /**
     * Applies demonic completion on this component.
     */
    public void applyDemonicCompletion() {
        // Make a universal location
        final Location uniLocation = new Location(this, Location.Type.UNIVERSAL, 0, 0);
        addLocation(uniLocation);

        final Edge inputEdge = uniLocation.addLeftEdge("*", EdgeStatus.INPUT);
        inputEdge.setIsLocked(true);
        addEdge(inputEdge);

        final Edge outputEdge = uniLocation.addRightEdge("*", EdgeStatus.OUTPUT);
        outputEdge.setIsLocked(true);
        addEdge(outputEdge);

        // Cache input signature, since it could be updated when added edges
        final List<String> inputStrings = new ArrayList<>(getInputStrings());

        getLocations().forEach(location -> inputStrings.forEach(input -> {
            // Get outgoing input edges that has the chosen input
            final List<Edge> matchingEdges = getListOfEdgesFromDisplayableEdges(getOutgoingEdges(location)).stream().filter(
                    edge -> edge.getStatus().equals(EdgeStatus.INPUT) &&
                            edge.getSync().equals(input)).collect(Collectors.toList()
            );

            // If no such edges, add an edge to Universal
            if (matchingEdges.isEmpty()) {
                final Edge edge = new Edge(location, EdgeStatus.INPUT);
                edge.setTargetLocation(uniLocation);
                edge.addSyncNail(input);
                addEdge(edge);
                return;
            }
            // If an edge has no guard and its target has no invariants, ignore.
            // Component is already input-enabled with respect to this location and input.
            if (matchingEdges.stream().anyMatch(edge -> edge.getGuard().isEmpty() &&
                    edge.getTargetLocation().getInvariant().isEmpty())) return;

            // Extract expression for which edges to create.
            // The expression is in DNF
            // We create edges to Universal for each child expression in the disjunction.
            createDemonicEdges(location, uniLocation, input, getNegatedEdgeExpression(matchingEdges));
        }));
    }

    /**
     * Creates edges to a specified Universal location from a location
     * in order to finish missing inputs with a demonic completion.
     * @param location the location to create self loops on
     * @param universal the Universal location to create edge to
     * @param input the input action to use in the synchronization properties
     * @param guardExpression the expression that represents the guards of the edges to create
     */
    private void createDemonicEdges(final Location location, final Location universal, final String input, final Expression<String> guardExpression) {
        final Edge edge;

        switch (guardExpression.getExprType()) {
            case Literal.EXPR_TYPE:
                // If false, do not create any edges
                if (!((Literal<String>) guardExpression).getValue()) break;

                // It should never be true, since that should be handled before calling this method
                throw new RuntimeException("Type of expression " + guardExpression + " not accepted");
            case Variable.EXPR_TYPE:
                edge = new Edge(location, EdgeStatus.INPUT);
                edge.setTargetLocation(universal);
                edge.addSyncNail(input);
                edge.addGuardNail(((Variable<String>) guardExpression).getValue());
                addEdge(edge);
                break;
            case And.EXPR_TYPE:
                edge = new Edge(location, EdgeStatus.INPUT);
                edge.setTargetLocation(universal);
                edge.addSyncNail(input);
                edge.addGuardNail(String.join("&&",
                        ((And<String>) guardExpression).getChildren().stream()
                                .map(child -> ((Variable<String>) child).getValue())
                                .collect(Collectors.toList())
                ));
                addEdge(edge);
                break;
            case Or.EXPR_TYPE:
                ((Or<String>) guardExpression).getChildren().forEach(child -> createDemonicEdges(location, universal, input, child));
                break;
            default:
                throw new RuntimeException("Type of expression " + guardExpression + " not accepted");
        }
    }

    /**
     * Initialises IO listeners, adding change listener to the list of edges
     * Also adds listeners to all current edges in edges.
     */
    private void initializeIOListeners() {
        final ChangeListener<Object> listener = (observable, oldValue, newValue) -> updateIOList();

        edges.addListener((ListChangeListener<DisplayableEdge>) c -> {
            // Update the list so empty I/O status is also added to I/OLists
            updateIOList();

            while(c.next()) {
                for (final DisplayableEdge e : c.getAddedSubList()) {
                    if (e instanceof Edge) {
                        getEdgeOrSubEdges(e).forEach(edge -> addSyncListener(listener, edge));
                    } else if (e instanceof GroupedEdge) {
                        ((GroupedEdge) e).getEdges().addListener((ListChangeListener<DisplayableEdge>) change -> {
                            while(change.next()) {
                                updateIOList();
                                // Add listeners to edges added to group edge
                                for (final DisplayableEdge addedEdge : change.getAddedSubList()) {
                                    getEdgeOrSubEdges(addedEdge).forEach(edge -> addSyncListener(listener, edge));
                                }

                                // Remove listeners from edges removed from group edge
                                for (final DisplayableEdge addedEdge : change.getRemoved()) {
                                    getEdgeOrSubEdges(addedEdge).forEach(edge -> {
                                        edge.syncProperty().removeListener(listener);
                                        edge.ioStatus.removeListener(listener);
                                    });
                                }
                            }
                        });
                    }
                }

                for (final DisplayableEdge e : c.getRemoved()) {
                    getEdgeOrSubEdges(e).forEach(edge -> {
                        edge.syncProperty().removeListener(listener);
                        edge.ioStatus.removeListener(listener);
                    });
                }
            }
        });

        // Add listener to edges initially
        edges.forEach(edge -> getEdgeOrSubEdges(edge).forEach(subEdge -> addSyncListener(listener, subEdge)));
    }

    /**
     * Adds a listener to the sync property and is status of an edge.
     * @param listener the listener
     * @param edge the edge
     */
    private static void addSyncListener(final ChangeListener<Object> listener, final Edge edge) {
        edge.syncProperty().addListener(listener);
        edge.ioStatus.addListener(listener);
    }

    /**
     * Method used for updating the inputstrings and outputstrings list
     * Sorts the list alphabetically, ignoring case
     */
    public void updateIOList() {
        final List<String> localInputStrings = new ArrayList<>();
        final List<String> localOutputStrings = new ArrayList<>();

        List<Edge> edgeList = getListOfEdgesFromDisplayableEdges(edges);

        for (final Edge edge : edgeList) {
            // Extract channel id based on UPPAAL id definition
            final String channel = edge.getSync().replaceAll("^([a-zA-Z_][a-zA-Z0-9_]*).*$", "$1");

            if(edge.getStatus() == EdgeStatus.INPUT){
                if(!edge.getSync().equals("*") && !localInputStrings.contains(channel)){
                    localInputStrings.add(channel);
                }
            } else if (edge.getStatus() == EdgeStatus.OUTPUT) {
                if(!edge.getSync().equals("*") && !localOutputStrings.contains(channel)){
                    localOutputStrings.add(channel);
                }
            }
        }

        // Sort the String alphabetically, ignoring case (e.g. all strings starting with "C" are placed together)
        localInputStrings.sort((item1, item2) -> item1.compareToIgnoreCase(item2));
        localOutputStrings.sort((item1, item2) -> item1.compareToIgnoreCase(item2));

        inputStrings.setAll(localInputStrings);
        outputStrings.setAll(localOutputStrings);
    }

    private List<Edge> getListOfEdgesFromDisplayableEdges(List<DisplayableEdge> displayableEdges) {
        List<Edge> result = new ArrayList<>();
        for (final DisplayableEdge originalEdge : displayableEdges) {
            result.addAll(getEdgeOrSubEdges(originalEdge));
        }

        return result;
    }

    private List<Edge> getEdgeOrSubEdges(DisplayableEdge edge) {
        List<Edge> result = new ArrayList<>();

        if(edge instanceof Edge) {
            result.add((Edge) edge);
        } else {
            result.addAll(((GroupedEdge) edge).getEdges());
        }

        return result;
    }

    /**
     * Get all locations in this, but the initial location (if one exists).
     * O(n), n is # of locations in component.
     * @return all but the initial location
     */
    public List<Location> getAllButInitialLocations() {
        final List<Location> locations = new ArrayList<>(getLocations());

        // Remove initial location
        final Location initLoc = getInitialLocation();
        if (initLoc != null) {
            locations.remove(initLoc);
        }

        return locations;
    }

    public ObservableList<Location> getLocations() {
        return locations;
    }

    /**
     * Finds a location in this component based on its id.
     * @param id id of location to find
     * @return the found location, or null if non was found
     */
    public Location findLocation(final String id) {
        for (final Location loc : getLocations()) {
            if (loc.getId().equals(id)) return loc;
        }

        return null;
    }

    public boolean addLocation(final Location location) {
        return locations.add(location);
    }

    public boolean removeLocation(final Location location) {
        return locations.remove(location);
    }

    /**
     * Returns all DisplayableEdges of the component (returning a list potentially containing GroupEdges and Edges)
     * @return All visual edges of the component
     */
    public ObservableList<DisplayableEdge> getDisplayableEdges() {
        return edges;
    }

    /**
     * Returns all edges of the component (returning the sub-edges of GroupEdges as individual edges)
     * @return All functional edges of the component
     */
    public List<Edge> getEdges() {
        return getListOfEdgesFromDisplayableEdges(edges);
    }

    public boolean addEdge(final DisplayableEdge edge) {
        if (edges.contains(edge)) return false;
        return edges.add(edge);
    }

    public boolean removeEdge(final DisplayableEdge edge) {
        return edges.remove(edge);
    }

    /**
     * Returns all edges either starting from or ending in the given location (returning a list potentially containing GroupEdges and Edges)
     * @param location to get related edges of
     * @return List of DisplayableEdges starting from or ending in the location
     */
    public List<DisplayableEdge> getRelatedEdges(final Location location) {
        final ArrayList<DisplayableEdge> relatedEdges = new ArrayList<>();

        edges.forEach(edge -> {
            if(location.equals(edge.getSourceLocation()) || location.equals(edge.getTargetLocation())) {
                relatedEdges.add(edge);
            }
        });

        return relatedEdges;
    }

    /**
     * Get edges that has a specified location as its source.
     * This is synchronized to avoid problems with multiple threads.
     * @param location the specified location
     * @return the filtered edges
     */
    public synchronized List<DisplayableEdge> getOutgoingEdges(final Location location) {
        return getDisplayableEdges().filtered(edge -> location == edge.getSourceLocation());
    }

    public boolean isDeclarationOpen() {
        return declarationOpen.get();
    }

    public BooleanProperty declarationOpenProperty() {
        return declarationOpen;
    }

    /**
     * Gets the initial location.
     * Done with linear search.
     * O(n), where n is number of locations in this.
     * @return the initial location, or null if there is none
     */
    public Location getInitialLocation() {
        for (final Location loc : getLocations()) {
            if (loc.getType() == Location.Type.INITIAL) {
                return  loc;
            }
        }

        return null;
    }

    /**
     * Sets current initial location (if one exists) to no longer initial.
     * Then sets a new initial location.
     * O(n), where n is number of locations in this.
     * @param initialLocation new initial location.
     */
    public void setInitialLocation(final Location initialLocation) {
        // Make current initial location no longer initial
        final Location currentInitLoc = getInitialLocation();
        if (currentInitLoc != null) {
            currentInitLoc.setType(Location.Type.NORMAL);
        }

        initialLocation.setType(Location.Type.INITIAL);
    }

    public DisplayableEdge getUnfinishedEdge() {
        for (final DisplayableEdge edge : edges) {
            if (edge.getTargetLocation() == null
                    || edge.getSourceCircular() instanceof MouseCircular
                    || edge.getTargetCircular() instanceof MouseCircular)
                return edge;
        }
        return null;
    }

    public boolean isFirsTimeShown() {
        return firsTimeShown.get();
    }

    public BooleanProperty firsTimeShownProperty() {
        return firsTimeShown;
    }

    public void setFirsTimeShown(final boolean firsTimeShown) {
        this.firsTimeShown.set(firsTimeShown);
    }

    public boolean isIncludeInPeriodicCheck() {
        return includeInPeriodicCheck.get();
    }

    public BooleanProperty includeInPeriodicCheckProperty() {
        return includeInPeriodicCheck;
    }

    public void setIncludeInPeriodicCheck(final boolean includeInPeriodicCheck) {
        this.includeInPeriodicCheck.set(includeInPeriodicCheck);
    }

    /**
     * Checks if there is currently an edge without a source location.
     */
    public boolean isAnyEdgeWithoutSource() {
        DisplayableEdge edgeWithoutSource = null;

        for (DisplayableEdge edge : getDisplayableEdges()) {
            if (edge.sourceCircularProperty().get() instanceof MouseCircular) {
                edgeWithoutSource = edge;
                break;
            }
        }

        return edgeWithoutSource != null;
    }

    @Override
    public JsonObject serialize() {
        final JsonObject result = super.serialize();
        result.addProperty(DECLARATIONS, getDeclarationsText());

        final JsonArray locations = new JsonArray();
        getLocations().forEach(location -> locations.add(location.serialize()));
        result.add(LOCATIONS, locations);

        final JsonArray edges = new JsonArray();
        getListOfEdgesFromDisplayableEdges(this.edges).forEach(edge -> edges.add(edge.serialize()));

        result.add(EDGES, edges);
        result.addProperty(DESCRIPTION, getDescription());
        box.addProperties(result);
        result.addProperty(COLOR, EnabledColor.getIdentifier(getColor()));
        result.addProperty(INCLUDE_IN_PERIODIC_CHECK, isIncludeInPeriodicCheck());

        return result;
    }

    @Override
    public void deserialize(final JsonObject json) {
        super.deserialize(json);

        setDeclarationsText(json.getAsJsonPrimitive(DECLARATIONS).getAsString());

        json.getAsJsonArray(LOCATIONS).forEach(jsonElement -> {
            final Location newLocation = new Location((JsonObject) jsonElement);
            locations.add(newLocation);
        });

        json.getAsJsonArray(EDGES).forEach(jsonElement -> {
            JsonObject edgeObject = (JsonObject) jsonElement;
            final Edge newEdge = new Edge((JsonObject) jsonElement, this);

            String edgeGroup = "";
            if (edgeObject.has("group")) {
                edgeGroup = edgeObject.getAsJsonPrimitive("group").getAsString();
            }

            if (!edgeGroup.isEmpty()) {
                GroupedEdge groupedEdge = null;
                for (DisplayableEdge edge : edges) {
                    if (edge instanceof GroupedEdge && edge.getId().equals(edgeGroup)) {
                        groupedEdge = ((GroupedEdge) edge);
                        break;
                    }
                }

                if (groupedEdge == null) {
                    GroupedEdge newGroupedEdge = new GroupedEdge(newEdge);
                    newGroupedEdge.setId(edgeGroup);

                    edges.add(newGroupedEdge);
                } else {
                    boolean hasSameGuardAndUpdate = groupedEdge.addEdgeToGroup(newEdge);

                    if (!hasSameGuardAndUpdate) {
                        // The edge has the same group id as another edge, but has different guard and/or update
                        newEdge.setGroup("");
                        edges.add(newEdge);
                    }
                }
            } else {
                edges.add(newEdge);
            }
        });

        setDescription(json.getAsJsonPrimitive(DESCRIPTION).getAsString());
        box.setProperties(json);

        if (box.getWidth() == 0 && box.getHeight() == 0) {
            box.setWidth(locations.stream().max(Comparator.comparingDouble(Location::getX)).get().getX() + Grid.GRID_SIZE * 10);
            box.setHeight(locations.stream().max(Comparator.comparingDouble(Location::getY)).get().getY() + Grid.GRID_SIZE * 10);
        }

        final EnabledColor enabledColor = (json.has(COLOR) ? EnabledColor.fromIdentifier(json.getAsJsonPrimitive(COLOR).getAsString()) : null);
        if (enabledColor != null) {
            setColorIntensity(enabledColor.intensity);
            setColor(enabledColor.color);
        } else {
            setRandomColor();
            for(Location loc : locations) {
                loc.setColor(this.getColor());
                loc.setColorIntensity(this.getColorIntensity());
            }
        }

        if(json.has(INCLUDE_IN_PERIODIC_CHECK)) {
            setIncludeInPeriodicCheck(json.getAsJsonPrimitive(INCLUDE_IN_PERIODIC_CHECK).getAsBoolean());
        } else {
            setIncludeInPeriodicCheck(false);
        }
    }

    /**
     * Dyes the component and its locations.
     * @param color the color to dye with
     * @param intensity the intensity of the color
     */
    public void dye(final Color color, final Color.Intensity intensity) {
        final Color previousColor = colorProperty().get();
        final Color.Intensity previousColorIntensity = colorIntensityProperty().get();

        final Map<Location, Pair<Color, Color.Intensity>> previousLocationColors = new HashMap<>();

        for (final Location location : getLocations()) {
            if (!location.getColor().equals(previousColor)) continue;
            previousLocationColors.put(location, new Pair<>(location.getColor(), location.getColorIntensity()));
        }

        UndoRedoStack.pushAndPerform(() -> { // Perform
            // Color the component
            setColorIntensity(intensity);
            setColor(color);

            // Color all of the locations
            previousLocationColors.keySet().forEach(location -> {
                location.setColorIntensity(intensity);
                location.setColor(color);
            });
        }, () -> { // Undo
            // Color the component
            setColorIntensity(previousColorIntensity);
            setColor(previousColor);

            // Color the locations accordingly to the previous color for them
            previousLocationColors.keySet().forEach(location -> {
                location.setColorIntensity(previousLocationColors.get(location).getValue());
                location.setColor(previousLocationColors.get(location).getKey());
            });
        }, String.format("Changed the color of %s to %s", this, color.name()), "color-lens");
    }

    private void bindReachabilityAnalysis() {
        // If there is no EcdarPresentation, we are running tests and EcdarController calls will fail
        if (Ecdar.getPresentation() == null) return;

        locations.addListener((ListChangeListener<? super Location>) c -> EcdarController.runReachabilityAnalysis());
        edges.addListener((ListChangeListener<? super DisplayableEdge>) c -> EcdarController.runReachabilityAnalysis());
        declarationsTextProperty().addListener((observable, oldValue, newValue) -> EcdarController.runReachabilityAnalysis());
        includeInPeriodicCheckProperty().addListener((observable, oldValue, newValue) -> EcdarController.runReachabilityAnalysis());
    }

    public String getDescription() {
        return description.get();
    }

    public void setDescription(final String description) {
        this.description.set(description);
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public ObservableList<String> getInputStrings() {
        return inputStrings;
    }

    public ObservableList<String> getOutputStrings() {
        return outputStrings;
    }

    /**
     * Generate and sets a unique id for this system
     */
    private void setComponentName() {
        for(int counter = 1; ; counter++) {
            if(!Ecdar.getProject().getComponentNames().contains(COMPONENT + counter)){
                setName((COMPONENT + counter));
                return;
            }
        }
    }

    /**
     * Generates an id to be used by universal and inconsistent locations in this component,
     * if one has already been generated, return that instead
     * @return generated universal/inconsistent id
     */
    String generateUniIncId(){
        final String id = getUniIncId();
        if(id != null){
            return id;
        } else {
            for(int counter = 0; ;counter++){
                if(!Ecdar.getProject().getUniIncIds().contains(String.valueOf(counter))){
                    return String.valueOf(counter);
                }
            }
        }
    }

    /**
     * Gets the id used by universal and inconsistent locations located in this component,
     * if neither universal nor inconsistent locations exist in this component it returns null.
     * @return the id, or null if this component have no universal or inconsistent locations
     */
    public String getUniIncId() {
        for (final Location location : getLocations()){
            if (location.getType() == Location.Type.UNIVERSAL || location.getType() == Location.Type.INCONSISTENT) {
                return location.getId().substring(Location.ID_LETTER_LENGTH);
            }
        }
        return null;
    }

    /**
     * Gets the id of all locations in this component
     * @return ids of all locations in this component
     */
    HashSet<String> getLocationIds() {
        final HashSet<String> ids = new HashSet<>();
        for (final Location location : getLocations()){
            if(location.getType() != Location.Type.UNIVERSAL || location.getType() != Location.Type.INCONSISTENT){
                ids.add(location.getId().substring(Location.ID_LETTER_LENGTH));
            }
        }
        return ids;
    }

    /**
     * Gets the id of all edges in this component
     * @return ids of all edges in this component
     */
    HashSet<String> getEdgeIds() {
        final HashSet<String> ids = new HashSet<>();
        for (final Edge edge : getEdges()){
            ids.add(edge.getId().substring(Edge.ID_LETTER_LENGTH));
        }
        return ids;
    }

    public String getDeclarationsText() {
        return declarationsText.get();
    }

    public void setDeclarationsText(final String declarationsText) {
        this.declarationsText.set(declarationsText);
    }

    public StringProperty declarationsTextProperty() {
        return declarationsText;
    }

    @Override
    public Box getBox() {
        return box;
    }

    /**
     * Gets the clocks defined in the declarations text.
     * @return the clocks
     */
    public List<String> getClocks() {
        final List<String> clocks = new ArrayList<>();

        final Matcher matcher = Pattern.compile(".*clock\\s+([^;]+);.*").matcher(getDeclarationsText());

        if (!matcher.find()) return clocks;

        return Arrays.stream(matcher.group(1).split(",")).map(String::trim).collect(Collectors.toList());
    }

    /**
     * Gets the local variables defined in the declarations text.
     * @return the local variables
     */
    public List<String> getLocalVariables() {
        final List<String> locals = new ArrayList<>();

        Arrays.stream(getDeclarationsText().split(";")).forEach(statement -> {
            final Matcher matcher = Pattern.compile("^\\s*(\\w+)\\s+(\\w+)(\\W|$)").matcher(statement);

            if ((!matcher.find()) || matcher.group(1).equals("clock")) return;

            locals.add(matcher.group(2));
        });

        return locals;
    }

    /**
     * Gets the local variables defined in the declarations text.
     * Also gets the lower and upper bounds for these variables.
     * @return Triples containing (left) name of the variable, (middle) lower bound, (right) upper bound
     */
    public List<Triple<String, Integer, Integer>> getLocalVariablesWithBounds() {
        final List<Triple<String, Integer, Integer>> typedefs = Ecdar.getProject().getGlobalDeclarations().getTypedefs();

        final List<Triple<String, Integer, Integer>> locals = new ArrayList<>();

        Arrays.stream(getDeclarationsText().split(";")).forEach(statement -> {
            final Matcher matcher = Pattern.compile("^\\s*(\\w+)\\s+(\\w+)(\\W|$)").matcher(statement);
            if (!matcher.find()) return;

            final Optional<Triple<String, Integer, Integer>> typedef = typedefs.stream()
                    .filter(def -> def.getLeft().equals(matcher.group(1))).findAny();
            if (!typedef.isPresent()) return;

            locals.add(Triple.of(matcher.group(2), typedef.get().getMiddle(), typedef.get().getRight()));
        });

        return locals;
    }

    /**
     * Gets the first occurring universal location in this component.
     * @return the first universal location, or null if none exists
     */
    public Location getUniversalLocation() {
        final FilteredList<Location> uniLocs = getLocations().filtered(l -> l.getType().equals(Location.Type.UNIVERSAL));

        if (uniLocs.isEmpty()) return null;

        return uniLocs.get(0);
    }

    /**
     * Moves all nodes one grid size left.
     */
    public void moveAllNodesLeft() {
        getLocations().forEach(loc -> loc.setX(loc.getX() - Grid.GRID_SIZE));
        getDisplayableEdges().forEach(edge -> edge.getNails().forEach(nail -> nail.setX(nail.getX() - Grid.GRID_SIZE)));
    }

    /**
     * Moves all nodes one grid size right.
     */
    public void moveAllNodesRight() {
        getLocations().forEach(loc -> loc.setX(loc.getX() + Grid.GRID_SIZE));
        getDisplayableEdges().forEach(edge -> edge.getNails().forEach(nail -> nail.setX(nail.getX() + Grid.GRID_SIZE)));
    }

    /**
     * Moves all nodes one grid size down.
     */
    public void moveAllNodesDown() {
        getLocations().forEach(loc -> loc.setY(loc.getY() + Grid.GRID_SIZE));
        getDisplayableEdges().forEach(edge -> edge.getNails().forEach(nail -> nail.setY(nail.getY() + Grid.GRID_SIZE)));
    }

    /**
     * Moves all nodes one grid size up.
     */
    public void moveAllNodesUp() {
        getLocations().forEach(loc -> loc.setY(loc.getY() - Grid.GRID_SIZE));
        getDisplayableEdges().forEach(edge -> edge.getNails().forEach(nail -> nail.setY(nail.getY() - Grid.GRID_SIZE)));
    }

    public List<DisplayableEdge> getInputEdges() {
        return getDisplayableEdges().filtered(edge -> edge.getStatus().equals(EdgeStatus.INPUT));
    }

    public List<DisplayableEdge> getOutputEdges() {
        return getDisplayableEdges().filtered(edge -> edge.getStatus().equals(EdgeStatus.OUTPUT));
    }
}
