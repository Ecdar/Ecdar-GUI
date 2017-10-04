package SW9.abstractions;

import SW9.utility.colors.Color;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * An object used for verifications.
 * This could be a component or a declarations object.
 */
public abstract class VerificationObject {
    private final StringProperty declarationsText;
    private final StringProperty name;
    private final ObjectProperty<Color> color;
    private final ObjectProperty<Color.Intensity> colorIntensity;

    VerificationObject() {
        declarationsText = new SimpleStringProperty("");
        name = new SimpleStringProperty("");
        color = new SimpleObjectProperty<>(Color.GREY_BLUE);
        colorIntensity = new SimpleObjectProperty<>(Color.Intensity.I700);
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

    public String getName() {
        return name.get();
    }

    public void setName(final String name) {
        this.name.unbind();
        this.name.set(name);
    }

    public StringProperty nameProperty() {
        return name;
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
}
