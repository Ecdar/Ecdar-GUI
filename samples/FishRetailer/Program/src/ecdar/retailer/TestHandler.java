package ecdar.retailer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

public abstract class TestHandler {
    final double timeUnit; // In ms

    protected TestHandler(double timeUnit) {
        this.timeUnit = timeUnit;
    }


    abstract boolean inputReady() throws IOException;

    abstract String read();

    abstract void onStepDone() throws InterruptedException;

    /**
     * In time units
     * @param clock
     * @return
     */
    abstract double getValue(final Instant clock);

    abstract Instant resetTime();


    public void write(String message) {
        System.out.println(message);
    }
}
