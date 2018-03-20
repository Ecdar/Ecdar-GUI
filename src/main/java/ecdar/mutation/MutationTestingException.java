package ecdar.mutation;

/**
 * An exception representing an error while working on a task related with.
 */
public class MutationTestingException extends Exception {
    /**
     * Constructs with a message.
     * @param message the message
     */
    public MutationTestingException(final String message) {
        super(message);
    }

    /**
     * Constructs with a message and a cause.
     * @param message the message
     * @param cause the cause
     */
    public MutationTestingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
