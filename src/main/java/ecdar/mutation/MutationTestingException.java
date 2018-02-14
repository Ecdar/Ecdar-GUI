package ecdar.mutation;

/**
 * An exception representing an error while working on a task related with
 */
public class MutationTestingException extends Exception {
    public MutationTestingException(final String message) {
        super(message);
    }

    public MutationTestingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
