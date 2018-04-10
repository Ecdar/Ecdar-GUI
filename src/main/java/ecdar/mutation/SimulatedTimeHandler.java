package ecdar.mutation;

import ecdar.mutation.models.AsyncInputReader;
import ecdar.mutation.models.MutationTestPlan;

import java.io.IOException;

/**
 * Handler for simulating time when testing.
 * When the system under test should simulate delay,
 * this writes "Delay: n", where n is an integer for the time in ms to simulate delay.
 * The system under test should respond with "Delay done" when it has finished simulating the delay.
 */
public class SimulatedTimeHandler extends MutationTestTimeHandler {
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

    /**
     * Constructs.
     * @param plan the test plan to test based of
     * @param writer a writer for writing to the system under test
     * @param reader the reader for reading inputs from the system under test
     */
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
        reader.waitAndConsume("Delay done");
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
