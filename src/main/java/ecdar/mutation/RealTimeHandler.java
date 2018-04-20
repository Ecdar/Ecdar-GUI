package ecdar.mutation;

import ecdar.mutation.models.AsyncInputReader;
import ecdar.mutation.models.MutationTestPlan;
import ecdar.mutation.models.SingleRunnable;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * Handler for handling time during a mutation test.
 * This performs actions to make the test run in real-time.
 */
public class RealTimeHandler extends MutationTestTimeHandler {
    private Instant delayStart, lastTime;
    private final AsyncInputReader reader;

    /**
     * Constructs.
     * @param plan the test plan to test with
     */
    public RealTimeHandler(final MutationTestPlan plan,final Consumer<Exception> exceptionConsumer, final AsyncInputReader reader) {
        super(plan, exceptionConsumer);
        this.reader = reader;
    }

    @Override
    void sleep(final SingleRunnable runnable) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                runnable.run();

            }
        }, getPlan().getTimeUnit() / 2);

        reader.addTempListener(runnable);
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
