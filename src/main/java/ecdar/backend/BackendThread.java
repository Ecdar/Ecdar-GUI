package ecdar.backend;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public abstract class BackendThread extends Thread {
    public AtomicBoolean hasBeenCanceled = new AtomicBoolean();
    final String query;
    final Consumer<Boolean> success;
    final Consumer<BackendException> failure;
    final QueryListener queryListener;

    public BackendThread(final String query,
                         final Consumer<Boolean> success,
                         final Consumer<BackendException> failure,
                         final QueryListener queryListener) {
        this.query = query;
        this.success = success;
        this.failure = failure;
        this.queryListener = queryListener;
    }
}
