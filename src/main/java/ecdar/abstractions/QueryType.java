package ecdar.abstractions;

public enum QueryType {
    REFINEMENT("refinement", "<="),
    QUOTIENT("quotient", "\\"),
    SPECIFICATION("specification", "Spec"),
    IMPLEMENTATION("implementation", "Imp"),
    LOCAL_CONSISTENCY("local-consistency", "l-Con"),
    GLOBAL_CONSISTENCY("global-consistency", "g-Con"),
    BISIM_MIN("bisim", "bsim"),
    GET_NEW_COMPONENT("get-new-component", "get");

    private final String queryName;
    private final String symbol;

    QueryType(String queryName, String symbol){
        this.queryName = queryName;
        this.symbol = symbol;
    }

    public String getQueryName() {
        return queryName;
    }

    public String getSymbol() {
        return symbol;
    }
}
