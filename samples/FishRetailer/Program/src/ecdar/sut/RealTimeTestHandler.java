package ecdar.sut;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Scanner;

public class RealTimeTestHandler extends TestHandler {

    RealTimeTestHandler(double timeUnit) {
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
    public void start(Runner stepStarter) throws IOException, InterruptedException {
        stepStarter.run();
    }

    @Override
    public void onStepDone(Runner startNewStep) throws InterruptedException, IOException {
        Thread.sleep((long) timeUnit / 4);
        startNewStep.run();
    }

    @Override
    public double getValue(Instant clock) {
        return Duration.between(clock, Instant.now()).toMillis() / timeUnit;
    }

    @Override
    public Instant resetTime() {
        return Instant.now();
    }
}
