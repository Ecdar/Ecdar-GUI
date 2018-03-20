package ecdar.mutation.models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A reader that asynchronously reads inputs.
 */
public class AsyncInputReader {
    private final List<String> lines = Collections.synchronizedList(new ArrayList<>());
    private IOException exception;

    /**
     * Constructs the reader and starts reading in another thread.
     * @param process the process to read from
     */
    public AsyncInputReader(final Process process) {
        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null){
                    lines.add(line);
                }
            } catch (IOException e) {
                exception = e;
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
     * Gets the exception that has happened.
     * @return the exception, or null if not have happened
     */
    public IOException getException() {
        return exception;
    }

    /**
     * Gets if an exception has happened.
     * @return true iff an exception has happened
     */
    public boolean isException() {
        return exception != null;
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
