package ecdar.mutation;

import ecdar.mutation.models.AsyncInputReader;
import ecdar.mutation.models.MutationTestPlan;

import java.io.IOException;

public class SimulatedTimeHandler extends MutationTestingTimeHandler {
    private final SutWriter writer;
    private final AsyncInputReader reader;


    /**
     * The simulated time (in time units) that have been delayed since the last delay rule.
     */
    private int delayTime;

    /**
     * The simulated time (in time units).
     */
    private int lastTime;

    public SimulatedTimeHandler(final MutationTestPlan plan, final SutWriter writer, final AsyncInputReader reader) {
        super(plan);
        this.writer = writer;
        this.reader = reader;
    }

    @Override
    void sleep() throws IOException, MutationTestingException, InterruptedException {
        writer.writeToSut("Delay: " + getPlan().getTimeUnit());
        delayTime ++;
        lastTime ++;
        reader.waitAndConsume("Step done");
    }

    @Override
    void onNewDelayRule() {
        delayTime = 0;
    }

    @Override
    void onTestStart() {
        delayTime = 0;
        lastTime = 0;
    }

    @Override
    boolean isMaxWaitTimeExceeded() {
        return delayTime > getPlan().getOutputWaitTime();
    }

    @Override
    double getTimeSinceLastTime() {
        final int result = lastTime;
        lastTime = 0;
        return  result;
    }
}
