package ecdar.issues;

import javafx.beans.Observable;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material.Material;

import java.util.function.Predicate;

public abstract class Issue<T extends Node> {

    private String message = null;
    private final BooleanBinding presentProperty;

    // Subclasses will override this, to provided us with the correct icon to use
    protected abstract Material getIcon();

    public Issue(final Predicate<T> presentPredicate, final T subject, final Observable... observables) {
        presentProperty = new BooleanBinding() {
            {
                // Bind to the provided observables (which may influence the "present" stringBinder
                super.bind(observables);
            }

            @Override
            protected boolean computeValue() {
                return presentPredicate.test(subject);
            }
        };
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public Boolean isPresent() {
        return presentProperty.get();
    }

    public ObservableBooleanValue presentPropertyProperty() {
        return presentProperty;
    }

    public FontIcon generateFontIconNode() {
        final FontIcon fontIcon = new FontIcon();

        final Tooltip tooltip = new Tooltip(message);
        Tooltip.install(fontIcon, tooltip);

        // Set the style of the icon
        fontIcon.setFill(Color.GRAY);
        fontIcon.iconCodeProperty().set(getIcon());

        // The icon should only be visible when it is present
        fontIcon.visibleProperty().bind(presentProperty);

        return fontIcon;
    }
}
