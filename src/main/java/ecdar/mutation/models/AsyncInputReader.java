package ecdar.mutation.models;

import ecdar.mutation.MutationTestingException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Consumer;
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

    public synchronized void addTempListener(final Runnable runnable) {
        tempListeners.add(runnable);
    }

    /**
     * Constructs the reader and starts reading in another thread.
     * @param process the process to read from
     */
    public AsyncInputReader(final Process process) {
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

                    lines.add(line);

                    // Call the listeners in a copy of them to avoid concurrency errors
                    final List<Runnable> listeners;
                    synchronized (this) {
                        listeners = new ArrayList<>(tempListeners);
                        tempListeners.clear();
                    }
                    listeners.forEach(Runnable::run);
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
     * Consumes a specific input.
     * It does not have to be the input in front.
     * Waits for about 5 seconds for the input to appear.
     * @param input the input to consume
     * @throws InterruptedException if an error occurs
     * @throws MutationTestingException if the input did not appear within 5 seconds
     */
    public void waitAndConsume(final String input) throws InterruptedException, MutationTestingException {
        for (int i = 0; i < 100; i++) {
            if (lines.contains(input)) {
                lines.remove(input);
                return;
            }

            Thread.sleep(50);
        }

        throw new MutationTestingException("System under test did not respond with \"" + input + "\" within 5 seconds");
    }
}
