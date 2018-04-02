package ecdar.mutation;

import ecdar.mutation.models.MutationTestPlan;

public abstract class MutationTestingTimeHandler {
    private final MutationTestPlan plan;

    public MutationTestingTimeHandler(final MutationTestPlan plan) {
        this.plan = plan;
    }

    public MutationTestPlan getPlan() {
        return plan;
    }

    abstract void sleep() throws InterruptedException;
    abstract void onNewDelayRule();
    abstract void onTestStart();
    abstract boolean isMaxWaitTimeExceeded();
    abstract double getTimeSinceLastTime();

    public static MutationTestingTimeHandler getHandler(final MutationTestPlan plan) {
        if (plan.isSimulateTime()) return new SimulatedTimeHandler(plan);
        else return new RealTimeHandler(plan);
    }
}
