package ecdar.mutation.models;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AsyncInputReader {
    private final List<String> lines = Collections.synchronizedList(new ArrayList<>());
    private IOException exception;

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

    public boolean ready() {
        return !lines.isEmpty();
    }

    public IOException getException() {
        return exception;
    }

    public boolean isException() {
        return exception != null;
    }

    public String read() {
        final String line = lines.get(0);
        lines.remove(0);
        return line;
    }
}
