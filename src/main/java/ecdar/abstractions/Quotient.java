package ecdar.abstractions;

/**
 * Model of a Quotient operator, extends ComponentOperator
 */
public class Quotient extends ComponentOperator {
    /**
     * Constructor.
     * @param system system containing the operator
     */
    public Quotient(final EcdarSystem system) {
        super(system);
        label.setValue("A\\\\B");
    }
}
