package ecdar.mutation;

import ecdar.abstractions.Component;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An operator for creating mutants of a component.
 */
abstract class MutationOperator {
    private final BooleanProperty selected = new SimpleBooleanProperty(true);

    public boolean isSelected() {
        return selected.get();
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public void setSelected(final boolean selected) {
        this.selected.set(selected);
    }

    /**
     * Gets a readable text to display.
     * @return the text
     */
    abstract String getText();

    abstract String getJsonName();

    abstract Collection<? extends Component> compute(final Component testModel);

    static List<MutationOperator> getAllOperators() {
        final List<MutationOperator> operators = new ArrayList<>();

        operators.add(new ChangeSourceOperator());
        operators.add(new ChangeTargetOperator());

        return operators;
    }
}
