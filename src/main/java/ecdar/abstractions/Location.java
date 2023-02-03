package ecdar.abstractions;

import ecdar.Ecdar;
import ecdar.code_analysis.Nearable;
import ecdar.controllers.EcdarController;
import ecdar.presentations.DropDownMenu;
import ecdar.utility.colors.Color;
import ecdar.utility.colors.EnabledColor;
import ecdar.utility.helpers.Circular;
import ecdar.utility.helpers.StringHelper;
import ecdar.utility.serialize.Serializable;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.beans.property.*;

import java.util.Collections;

public class Location implements Circular, Serializable, Nearable, DropDownMenu.HasColor {
    private static final String NICKNAME = "nickname";
    private static final String ID = "id";
    private static final String INVARIANT = "invariant";
    private static final String TYPE = "type";
    private static final String URGENCY = "urgency";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String COLOR = "color";
    private static final String NICKNAME_X = "nicknameX";
    private static final String NICKNAME_Y = "nicknameY";
    private static final String INVARIANT_X = "invariantX";
    private static final String INVARIANT_Y = "invariantY";
    private static final String UNI = "U";
    private static final String INC = "I";
    public static final String LOCATION = "L";
    static final int ID_LETTER_LENGTH = 1;

    // Verification properties
    private final StringProperty nickname = new SimpleStringProperty("");
    private final StringProperty id = new SimpleStringProperty("");
    private final StringProperty invariant = new SimpleStringProperty("");
    private final ObjectProperty<Type> type = new SimpleObjectProperty<>(Type.NORMAL);
    private final ObjectProperty<Urgency> urgency = new SimpleObjectProperty<>(Urgency.NORMAL);

    // Styling properties
    private final DoubleProperty x = new SimpleDoubleProperty(0d);
    private final DoubleProperty y = new SimpleDoubleProperty(0d);
    private final DoubleProperty radius = new SimpleDoubleProperty(0d);
    private final SimpleDoubleProperty scale = new SimpleDoubleProperty(1d);
    private final ObjectProperty<Color> color = new SimpleObjectProperty<>(Color.GREY_BLUE);
    private final ObjectProperty<Color.Intensity> colorIntensity = new SimpleObjectProperty<>(Color.Intensity.I700);

    private final DoubleProperty nicknameX = new SimpleDoubleProperty(0d);
    private final DoubleProperty nicknameY = new SimpleDoubleProperty(0d);
    private final DoubleProperty invariantX = new SimpleDoubleProperty(0d);
    private final DoubleProperty invariantY = new SimpleDoubleProperty(0d);

    private final ObjectProperty<Reachability> reachability = new SimpleObjectProperty<>();

    private final SimpleBooleanProperty isLocked = new SimpleBooleanProperty(false);
    private final BooleanProperty failing = new SimpleBooleanProperty(false);

    public Location() {
    }

    public Location(final String id) {
        setId(id);
    }

    public Location(final Component component, final Type type, final double x, final double y){
        setX(x);
        setY(y);
        setType(type);
        if(type == Type.UNIVERSAL){
            setIsLocked(true);
            setId(UNI + component.generateUniIncId());
        } else if (type == Type.INCONSISTENT) {
            setIsLocked(true);
            setUrgency(Location.Urgency.URGENT);
            setId(INC + component.generateUniIncId());
        }
        setColorIntensity(component.getColorIntensity());
        setColor(component.getColor());
    }

    public Location(final JsonObject jsonObject) {
        deserialize(jsonObject);
    }

    /**
     * Generates an id for this, and binds reachability analysis.
     */
    public void initialize() {
        setId();
    }

    /**
     * Creates a clone of another location.
     * Copies objects used for verification.
     * The id of the original is used for this one.
     * Reachability analysis is not initialized.
     * @return the clone
     */
    Location cloneForVerification() {
        final Location location = new Location();

        location.setId(getId());
        location.setType(getType());
        location.setUrgency(getUrgency());
        location.setInvariant(getInvariant());

        location.setX(getX());
        location.setY(getY());
        location.setNicknameX(getNicknameX());
        location.setNicknameY(getNicknameY());
        location.setInvariantX(getInvariantX());
        location.setInvariantY(getInvariantY());

        return location;
    }

    public String getNickname() {
        return nickname.get();
    }

    public void setNickname(final String nickname) {
        this.nickname.set(nickname);
    }

    public StringProperty nicknameProperty() {
        return nickname;
    }

    public String getId() {
        return id.get();
    }

    /**
     * Generate and sets a unique id for this location
     */
    private void setId() {
        for(int counter = 0; ; counter++) {
            if(!Ecdar.getProject().getLocationIds().contains(String.valueOf(counter))){
                id.set(LOCATION + counter);
                return;
            }
        }
    }

