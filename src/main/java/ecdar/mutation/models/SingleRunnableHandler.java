package ecdar.mutation.models;

/**
 * A handler to only run once.
 * Here you at run-time specifies the runnable to run.
 */
public class SingleRunnableHandler {
    private boolean isRun = false;

    /**
     * If this is the first call to this method in this instance, the runnable is run.
     * Otherwise, this method does nothing.
     * @param runnable the runnable
     */
    public synchronized void run(final Runnable runnable) {
        if (isRun) return;
        isRun = true;

        runnable.run();
    }
}
