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
    void onAllJobsSuccessfullyDone();

    /**
     * Called to potentially write a progress how many jobs are completed.
     * @param jobsEnded number of jobs ended
     */
    void writeProgress(final int jobsEnded);

    /**
     * Gets the maximum number of jobs allowed to run concurrently
     * @return the number of jobs
     */
    int getMaxConcurrentJobs();

    /**
     * Starts a job.
     * @param index the index of the job
     */
    void startJob(final int index);
}
