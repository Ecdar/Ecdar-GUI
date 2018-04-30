package ecdar.mutation.models;

import ecdar.mutation.MutationTestingException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A reader that asynchronously reads inputs.
 */
public class AsyncInputReader {
    private final List<String> lines = Collections.synchronizedList(new ArrayList<>());
    private IOException ioException;
    private MutationTestingException mutationException;

    private final List<Runnable> tempListeners = new ArrayList<>(); // To be called a single time when a line appears
    private final Process process;

    /**
     * Adds a listener for the next reading.
     * The listener is only called once.
     * @param listener the listener to be called
     */
    public synchronized void addTempListener(final Runnable listener) {
        tempListeners.add(listener);
    }

    /**
     * Constructs the reader and starts reading in another thread.
     * @param process the process to read from
     */
    public AsyncInputReader(final Process process) {
        this.process = process;
        // Read input stream
        new Thread(() -> {
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Catch SUT debug messages
                    final Matcher match = Pattern.compile("Debug: (.*)").matcher(line);
                    if (match.find()) {
                        System.out.println("SUT debug: " + match.group(1));
                        continue;
                    }

                    synchronized (this) {
                        lines.add(line);

                        // Call the listeners in a copy of them to avoid concurrency errors
                        final List<Runnable> listeners;

                        listeners = new ArrayList<>(tempListeners);
                        tempListeners.clear();

                        listeners.forEach(Runnable::run);
                    }
                }
            } catch (IOException e) {
                ioException = e;
            }
        }).start();

        // Read error stream
        new Thread(() -> {
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                final List<String> errorLines = reader.lines().collect(Collectors.toList());

                if (!errorLines.isEmpty()) {
                    throw new MutationTestingException("Error from an error stream: " + String.join("\n", errorLines));
                }
            } catch (final IOException e) {
                ioException = e;
            } catch (MutationTestingException e) {
                mutationException = e;
            }
        }).start();
    }

    /**
     * Gets if an input is ready to be consumed.
     * @return true iff ready
     * @throws MutationTestingException if an error has occurred on the error stream
     * @throws IOException if an IO error has occurred
     */
    public boolean ready() throws IOException, MutationTestingException {
        checkExceptions();

        return !lines.isEmpty();
    }

    /**
     * If an error has occurred, this throws an exception.
     * @throws MutationTestingException if an error has occurred on the error stream
     * @throws IOException if an IO error has occurred
     */
    private void checkExceptions() throws IOException, MutationTestingException {
        if (ioException != null) throw ioException;
        if (mutationException != null) throw mutationException;
    }

    /**
     * Consumes an input.
     * @return the input
     */
    public String consume() {
        final String line = lines.get(0);
        lines.remove(0);
        return line;
    }

    /**
     * Consumes a specified input, but with some timeout.
     * The specified input does not have to be the input in front.
     * Waits for 5 seconds for the input to appear.
     * @param input the input to consume
     * @param onConsumed listener to be called when input is consumed, if it was done before the timeout
     * @param onTimeout listener to be called in 5 seconds, if the input was not consumed
     */
    public void consumeWithTimeout(final String input, final Runnable onConsumed, final Runnable onTimeout) {
        if (!process.isAlive()) {
            onConsumed.run();
            return;
        }

        final SingleRunnableHandler runnableHandler = new SingleRunnableHandler();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (!process.isAlive()) onConsumed.run();
                else runnableHandler.run(onTimeout);
            }
        }, 5000);


        waitAndConsumeWithoutTimeout(input, () -> runnableHandler.run(onConsumed));
    }

    /**
     * Consumes a specified input.
     * The specified input does not have to be the input in front.
     * @param input the input to consume
     * @param onConsumed listener to be called when input is consumed
     */
    private synchronized void waitAndConsumeWithoutTimeout(final String input, final Runnable onConsumed) {
        if (lines.contains(input)) {
            lines.remove(input);
            onConsumed.run();
            return;
        }

        addTempListener(() -> waitAndConsumeWithoutTimeout(input, onConsumed));
    }
}
