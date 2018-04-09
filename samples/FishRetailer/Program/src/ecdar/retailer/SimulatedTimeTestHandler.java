package ecdar.retailer;

import org.omg.PortableServer.THREAD_POLICY_ID;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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
    private List<String> linesBuffer;

    public SimulatedTimeTestHandler(final double timeUnit) throws InterruptedException {
        super(timeUnit);

        time = Instant.now();

        linesBuffer = new ArrayList<>();


        new Thread(() -> {
            String line;
            final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            try {
                while ((line = reader.readLine()) != null) {
                    linesBuffer.add(line);
                }
            } catch (IOException e) {
                write("Debug: IOException, " + e.getMessage());
            }
        }).start();

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
    public void onStepDone() throws InterruptedException {
        waitForDelay();
    }

    private void waitForDelay() throws InterruptedException {
        // Sleep until delay appears
        while (linesBuffer.stream().noneMatch(this::isDelay)) Thread.sleep(100);

        // Fetch delay
        String delayLine = linesBuffer.stream().filter(this::isDelay).findFirst().orElseThrow(() -> new RuntimeException("lines had no delays"));

        final Matcher matcher = Pattern.compile("Delay: (\\d+)").matcher(delayLine);

        if (matcher.find()) { // Simulate delay
            time = time.plus(Duration.ofMillis(Long.parseLong(matcher.group(1))));
            linesBuffer.removeIf(this::isDelay);
        } else throw new RuntimeException("Delay line is not a delay");
    }

    private boolean isDelay(final String input) {
        final Matcher matcher = Pattern.compile("Delay: (\\d+)").matcher(input);

        return matcher.find();
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
