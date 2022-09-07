package ecdar.backend;

public class BackendException extends Exception {
    public BackendException(final String message) {
        super(message);
    }

    public BackendException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public static class NoAvailableBackendConnectionException extends BackendException {
        public NoAvailableBackendConnectionException(final String message) {
            super(message);
        }

        public NoAvailableBackendConnectionException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    public static class BadBackendQueryException extends BackendException {
        public BadBackendQueryException(final String message) {
            super(message);
        }

        public BadBackendQueryException(final String message, final Exception cause) {
            super(message, cause);
        }
    }

    public static class MissingFileQueryException extends BackendException {
        public MissingFileQueryException(final String s) {
            super(s);
        }

        public MissingFileQueryException(final String message, final Exception cause) {
            super(message, cause);
        }
    }

    public static class QueryErrorException extends BackendException {

        public QueryErrorException(final String message) {
            super(message);
        }

        public QueryErrorException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    public static class QueryUncertainException extends BackendException {
        public QueryUncertainException(final String s) {
            super(s);
        }

        public QueryUncertainException(final String message, final Exception cause) {
            super(message, cause);
        }
    }
}
