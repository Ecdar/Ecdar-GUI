package ecdar.mutation;

import ecdar.mutation.models.MutationTestPlan;

public class SimulatedTimeHandler extends MutationTestingTimeHandler {
    public SimulatedTimeHandler(MutationTestPlan plan) {
        super(plan);
    }

    @Override
    void sleep() {

    }

    @Override
    void onNewDelayRule() {

    }

    @Override
    void onTestStart() {

    }

    @Override
    boolean isMaxWaitTimeExceeded() {
        return false;
    }

    @Override
    double getTimeSinceLastTime() {
        return 0;
    }
}
