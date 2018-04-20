package ecdar.mutation.models;

public class SingleRunnable implements Runnable {
    private boolean isRun = false;
    private final Runnable runnable;

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
