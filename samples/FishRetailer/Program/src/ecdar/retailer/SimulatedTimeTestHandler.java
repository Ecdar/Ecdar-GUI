package ecdar.retailer;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimulatedTimeTestHandler extends TestHandler {
    private static String bufferLine;
    private Instant time;
    private boolean isDelaying = false;
    private List<String> linesBuffer;

    public SimulatedTimeTestHandler(final double timeUnit) {
        super(timeUnit);

        time = Instant.now();

        linesBuffer = new ArrayList<>();

        write("Debug: Start");

        waitForDelay();
    }

    @Override
    public boolean inputReady() throws IOException {
        return !linesBuffer.isEmpty();
    }

    @Override
    public String read() {
        final String line = linesBuffer.get(0);
        linesBuffer.remove(0);
        return line;
    }

    @Override
    public void onStepDone() {
        write("Debug: onStepDone");

        write("Delay done");

        waitForDelay();
    }

    private void waitForDelay() {
        write("Debug: waitForDelay");

        boolean done = false;

        while (!done) {
            write("Debug: reading next line");
            final String line = new Scanner(System.in).nextLine();
            write("Debug: Read line " + line);

            if (line.startsWith("Test")) {
                write("Debug: " + line);
                continue;
            }

            final Matcher matcher = Pattern.compile("Delay: (\\d+)").matcher(line);

            if (matcher.find()) { // If delay, simulate delay
                write("Debug: Got delay1");
                time = time.plus(Duration.ofMillis(Long.parseLong(matcher.group(1))));
                done = true;
            } else { // Else, it is an input, put it on the buffer
                linesBuffer.add(line);
            }
        }

        write("Debug: Got delay2");
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
