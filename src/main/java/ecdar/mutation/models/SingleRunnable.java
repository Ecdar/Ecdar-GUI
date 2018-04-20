package ecdar.mutation.models;

/**
 * An object that only runs a runnable a single time.
 * If running more than once, the subsequent runs does nothing.
 */
public class SingleRunnable implements Runnable {
    private boolean isRun = false;
    private final Runnable runnable;

    /**
     * Constructs this.
     * @param runnable the runnable to run only once
     */
    public SingleRunnable(final Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public synchronized void run() {
        if (isRun) return;
        isRun = true;

        runnable.run();
    }
}
