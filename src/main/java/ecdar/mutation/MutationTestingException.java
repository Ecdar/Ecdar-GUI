package ecdar.mutation;

public class MutationTestingException extends Exception {
    public MutationTestingException(final String message) {
        super(message);
    }

    public MutationTestingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
