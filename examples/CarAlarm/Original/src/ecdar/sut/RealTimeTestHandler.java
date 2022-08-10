package ecdar.sut;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;

public class RealTimeTestHandler extends TestHandler {

    RealTimeTestHandler(double timeUnit) {
        super(timeUnit);
    }

    @Override
    public void start(Runner stepStarter) throws IOException, InterruptedException {
        stepStarter.run();
    }

    @Override
    public void onStepDone(Runner startNewStep) throws InterruptedException, IOException {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    startNewStep.run();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, (long) timeUnit / 4);
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
