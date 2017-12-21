package SW9.abstractions;

public class ComponentOperatorJsonFactory {
    private final static String COMPOSITION_TYPE = "composition";
    private final static String CONJUNCTION_TYPE = "conjunction";
    private final static String QUOTIENT_TYPE = "quotient";


    public static String getJsonType(final ComponentOperator operator) {
        if (operator instanceof Composition) return COMPOSITION_TYPE;
        if (operator instanceof Conjunction) return CONJUNCTION_TYPE;
        if (operator instanceof Quotient) return QUOTIENT_TYPE;

        throw new IllegalStateException("Type of operator not understood.");
    }
}
