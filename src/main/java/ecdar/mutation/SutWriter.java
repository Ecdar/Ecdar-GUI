package ecdar.mutation;

import java.io.IOException;

public interface SutWriter {
    /**
     * Writes to the system.in of the system under test.
     * @param outputBroadcast the string to write to the system under test.
     * @throws IOException if an IO error occurs
     */
    void writeToSut(final String outputBroadcast) throws IOException;
}
