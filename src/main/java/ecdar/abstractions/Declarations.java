package ecdar.abstractions;

import ecdar.utility.colors.Color;
import com.google.gson.JsonObject;
import ecdar.utility.colors.EnabledColor;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Overall declarations of a model.
 * This could be global declarations or system declarations.
 */
public class Declarations extends HighLevelModel {
    private final StringProperty declarationsText = new SimpleStringProperty("");

    /**
     * Constructor with a name.
     * @param name name of the declarations
     */
    public Declarations(final String name) {
        setName(name);
        setColor(EnabledColor.getDefault());
    }


    public Declarations(final JsonObject json) {
        deserialize(json);
        setColor(EnabledColor.getDefault());
    }

    public String getDeclarationsText() {
        return declarationsText.get();
    }

    public void setDeclarationsText(final String declarationsText) {
        this.declarationsText.set(declarationsText);
    }

    @Override
    public JsonObject serialize() {
        final JsonObject result = super.serialize();

        result.addProperty(DECLARATIONS, getDeclarationsText());

        return result;
    }

    @Override
    public void deserialize(final JsonObject json) {
        super.deserialize(json);

        setDeclarationsText(json.getAsJsonPrimitive(DECLARATIONS).getAsString());
    }

    public void clearDeclarationsText() {
        setDeclarationsText("");
    }

    /**
     * Gets the custom types defined with typedef of a range of ints.
     * @return Triples containing (left) name of the type, (middle) lower bound, (right) upper bound
     */
    public List<Triple<String, Integer, Integer>> getTypedefs() {
        final List<Triple<String, Integer, Integer>> types = new ArrayList<>();

        final Matcher matcher = Pattern.compile(".*typedef\\s+int\\s*\\[(\\d+)\\s*,\\s*(\\d+)]\\s*(\\w*)\\s*;.*").matcher(getDeclarationsText());

        while (matcher.find()) {
            types.add(Triple.of(matcher.group(3), Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))));
        }

        return types;
    }
}
