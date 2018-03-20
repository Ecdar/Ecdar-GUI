package ecdar.mutation;

public interface Job {
    void start(final Runnable onDone);
}
