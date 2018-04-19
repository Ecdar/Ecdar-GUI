package ecdar.mutation;

import ecdar.mutation.models.AsyncInputReader;
import ecdar.mutation.models.MutationTestPlan;
import ecdar.mutation.models.SingleRunnable;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Handler for handling time during a mutation test.
 */
public abstract class MutationTestTimeHandler {
    private final MutationTestPlan plan;
    final Consumer<Exception> exceptionConsumer;

    /**
     * Constructs.
     * @param plan the test plan to base the test of
     * @param exceptionConsumer
     */
    public MutationTestTimeHandler(final MutationTestPlan plan, Consumer<Exception> exceptionConsumer) {
        this.plan = plan;
        this.exceptionConsumer = exceptionConsumer;
    }

    public MutationTestPlan getPlan() {
        return plan;
    }

    /**
     * Sleeps for some time.
     * @throws InterruptedException if an error occurs
     * @throws IOException if an error occurs
     * @throws MutationTestingException if an error occurs
     */
    abstract void sleep(final SingleRunnable runnable);

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

    /**
     * Gets a new time handler.
     * @param plan the test plan to test with
     * @param writer the writer for writing to the system under test
     * @param reader the reader for reading from the system under test
     * @return the new time handler
     */
    // TODO Make the if statement in caller instead, and remove this method
    public static MutationTestTimeHandler getHandler(final MutationTestPlan plan, final Consumer<Exception> exceptionConsumer, final SutWriter writer, final AsyncInputReader reader) {
        if (plan.isSimulateTime()) return new SimulatedTimeHandler(plan, exceptionConsumer, writer, reader);
        else return new RealTimeHandler(plan, exceptionConsumer, reader);
    }
}