    /**
     * Sets a specific id for this location
     * @param string id to set
     */
    public void setId(final String string){
        id.set(string);
    }

    public StringProperty idProperty() {
        return id;
    }

    public String getInvariant() {
        return StringHelper.ConvertUnicodeToSymbols(invariant.get());
    }

    public void setInvariant(final String invariant) {
        this.invariant.set(invariant);
    }

    public StringProperty invariantProperty() {
        return invariant;
    }

    public Type getType() {
        return type.get();
    }

    public void setType(final Type type) {
        this.type.set(type);
    }

    public ObjectProperty<Type> typeProperty() {
        return type;
    }

    public Urgency getUrgency() {
        return urgency.get();
    }

    public void setUrgency(final Urgency urgency) {
        // If there is no EcdarPresentation, we are running tests and EcdarController calls will fail
        this.urgency.set(urgency);
    }

    public ObjectProperty<Urgency> urgencyProperty() {
        return urgency;
    }

    public double getX() {
        return x.get();
    }

    public void setX(final double x) {
        this.x.set(x);
    }

    public DoubleProperty xProperty() {
        return x;
    }

    public double getY() {
        return y.get();
    }

    public void setY(final double y) {
        this.y.set(y);
    }

    public DoubleProperty yProperty() {
        return y;
    }

    public Color getColor() {
        return color.get();
    }

    public void setColor(final Color color) {
        this.color.set(color);
    }

    public ObjectProperty<Color> colorProperty() {
        return color;
    }

    public Color.Intensity getColorIntensity() {
        return colorIntensity.get();
    }

    public void setColorIntensity(final Color.Intensity colorIntensity) {
        this.colorIntensity.set(colorIntensity);
    }

    public ObjectProperty<Color.Intensity> colorIntensityProperty() {
        return colorIntensity;
    }

    public double getRadius() {
        return radius.get();
    }

    public void setRadius(final double radius) {
        this.radius.set(radius);
    }

    @Override
    public DoubleProperty radiusProperty() {
        return radius;
    }

    @Override
    public DoubleProperty scaleProperty() {
        return scale;
    }

    public double getNicknameX() {
        return nicknameX.get();
    }

    public void setNicknameX(final double nicknameX) {
        this.nicknameX.set(nicknameX);
    }

    public DoubleProperty nicknameXProperty() {
        return nicknameX;
    }

    public double getNicknameY() {
        return nicknameY.get();
    }

    public void setNicknameY(final double nicknameY) {
        this.nicknameY.set(nicknameY);
    }

    public DoubleProperty nicknameYProperty() {
        return nicknameY;
    }

    public double getInvariantX() {
        return invariantX.get();
    }

    public void setInvariantX(final double invariantX) {
        this.invariantX.set(invariantX);
    }

    public DoubleProperty invariantXProperty() {
        return invariantX;
    }

    public double getInvariantY() {
        return invariantY.get();
    }

    public void setInvariantY(final double invariantY) {
        // If there is no EcdarPresentation, we are running tests and EcdarController calls will fail
        this.invariantY.set(invariantY);
    }

    public DoubleProperty invariantYProperty() {
        return invariantY;
    }

    public String getMostDescriptiveIdentifier() {
        if(!Strings.isNullOrEmpty(getNickname())) {
            return getNickname();
        } else {
            return getId();
        }
    }

    /**
     * Adds an edge to the left side of a location
     * @param syncString the string of the channel to synchronize over
     * @param status the status of the edge
     * @return the finished edge
     */
    public Edge addLeftEdge(final String syncString, final EdgeStatus status) {
        final Edge edge = new Edge(this, status);
        edge.setTargetLocation(this);
        edge.setProperty(Edge.PropertyType.SYNCHRONIZATION, Collections.singletonList(syncString));
        final Nail inputNail1;
        final Nail inputNailSync;
        final Nail inputNail2;
        inputNail1 = new Nail(getX() - 40, getY() - 10);
        inputNailSync = new Nail(getX() - 60, getY());
        inputNail2 = new Nail(getX() - 40, getY() + 10);
        inputNailSync.setPropertyType(Edge.PropertyType.SYNCHRONIZATION);
        edge.addNail(inputNail1);
        edge.addNail(inputNailSync);
        edge.addNail(inputNail2);
        return edge;
    }

    /**
     * Adds an edge to the right side of a location
     * @param syncString the string of the channel to synchronize over
     * @param status the status of the edge
     * @return the finished edge
     */
    public Edge addRightEdge(final String syncString, final EdgeStatus status){
        final Edge edge = new Edge(this, status);
        edge.setTargetLocation(this);
        edge.setProperty(Edge.PropertyType.SYNCHRONIZATION, Collections.singletonList(syncString));
        final Nail inputNail1;
        final Nail inputNailSync;
        final Nail inputNail2;
        inputNail1 = new Nail(getX() + 40, getY() - 10);
        inputNailSync = new Nail(getX() + 60, getY());
        inputNail2 = new Nail(getX() + 40, getY() + 10);
        inputNailSync.setPropertyType(Edge.PropertyType.SYNCHRONIZATION);
        edge.addNail(inputNail1);
        edge.addNail(inputNailSync);
        edge.addNail(inputNail2);
        return edge;
    }

