package SW9.abstractions;

/**
 * Model of a Conjunction operator, extends ComponentOperator
 */
public class Conjunction extends ComponentOperator {
    /**
     * Constructor.
     * @param system system containing the operator
     */
    public Conjunction(final EcdarSystem system) {
        super(system);
        label.setValue("&&");
    }
}
