package ecdar.mutation.models;

import ecdar.abstractions.Component;
import ecdar.mutation.MutationTestingException;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An operator for creating mutants of a component.
 */
public abstract class MutationOperator {
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
    public abstract String getText();

    abstract String getJsonName();

    /**
     * Generates mutants.
     * @param original the component to mutate
     * @return the generated mutants
     * @throws MutationTestingException if a mutation error happens
     */
    public abstract Collection<? extends Component> generate(final Component original) throws MutationTestingException;

    /**
     * Gets all available mutation operators.
     * @return the operators
     */
    static List<MutationOperator> getAllOperators() {
        final List<MutationOperator> operators = new ArrayList<>();

        operators.add(new ChangeSourceOperator());
        operators.add(new ChangeTargetOperator());
        operators.add(new ChangeGuardOperator());
        operators.add(new InvertResetOperator());
        operators.add(new SinkLocationOperator());

        return operators;
    }
}
