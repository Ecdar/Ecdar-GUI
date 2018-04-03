package ecdar.retailer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Scanner;

public class RealTimeTestHandler extends TestHandler {

    public RealTimeTestHandler(double timeUnit) {
        super(timeUnit);
    }

    @Override
    public boolean inputReady() throws IOException {
        return System.in.available() != 0;
    }

    @Override
    public String read() {
        return new Scanner(System.in).nextLine();
    }

    @Override
    public void onStepDone() throws InterruptedException {
        Thread.sleep((long) timeUnit / 4);
    }

    @Override
    public double getValue(Instant clock) {
        return Duration.between(clock, Instant.now()).toMillis() / timeUnit;
    }

    @Override
    Instant resetTime() {
        return Instant.now();
    }
}
