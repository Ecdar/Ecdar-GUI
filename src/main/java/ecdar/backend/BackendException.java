package ecdar.backend;

public class BackendException extends Exception {
    public BackendException(final String message) {
        super(message);
    }

    public BackendException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public static class NoAvailableEngineConnectionException extends BackendException {
        public NoAvailableEngineConnectionException(final String message) {
            super(message);
        }

        public NoAvailableEngineConnectionException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    public static class gRpcChannelShutdownException extends BackendException {
        public gRpcChannelShutdownException(final String message) {
            super(message);
        }

        public gRpcChannelShutdownException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    public static class EngineProcessDestructionException extends BackendException {
        public EngineProcessDestructionException(final String message) {
            super(message);
        }

        public EngineProcessDestructionException(final String message, final Throwable cause) {
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
