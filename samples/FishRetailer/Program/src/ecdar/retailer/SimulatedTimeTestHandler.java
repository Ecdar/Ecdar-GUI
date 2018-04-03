package ecdar.retailer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimulatedTimeTestHandler extends TestHandler {
    private static String bufferLine;
    private Instant time;
    private boolean isDelaying = false;

    public SimulatedTimeTestHandler(final double timeUnit) {
        super(timeUnit);

        time = Instant.now();
    }

    @Override
    public boolean inputReady() throws IOException {
        return (bufferLine != null) || (System.in.available() != 0);
    }

    @Override
    public String read() {
        if (bufferLine != null) {
            final String line = bufferLine;
            bufferLine = null;
            return line;
        } else return new Scanner(System.in).nextLine();
    }

    @Override
    public void onStepDone() {
        write("Step done");

        if (bufferLine != null) throw new RuntimeException("While starting new step, buffer is non-empty");

        final String line = new Scanner(System.in).nextLine();

        final Matcher matcher = Pattern.compile("Delay: (\\d+)").matcher(line);

        if (matcher.find()) { // If delay, simulate delay
            time = time.plus(Duration.ofMillis(Long.parseLong(matcher.group(1))));
        } else { // Else, it is an input, put it on the buffer
            bufferLine = line;
        }
    }

    @Override
    double getValue(Instant clock) {
        return Duration.between(clock, time).toMillis() / timeUnit;
    }

    @Override
    Instant resetTime() {
        return time;
    }
}
