package SW9.abstractions;

/**
 * Model of a Composition operator, extends ComponentOperator
 */
public class Composition extends ComponentOperator {
    /**
     * Constructor.
     * @param system system containing the operator
     */
    public Composition(final EcdarSystem system) {
        super(system);
        label.setValue("||");
    }
}
