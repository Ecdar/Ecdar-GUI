package ecdar.mutation;

import ecdar.mutation.models.MutationTestPlan;

import java.time.Duration;
import java.time.Instant;

/**
 * Handler for handling time during a mutation test.
 * This performs actions to make the test run in real-time.
 */
public class RealTimeHandler extends MutationTestTimeHandler {
    private Instant delayStart, lastTime;

    /**
     * Constructs.
     * @param plan the test plan to test with
     */
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
