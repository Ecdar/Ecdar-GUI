package ecdar.sut;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimulatedTimeTestHandler extends TestHandler {
    private Instant time;

    SimulatedTimeTestHandler(final double timeUnit) {
        super(timeUnit);

        time = Instant.now();
    }

    @Override
    public void start(final Runner stepStarter) throws IOException, InterruptedException {
        waitForDelay(stepStarter);
    }

    @Override
    public void onStepDone(final Runner startNewStep) throws IOException, InterruptedException {
        write("Delay done");

        waitForDelay(startNewStep);
    }

    private synchronized void waitForDelay(final Runner startNewStep) throws IOException, InterruptedException {
        if (linesBuffer.stream().noneMatch(this::isDelay)) {
            tempLinesListeners.add(() -> waitForDelay(startNewStep));
            return;
        }

        // Fetch delay
        String delayLine = linesBuffer.stream().filter(this::isDelay).findFirst().orElseThrow(() -> new RuntimeException("lines had no delays"));

        final Matcher matcher = Pattern.compile("Delay: (\\d+)").matcher(delayLine);

        if (matcher.find()) { // Simulate delay
            time = time.plus(Duration.ofMillis(Long.parseLong(matcher.group(1))));
            linesBuffer.removeIf(this::isDelay);
        } else throw new RuntimeException("Delay line is not a delay");

        startNewStep.run();
    }

    private boolean isDelay(final String input) {
        final Matcher matcher = Pattern.compile("Delay: (\\d+)").matcher(input);

        return matcher.find();
    }

    @Override
    public double getValue(Instant clock) {
        return Duration.between(clock, time).toMillis() / timeUnit;
    }

    @Override
    public Instant resetTime() {
        return time;
    }
}
