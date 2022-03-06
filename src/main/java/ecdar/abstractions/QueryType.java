package ecdar.abstractions;

public enum QueryType {
    REFINEMENT("refinement", "<="),
    QUOTIENT("quotient", "\\"),
    SPECIFICATION("specification", "Spec"),
    IMPLEMENTATION("implementation", "Imp"),
    LOCAL_CONSISTENCY("consistency", "lCon"), // ToDo NIELS: Will become local-consistency
    GLOBAL_CONSISTENCY("global-consistency", "gCon"),
    BISIM_MIN("bisim", "bsim"),
    GET_COMPONENT("get-component", "get"),
    REACHABILITY("reachability", "E<>");

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
        switch (s.toLowerCase()) {
            case "refinement":
                return REFINEMENT;
            case "quotient":
                return QUOTIENT;
            case "specification":
                return SPECIFICATION;
            case "implementation":
                return IMPLEMENTATION;
            case "consistency":
            case "local-consistency":
                return LOCAL_CONSISTENCY;
            case "global-consistency":
                return GLOBAL_CONSISTENCY;
            case "bisim":
                return BISIM_MIN;
            case "get":
            case "get-component":
                return GET_COMPONENT;
            case "reachability":
                return REACHABILITY;
            default:
                return null;
        }
    }
}
