package ecdar.backend;

public class BackendException extends Exception {
    public BackendException(final String message) {
        super(message);
    }

    public BackendException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public static class BadBackendQueryException extends BackendException {
        public BadBackendQueryException(final String s) {
            super(s);
        }

        public BadBackendQueryException(final String s, final Exception cause) {
            super(s, cause);
        }
    }

    public static class MissingFileQueryException extends BackendException {
        public MissingFileQueryException(final String s) {
            super(s);
        }

        public MissingFileQueryException(final String s, final Exception cause) {
            super(s, cause);
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

    public class QueryUncertainException extends BackendException {
        public QueryUncertainException(final String s) {
            super(s);
        }

        public QueryUncertainException(final String s, final Exception cause) {
            super(s, cause);
        }
    }
}
