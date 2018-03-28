package ecdar.mutation;

/**
 * Handler for supplying information about job handling and for starting a job.
 */
public interface ConcurrentJobsHandler {
    /**
     * Gets if we should stop working.
     * @return true iff we should stop
     */
    boolean shouldStop();

    /**
     * Called if jobs are stopped, rather than completed naturally.
     */
    void onStopped();

    /**
     * Called if jobs are completed naturally.
     */
    default void onAllJobsSuccessfullyDone() {}

    /**
     * Called when the number of remaining jobs are changed.
     * @param remaining number of jobs remaining
     */
    default void onProgressRemaining(final int remaining) {}

    /**
     * Gets the maximum number of jobs allowed to run concurrently
     * @return the number of jobs
     */
    int getMaxConcurrentJobs();
}
