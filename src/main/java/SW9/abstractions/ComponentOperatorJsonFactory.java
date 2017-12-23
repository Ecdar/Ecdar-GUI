package SW9.abstractions;

public class ComponentOperatorJsonFactory {
    private final static String COMPOSITION_TYPE = "composition";
    private final static String CONJUNCTION_TYPE = "conjunction";
    private final static String QUOTIENT_TYPE = "quotient";

    private final static String TYPE_NOT_UNDERSTOOD_ERROR = "Type of operator not understood.";


    public static String getJsonType(final ComponentOperator operator) {
        if (operator instanceof Composition) return COMPOSITION_TYPE;
        if (operator instanceof Conjunction) return CONJUNCTION_TYPE;
        if (operator instanceof Quotient) return QUOTIENT_TYPE;

        throw new IllegalStateException(TYPE_NOT_UNDERSTOOD_ERROR);
    }

    public static ComponentOperator create(final String type, final EcdarSystem system) {
        if (type.equals(COMPOSITION_TYPE)) return new Composition(system);
        if (type.equals(CONJUNCTION_TYPE)) return new Conjunction(system);
        if (type.equals(QUOTIENT_TYPE)) return new Quotient(system);

        throw new IllegalStateException(TYPE_NOT_UNDERSTOOD_ERROR);
    }
}
