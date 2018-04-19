package ecdar.mutation;

import ecdar.mutation.models.AsyncInputReader;
import ecdar.mutation.models.MutationTestPlan;
import ecdar.mutation.models.SingleRunnable;

import java.io.IOException;
import java.util.function.Consumer;

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
    public SimulatedTimeHandler(final MutationTestPlan plan, final Consumer<Exception> exceptionConsumer, final SutWriter writer, final AsyncInputReader reader) {
        super(plan, exceptionConsumer);
        this.writer = writer;
        this.reader = reader;
    }

    @Override
    void sleep(final SingleRunnable runnable) {
        try {
            writer.writeToSut("Delay: 1"); // Tell SUT to delay 1 time unit
        } catch (IOException e) {
            exceptionConsumer.accept(e);
        }

        delayTime ++;
        lastTime ++;

        try {
            reader.waitAndConsume("Delay done"); // Wait until SUT said it has simulated delay
        } catch (InterruptedException | MutationTestingException e) {
            exceptionConsumer.accept(e);
        }

        runnable.run();
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
