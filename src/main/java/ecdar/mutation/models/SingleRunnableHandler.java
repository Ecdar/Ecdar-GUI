package ecdar.mutation.models;

public class SingleRunnableHandler {
    private boolean isRun = false;

    public synchronized void run(final Runnable runnable) {
        if (isRun) return;
        isRun = true;

        runnable.run();
    }
}
