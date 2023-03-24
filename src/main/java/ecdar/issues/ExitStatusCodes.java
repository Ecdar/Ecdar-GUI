package ecdar.issues;

/**
 * Enum for representing the status of a requested exit
 */
public enum ExitStatusCodes {
    SHUTDOWN_SUCCESSFUL(0),
    GRACEFUL_SHUTDOWN_FAILED(-1),
    CLOSE_ENGINE_CONNECTIONS_FAILED(-2);

    private final int statusCode;
    ExitStatusCodes(int statusCode) { this.statusCode = statusCode; }

    public int getStatusCode() {
        return statusCode;
    }
}