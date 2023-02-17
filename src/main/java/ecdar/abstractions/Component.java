package ecdar.abstractions;

import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.helpers.Boxed;
import ecdar.utility.helpers.MouseCircular;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.util.Pair;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static ecdar.abstractions.Project.LOCATION;

/**
 * A component that models an I/O automata.
 */
public class Component extends HighLevelModel implements Boxed {
    private static final String LOCATIONS = "locations";
    private static final String EDGES = "edges";
    private static final String INCLUDE_IN_PERIODIC_CHECK = "includeInPeriodicCheck";

    // Verification properties
    private final ObservableList<Location> locations = FXCollections.observableArrayList();
    private final ObservableList<DisplayableEdge> displayableEdges = FXCollections.observableArrayList();
    private final ObservableList<String> inputStrings = FXCollections.observableArrayList();
    private final ObservableList<String> outputStrings = FXCollections.observableArrayList();
    private final StringProperty description = new SimpleStringProperty("");
    private final StringProperty declarationsText = new SimpleStringProperty("");

    // Background check
    private final BooleanProperty includeInPeriodicCheck = new SimpleBooleanProperty(true);

    // Styling properties
    private final Box box = new Box();
    private final BooleanProperty declarationOpen = new SimpleBooleanProperty(false);

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
    public Component(final boolean doRandomColor, final String name) {
        setName(name);

        // Make initial location
        final Location initialLocation = new Location();
        initialLocation.initialize(LOCATION + "1");
        initialLocation.setType(Location.Type.INITIAL);

        // Place in center
        initialLocation.setX(box.getX() + box.getWidth() / 2);
        initialLocation.setY(box.getY() + box.getHeight() / 2);

        if (doRandomColor) {
            // Run random coloring later to avoid Ecdar.getProject() being null
            Platform.runLater(() -> {
                setRandomColor();
                initialLocation.setColorIntensity(getColorIntensity());
                initialLocation.setColor(getColor());
            });
        } else {
            initialLocation.setColorIntensity(getColorIntensity());
            initialLocation.setColor(getColor());
        }

        addLocation(initialLocation);
        initializeIOListeners();
    }

    public Component(final JsonObject json) {
        deserialize(json);
        initializeIOListeners();
        updateIOList();
    }

    /**
     * Initialises IO listeners, adding change listener to the list of edges
     * Also adds listeners to all current edges in edges.
     */
    private void initializeIOListeners() {
        final ChangeListener<Object> listener = (observable, oldValue, newValue) -> updateIOList();

        displayableEdges.addListener((ListChangeListener<DisplayableEdge>) c -> {
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
        getEdges().forEach(edge -> getEdgeOrSubEdges(edge).forEach(subEdge -> addSyncListener(listener, subEdge)));
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
        return displayableEdges;
    }

    /**
     * Returns all edges of the component (returning the sub-edges of GroupEdges as individual edges)
     * @return All functional edges of the component
     */
    public List<Edge> getEdges() {
        return getListOfEdgesFromDisplayableEdges(displayableEdges);
    }

    public boolean addEdge(final DisplayableEdge edge) {
        if (displayableEdges.contains(edge)) return false;
        return displayableEdges.add(edge);
    }

    public boolean removeEdge(final DisplayableEdge edge) {
        return displayableEdges.remove(edge);
    }

    /**
     * Returns all edges either starting from or ending in the given location (returning a list potentially containing GroupEdges and Edges)
     * @param location to get related edges of
     * @return List of DisplayableEdges starting from or ending in the location
     */
    public List<DisplayableEdge> getRelatedEdges(final Location location) {
        final ArrayList<DisplayableEdge> relatedEdges = new ArrayList<>();

        displayableEdges.forEach(edge -> {
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

    public List<Edge> getListOfEdgesFromDisplayableEdges(List<DisplayableEdge> displayableEdges) {
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
        for (final DisplayableEdge edge : displayableEdges) {
            if (edge.getTargetLocation() == null
                    || edge.getSourceCircular() instanceof MouseCircular
                    || edge.getTargetCircular() instanceof MouseCircular)
                return edge;
        }
        return null;
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

    public String getDeclarationsText() {
        return declarationsText.get();
    }

    public void setDeclarationsText(final String declarationsText) {
        this.declarationsText.set(declarationsText);
    }

    public StringProperty declarationsTextProperty() {
        return declarationsText;
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

    public List<DisplayableEdge> getInputEdges() {
        return getDisplayableEdges().filtered(edge -> edge.getStatus().equals(EdgeStatus.INPUT));
    }

    public List<DisplayableEdge> getOutputEdges() {
        return getDisplayableEdges().filtered(edge -> edge.getStatus().equals(EdgeStatus.OUTPUT));
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

    /**
     * Method used for updating the inputstrings and outputstrings list
     * Sorts the list alphabetically, ignoring case
     */
    public void updateIOList() {
        final List<String> localInputStrings = new ArrayList<>();
        final List<String> localOutputStrings = new ArrayList<>();

        List<Edge> edgeList = getListOfEdgesFromDisplayableEdges(displayableEdges);

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

    @Override
    public Box getBox() {
        return box;
    }

    @Override
    public JsonObject serialize() {
        final JsonObject result = super.serialize();
        result.addProperty(DECLARATIONS, getDeclarationsText());

        final JsonArray locations = new JsonArray();
        getLocations().forEach(location -> locations.add(location.serialize()));
        result.add(LOCATIONS, locations);

        final JsonArray edges = new JsonArray();
        getListOfEdgesFromDisplayableEdges(this.displayableEdges).forEach(edge -> edges.add(edge.serialize()));

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
                for (DisplayableEdge edge : displayableEdges) {
                    if (edge instanceof GroupedEdge && edge.getId().equals(edgeGroup)) {
                        groupedEdge = ((GroupedEdge) edge);
                        break;
                    }
                }

                if (groupedEdge == null) {
                    GroupedEdge newGroupedEdge = new GroupedEdge(newEdge);
                    newGroupedEdge.setId(edgeGroup);

                    displayableEdges.add(newGroupedEdge);
                } else {
                    boolean hasSameGuardAndUpdate = groupedEdge.addEdgeToGroup(newEdge);

                    if (!hasSameGuardAndUpdate) {
                        // The edge has the same group id as another edge, but has different guard and/or update
                        newEdge.setGroup("");
                        displayableEdges.add(newEdge);
                    }
                }
            } else {
                displayableEdges.add(newEdge);
            }
        });

        setDescription(json.getAsJsonPrimitive(DESCRIPTION).getAsString());
        box.setProperties(json);

        if (box.getWidth() == 0 && box.getHeight() == 0) {
            box.setWidth(locations.stream().max(Comparator.comparingDouble(Location::getX)).get().getX() + 10 * 10);
            box.setHeight(locations.stream().max(Comparator.comparingDouble(Location::getY)).get().getY() + 10 * 10);
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
}