    public Reachability getReachability() {
        return reachability.get();
    }

    public ObjectProperty<Reachability> reachabilityProperty() {
        return reachability;
    }

    public void setReachability(final Reachability reachability) {
        this.reachability.set(reachability);
    }

    /**
     * Gets whether the location is locked
     * @return true if it is locked, false if not
     */
    public SimpleBooleanProperty getIsLocked() {return isLocked;}

    /**
     * Sets whether this location is locked
     * @param bool the value that isLocked is set to, true if the location is meant to be locked, false if it not
     */
    public void setIsLocked(final boolean bool) {isLocked.setValue(bool); }

    @Override
    public JsonObject serialize() {
        final JsonObject result = new JsonObject();
        result.addProperty(ID, getId());
        result.addProperty(NICKNAME, getNickname());
        result.addProperty(INVARIANT, getInvariant());
        result.add(TYPE, new Gson().toJsonTree(getType(), Type.class));
        result.add(URGENCY, new Gson().toJsonTree(getUrgency(), Urgency.class));

        result.addProperty(X, getX());
        result.addProperty(Y, getY());
        result.addProperty(COLOR, EnabledColor.getIdentifier(getColor()));

        result.addProperty(NICKNAME_X, getNicknameX());
        result.addProperty(NICKNAME_Y, getNicknameY());
        result.addProperty(INVARIANT_X, getInvariantX());
        result.addProperty(INVARIANT_Y, getInvariantY());

        return result;
    }

    @Override
    public void deserialize(final JsonObject json) {
        setId(json.getAsJsonPrimitive(ID).getAsString());
        setNickname(json.getAsJsonPrimitive(NICKNAME).getAsString());
        setInvariant(json.getAsJsonPrimitive(INVARIANT).getAsString());
        setType(new Gson().fromJson(json.getAsJsonPrimitive(TYPE), Type.class));
        setUrgency(new Gson().fromJson(json.getAsJsonPrimitive(URGENCY), Urgency.class));

        setX(json.getAsJsonPrimitive(X).getAsDouble());
        setY(json.getAsJsonPrimitive(Y).getAsDouble());

        final EnabledColor enabledColor = (json.has(COLOR) ? EnabledColor.fromIdentifier(json.getAsJsonPrimitive(COLOR).getAsString()) : null);
        if (enabledColor != null) {
            setColorIntensity(enabledColor.intensity);
            setColor(enabledColor.color);
        }

        if(json.has(NICKNAME_X) && json.has(NICKNAME_Y)) {
            setNicknameX(json.getAsJsonPrimitive(NICKNAME_X).getAsDouble());
            setNicknameY(json.getAsJsonPrimitive(NICKNAME_Y).getAsDouble());
        } else {
            setNicknameX(0);
            setNicknameY(20);
        }

        if(json.has(INVARIANT_X) && json.has(INVARIANT_Y)) {
            setInvariantX(json.getAsJsonPrimitive(INVARIANT_X).getAsDouble());
            setInvariantY(json.getAsJsonPrimitive(INVARIANT_Y).getAsDouble());
        } else {
            setInvariantX(0);
            setInvariantY(40);
        }
    }

    @Override
    public String generateNearString() {
        return "Location " + (!Strings.isNullOrEmpty(getNickname()) ? (getNickname() + " (" + getId() + ")") : getId());
    }

    /**
     * Sets whether this location failed for the last query
     * @param bool if a query responded failure with the location, bool should be true.
     */
    public void setFailing(boolean bool) {
        this.failing.set(bool);
    }

    /**
     * Whether this location is currently failing.
     * @return Whether this location is currently failing.
     */
    public boolean getFailing() {
        return this.failing.get();
    }

    /**
     * The observable boolean property for 'failing' of this.
     * @return The observable boolean property for 'failing' of this.
     */
    public BooleanProperty failingProperty() {
        return this.failing;
    }

    public enum Type {
        NORMAL, INITIAL, UNIVERSAL, INCONSISTENT
    }

    public enum Urgency {
        NORMAL, URGENT, COMMITTED, PROHIBITED
    }

    public enum Reachability {
        REACHABLE, UNREACHABLE, UNKNOWN, EXCLUDED
    }

    public boolean isUniversalOrInconsistent() {
        return getType().equals(Type.UNIVERSAL) || getType().equals(Type.INCONSISTENT);
    }
}