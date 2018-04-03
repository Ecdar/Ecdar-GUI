package ecdar.mutation;

import ecdar.mutation.models.AsyncInputReader;
import ecdar.mutation.models.MutationTestPlan;

import java.io.IOException;

public abstract class MutationTestingTimeHandler {
    private final MutationTestPlan plan;

    public MutationTestingTimeHandler(final MutationTestPlan plan) {
        this.plan = plan;
    }

    public MutationTestPlan getPlan() {
        return plan;
    }

    abstract void sleep() throws InterruptedException, IOException, MutationTestingException;
    abstract void onNewDelayRule();
    abstract void onTestStart();
    abstract boolean isMaxWaitTimeExceeded();

    /**
     * Gets the duration since last time this method was called.
     * @return the duration in time units
     */
    abstract double getTimeSinceLastTime();

    public static MutationTestingTimeHandler getHandler(final MutationTestPlan plan, final SutWriter writer, final AsyncInputReader reader) {
        if (plan.isSimulateTime()) return new SimulatedTimeHandler(plan, writer, reader);
        else return new RealTimeHandler(plan);
    }
}
