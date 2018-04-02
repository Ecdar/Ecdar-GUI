package ecdar.mutation;

import ecdar.mutation.models.MutationTestPlan;

import java.time.Duration;
import java.time.Instant;

public class RealTimeHandler extends MutationTestingTimeHandler {
    private Instant delayStart, lastTime;

    public RealTimeHandler(final MutationTestPlan plan) {
        super(plan);
    }

    @Override
    void sleep() throws InterruptedException {
        Thread.sleep(getPlan().getTimeUnit() / 4);
    }

    @Override
    void onNewDelayRule() {
        delayStart = Instant.now();
    }

    @Override
    void onTestStart() {
        lastTime = Instant.now();
    }

    @Override
    boolean isMaxWaitTimeExceeded() {
        return Duration.between(delayStart, Instant.now()).toMillis() / (double)getPlan().getTimeUnit() > getPlan().getOutputWaitTime();
    }

    @Override
    double getTimeSinceLastTime() {
        final double waitedTimeUnits = Duration.between(lastTime, Instant.now()).toMillis() / (double) getPlan().getTimeUnit();
        lastTime = Instant.now();

        return waitedTimeUnits;
    }
}
