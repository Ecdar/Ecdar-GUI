package SW9.abstractions;

/**
 * Model of a Composition operator, extends ComponentOperator
 */
public class Composition extends ComponentOperator {
    public Composition(final EcdarSystem system) {
        super(system);
        label.setValue("&&");
    }

    @Override
    public String getJsonType() {
        return "composition";
    }
}
