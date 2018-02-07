package ecdar.abstractions;

import ecdar.Ecdar;
import ecdar.controllers.EcdarController;
import ecdar.presentations.Grid;
import ecdar.utility.UndoRedoStack;
import ecdar.utility.colors.Color;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.helpers.Boxed;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.util.Pair;

import java.util.*;

/**
 * A component that models an I/O automata.
 */
public class Component extends HighLevelModelObject implements Boxed {
    static final String COMPONENT = "Component";
    private static final String LOCATIONS = "locations";
    private static final String EDGES = "edges";
    private static final String INCLUDE_IN_PERIODIC_CHECK = "includeInPeriodicCheck";

    // Verification properties
    private final ObservableList<Location> locations = FXCollections.observableArrayList();
    private final ObservableList<Edge> edges = FXCollections.observableArrayList();
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

    /**
     * Constructs an empty component
     */
    private Component() {

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
        initialLocation.setX(Grid.snap(box.getX() + box.getWidth() / 2));
        initialLocation.setY(Grid.snap(box.getY() + box.getHeight() / 2));

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
     * Does not initialize io listeners.
     * Reachability analysis binding is not initialized.
     * @return the clone
     */
    public Component cloneForVerification() {
        final Component clone = new Component();
        clone.addVerificationObjects(this);

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

        for (final Edge originalEdge : original.getEdges()) {
            addEdge(originalEdge.cloneForVerification(this));
        }

        setDeclarationsText(original.getDeclarationsText());
    }

    /**
     * Initialises IO listeners, adding change listener to the list of edges
     * Also adds listeners to all current edges in edges.
     */
    private void initializeIOListeners() {
        final ChangeListener<Object> listener = (observable, oldValue, newValue) -> updateIOList();

        edges.addListener((ListChangeListener<Edge>) c -> {
            // Update the list so empty I/O status is also added to I/OLists
            updateIOList();

            while(c.next()) {
                for (final Edge e : c.getAddedSubList()) {
                    addSyncListener(listener, e);
                }

                for (final Edge e : c.getRemoved()) {
                    e.syncProperty().removeListener(listener);
                    e.ioStatus.removeListener(listener);
                }
            }
        });

        // Add listener to edges initially
        edges.forEach(edge -> addSyncListener(listener, edge));
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

        for (final Edge edge : edges) {
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

    public ObservableList<Edge> getEdges() {
        return edges;
    }

    public boolean addEdge(final Edge edge) {
        return edges.add(edge);
    }

    public boolean removeEdge(final Edge edge) {
        return edges.remove(edge);
    }

    public List<Edge> getRelatedEdges(final Location location) {
        final ArrayList<Edge> relatedEdges = new ArrayList<>();

        edges.forEach(edge -> {
            if(location.equals(edge.getSourceLocation()) ||location.equals(edge.getTargetLocation())) {
                relatedEdges.add(edge);
            }
        });

        return relatedEdges;
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

    public Edge getUnfinishedEdge() {
        for (final Edge edge : edges) {
            if (edge.getTargetLocation() == null)
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

    @Override
    public JsonObject serialize() {
        final JsonObject result = super.serialize();

        result.addProperty(DECLARATIONS, getDeclarationsText());

        final JsonArray locations = new JsonArray();
        getLocations().forEach(location -> locations.add(location.serialize()));
        result.add(LOCATIONS, locations);

        final JsonArray edges = new JsonArray();
        getEdges().forEach(edge -> edges.add(edge.serialize()));
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
            final Edge newEdge = new Edge((JsonObject) jsonElement, this);
            edges.add(newEdge);
        });

        setDescription(json.getAsJsonPrimitive(DESCRIPTION).getAsString());

        box.setProperties(json);

        final EnabledColor enabledColor = EnabledColor.fromIdentifier(json.getAsJsonPrimitive(COLOR).getAsString());
        if (enabledColor != null) {
            setColorIntensity(enabledColor.intensity);
            setColor(enabledColor.color);
        }

        setIncludeInPeriodicCheck(json.getAsJsonPrimitive(INCLUDE_IN_PERIODIC_CHECK).getAsBoolean());
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
        locations.addListener((ListChangeListener<? super Location>) c -> EcdarController.runReachabilityAnalysis());
        edges.addListener((ListChangeListener<? super Edge>) c -> EcdarController.runReachabilityAnalysis());
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
     * if neither universal nor inconsistent locations exist in this component it returns null
     */
    String getUniIncId() {
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
}
