package ecdar.mutation.models;

import ecdar.mutation.MutationTestingException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A reader that asynchronously reads inputs.
 */
public class AsyncInputReader {
    private final List<String> lines = Collections.synchronizedList(new ArrayList<>());
    private IOException ioException;
    private MutationTestingException mutationException;

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
                    lines.add(line);
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
     */
    public boolean ready() {
        return !lines.isEmpty();
    }

    /**
     * If an error has occurred, this throws an exception.
     * @throws MutationTestingException if an error has occurred on the error stream
     * @throws IOException if an IO error has occurred
     */
    public void checkExceptions() throws IOException, MutationTestingException {
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
}
