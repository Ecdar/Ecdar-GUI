package SW9.abstractions;

import SW9.Ecdar;
import SW9.code_analysis.Nearable;
import SW9.controllers.EcdarController;
import SW9.presentations.DropDownMenu;
import SW9.presentations.Grid;
import SW9.utility.colors.Color;
import SW9.utility.colors.EnabledColor;
import SW9.utility.helpers.Circular;
import SW9.utility.serialize.Serializable;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.beans.property.*;

public class Location implements Circular, Serializable, Nearable, DropDownMenu.HasColor {
    private static final String NICKNAME = "nickname";
    private static final String ID = "id";
    private static final String INVARIANT = "invariant";
    private static final String TYPE = "type";
    private static final String URGENCY = "urgency";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String COLOR = "color";
    private static final String NICKNAME_X = "nickname_x";
    private static final String NICKNAME_Y = "nickname_y";
    private static final String INVARIANT_X = "invariant_x";
    private static final String INVARIANT_Y = "invariant_y";
    private static final String UNI = "U";
    private static final String INC = "I";
    public static final String LOCATION = "L";
    static final int ID_LENGTH = 1;

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

    public Location() {
        setId();
        bindReachabilityAnalysis();
    }

    public Location(final String id) {
        setId(id);
        bindReachabilityAnalysis();
    }

    public Location(final Component component, final Type type, final double x, final double y){
        setX(Grid.snap(x));
        setY(Grid.snap(y));
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
        bindReachabilityAnalysis();
    }

    public String getNickname() {
        return nickname.get();
    }

    private void setNickname(final String nickname) {
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
    private void setId(final String string){
        id.set(string);
    }

    public StringProperty idProperty() {
        return id;
    }

    public String getInvariant() {
        return invariant.get();
    }

    private void setInvariant(final String invariant) {
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
        EcdarController.runReachabilityAnalysis();
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

    private void setNicknameX(final double nicknameX) {
        this.nicknameX.set(nicknameX);
    }

    public DoubleProperty nicknameXProperty() {
        return nicknameX;
    }

    public double getNicknameY() {
        return nicknameY.get();
    }

    private void setNicknameY(final double nicknameY) {
        this.nicknameY.set(nicknameY);
    }

    public DoubleProperty nicknameYProperty() {
        return nicknameY;
    }

    public double getInvariantX() {
        return invariantX.get();
    }

    private void setInvariantX(final double invariantX) {
        this.invariantX.set(invariantX);
    }

    public DoubleProperty invariantXProperty() {
        return invariantX;
    }

    public double getInvariantY() {
        return invariantY.get();
    }

    private void setInvariantY(final double invariantY) {
        EcdarController.runReachabilityAnalysis();
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
    public Edge addLeftEdge(final String syncString ,final EdgeStatus status) {
        final Edge edge = new Edge(this, status);
        edge.setTargetLocation(this);
        edge.setProperty(Edge.PropertyType.SYNCHRONIZATION, syncString);
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
    public Edge addRightEdge(final String syncString ,final EdgeStatus status){
        final Edge edge = new Edge(this, status);
        edge.setTargetLocation(this);
        edge.setProperty(Edge.PropertyType.SYNCHRONIZATION, syncString);
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
    private void setIsLocked(final boolean bool) {isLocked.setValue(bool); }

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

        final EnabledColor enabledColor = EnabledColor.fromIdentifier(json.getAsJsonPrimitive(COLOR).getAsString());
        if (enabledColor != null) {
            setColorIntensity(enabledColor.intensity);
            setColor(enabledColor.color);
        }

        setNicknameX(json.getAsJsonPrimitive(NICKNAME_X).getAsDouble());
        setNicknameY(json.getAsJsonPrimitive(NICKNAME_Y).getAsDouble());
        setInvariantX(json.getAsJsonPrimitive(INVARIANT_X).getAsDouble());
        setInvariantY(json.getAsJsonPrimitive(INVARIANT_Y).getAsDouble());
    }

    @Override
    public String generateNearString() {
        return "Location " + (!Strings.isNullOrEmpty(getNickname()) ? (getNickname() + " (" + getId() + ")") : getId());
    }
    public enum Type {
        NORMAL, INITIAL, UNIVERSAL, INCONSISTENT
    }

    public enum Urgency {
        NORMAL, URGENT, COMMITTED
    }

    public enum Reachability {
        REACHABLE, UNREACHABLE, UNKNOWN, EXCLUDED
    }

    private void bindReachabilityAnalysis() {
        invariantProperty().addListener((observable, oldValue, newValue) -> EcdarController.runReachabilityAnalysis());
        urgencyProperty().addListener((observable, oldValue, newValue) -> EcdarController.runReachabilityAnalysis());
    }
}