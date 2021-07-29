package ecdar.abstractions;

public enum QueryType {
    REACHABILITY("reachability", "E<>"),
    REFINEMENT("refinement", "<="),
    QUOTIENT("quotient", "\\"),
    SPECIFICATION("specification", "Spec"),
    IMPLEMENTATION("implementation", "Imp"),
    LOCAL_CONSISTENCY("local-consistency", "lCon"),
    GLOBAL_CONSISTENCY("global-consistency", "gCon"),
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

    public static QueryType fromString(String s) {
        for (QueryType type : QueryType.values()) {
            if (type.queryName.equals(s.toLowerCase()) || type.symbol.equals(s.toLowerCase())) {
                return type;
            }
        }

        return null;
    }
}
