package ecdar.abstractions;

public enum QueryType {
    REACHABILITY("reachability", "E<>"),
    REFINEMENT("refinement", "<="),
    SPECIFICATION("specification", "Spec"),
    IMPLEMENTATION("implementation", "Imp"),
    CONSISTENCY("consistency", "Con"),
    LOCAL_CONSISTENCY("local-consistency", "LCon"),
    BISIM_MINIM("bisim-minim", "Bsim"),
    GET_COMPONENT("get-component", "Get");

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
            case "reachability":
                return REACHABILITY;
            case "refinement":
                return REFINEMENT;
            case "specification":
                return SPECIFICATION;
            case "implementation":
                return IMPLEMENTATION;
            case "consistency":
                return CONSISTENCY;
            case "local-consistency":
                return LOCAL_CONSISTENCY;
            case "bisim-minim":
                return BISIM_MINIM;
            case "get":
            case "get-component":
                return GET_COMPONENT;
            default:
                return null;
        }
    }
}
