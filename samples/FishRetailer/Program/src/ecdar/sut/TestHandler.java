package ecdar.sut;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public abstract class TestHandler {
    final double timeUnit; // In ms

    final List<String> linesBuffer = new ArrayList<>();

    final List<Runner> tempLinesListeners = new ArrayList<>(); // TO be called a single time when a line line appears

    public static TestHandler createHandler(final double timeUnit, final boolean shouldSimulate) {
        if (shouldSimulate) return new SimulatedTimeTestHandler(timeUnit);
        else return new RealTimeTestHandler(timeUnit);
    }

    TestHandler(double timeUnit) {
        this.timeUnit = timeUnit;

        new Thread(() -> {
            String line;
            final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            try {
                while ((line = reader.readLine()) != null) {
                    final List<Runner> listeners;

                    synchronized (this) {
                        linesBuffer.add(line);

                        listeners = new ArrayList<>(tempLinesListeners);
                        tempLinesListeners.clear();
                    }

                    listeners.forEach(listener -> {
                        try {
                            listener.run();
                        } catch (IOException | InterruptedException e) {
                            write("Debug: Exception, " + e.getMessage());
                        }
                    });
                }
            } catch (IOException e) {
                write("Debug: Exception, " + e.getMessage());
            }
        }).start();
    }

    public boolean inputReady() {
        return !linesBuffer.isEmpty();
    }

    public String read() {
        final String line = linesBuffer.get(0);
        linesBuffer.remove(0);
        return line;
    }

    public abstract void start(final Runner stepStarter) throws IOException, InterruptedException;

    public abstract void onStepDone(final Runner startNewStep) throws InterruptedException, IOException;

    /**
     * In time units
     * @param clock
     * @return
     */
    public abstract double getValue(final Instant clock);

    public abstract Instant resetTime();


    public void write(String message) {
        System.out.println(message);
    }
}
