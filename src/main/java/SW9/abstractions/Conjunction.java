package SW9.abstractions;

/**
 * Model of a Conjunction operator, extends ComponentOperator
 */
public class Conjunction extends ComponentOperator {
    public Conjunction(final EcdarSystem system) {
        super(system);
        label.setValue("||");
    }

    @Override
    public String getJsonType() {
        return "conjunction";
    }
}
