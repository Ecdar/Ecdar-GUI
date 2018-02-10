package ecdar.mutation;

import ecdar.abstractions.Component;

import java.util.Collection;

/**
 * An operator for creating mutants of a compoennt
 */
abstract class MutationOperator {
    /**
     * Gets a readable text to display.
     * @return the text
     */
    abstract String getText();

    public abstract Collection<? extends Component> compute(final Component testModel);
}
