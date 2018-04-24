package ecdar.mutation;

import ecdar.mutation.models.AsyncInputReader;
import ecdar.mutation.models.MutationTestPlan;
import ecdar.mutation.models.SingleRunnable;

import java.util.function.Consumer;

/**
 * Handler for handling time during a mutation test.
 */
public abstract class MutationTestTimeHandler {
    private final MutationTestPlan plan;
    private final AsyncInputReader reader;
    private final Consumer<Exception> exceptionConsumer;

    /**
     * Constructs this.
     * @param plan the test plan to base the test of
     * @param exceptionConsumer consumer to be called if an exception happens
     * @param reader the reader for reading inputs from the system under test
     */
    public MutationTestTimeHandler(final MutationTestPlan plan, final AsyncInputReader reader, final Consumer<Exception> exceptionConsumer) {
        this.plan = plan;
        this.reader = reader;
        this.exceptionConsumer = exceptionConsumer;
    }


    /* Properties */

    public MutationTestPlan getPlan() {
        return plan;
    }

    public AsyncInputReader getReader() {
        return reader;
    }

    public Consumer<Exception> getExceptionConsumer() {
        return exceptionConsumer;
    }

    /* Other */

    /**
     * Sleeps for some time.
     * @param listener Listener to be called when done sleeping
     */
    abstract void sleep(final SingleRunnable listener);

    /**
     * Handles what to do, when the test switches to another delay rule.
     */
    abstract void onNewDelayRule();

    /**
     * Handles what to do, when the test starts.
     */
    abstract void onTestStart();

    /**
     * The system under test should output within some maximum waiting time.
     * Gets if this time is exceeded.
     * @return true iff the time is exceeded
     */
    abstract boolean isMaxWaitTimeExceeded();

    /**
     * Gets the duration since last time this method was called.
     * @return the duration in time units
     */
    abstract double getTimeSinceLastTime();
}
