package ecdar.backend;

import ecdar.abstractions.QueryState;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public abstract class QuotientBackendThread extends Thread {
    public AtomicBoolean hasBeenCanceled = new AtomicBoolean();
    final String query;
    final Consumer<Boolean> success;
    final Consumer<BackendException> failure;
    final QueryListener queryListener;

    public QuotientBackendThread(final String query,
                                final Consumer<Boolean> success,
                                final Consumer<BackendException> failure,
                                final QueryListener queryListener) {
        this.query = query;
        this.success = success;
        this.failure = failure;
        this.queryListener = queryListener;
    }

    void handleResult(QueryState result, String line) {
        if (result.getStatusCode() == QueryState.COMPONENT_GENERATED.getStatusCode()) {
            success.accept(true);
        } else if (result.getStatusCode() == QueryState.ERROR.getStatusCode()){
            success.accept(false);
        } else if (result.getStatusCode() == QueryState.SYNTAX_ERROR.getStatusCode()) {
            failure.accept(new BackendException.QueryErrorException(line));
        } else {
            failure.accept(new BackendException.BadBackendQueryException(line));
        }
    }
}
