package ecdar.mutation;

/**
 * An operator for creating mutants of a compoennt
 */
abstract class MutationOperator {
    /**
     * Gets a readable text to display.
     * @return the text
     */
    abstract String getText();
}
