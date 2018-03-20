package ecdar.mutation;

/**
 * Driver for running jobs concurrently.
 */
public class ConcurrentJobsDriver {
    private final ConcurrentJobsHandler handler;
    private final int jobsNum;
    private int generationJobsStarted = 0;
    private int generationJobsEnded = 0;

    /**
     * Constructs.
     * @param handler handler to run jobs with
     * @param jobsNum total number of jobs to run
     */
    public ConcurrentJobsDriver(final ConcurrentJobsHandler handler, final int jobsNum) {
        this.handler = handler;
        this.jobsNum = jobsNum;
    }

    /**
     * Starts the jobs.
     */
    public void start() {
        updateJobs();
    }

    /**
     * Should be called when a job attempt is done.
     * Even when the jobs failed.
     *
     * This method should be called in a JavaFX thread, since it could update JavaFX elements.
     */
    public synchronized void onJobDone() {
        generationJobsEnded++;
        updateJobs();
    }

    /**
     * Updates what test-case generation jobs to run.
     */
    private synchronized void updateJobs() {
        if (handler.shouldStop()) {
            if (getGenerationJobsRunning() == 0) {
                handler.onStopped();
            }

            return;
        }

        // If we are done, clean up and move on
        if (generationJobsEnded >= jobsNum) {
            handler.onAllJobsSuccessfullyDone();
            return;
        }

        handler.writeProgress(generationJobsEnded);

        // while we have not reach the maximum allowed threads and there are still jobs to start
        while (getGenerationJobsRunning() < handler.getMaxConcurrentJobs() &&
                generationJobsStarted < jobsNum) {
            handler.startJob(generationJobsStarted);
            generationJobsStarted++;
        }
    }


    /**
     * Gets the number of generation jobs currently running.
     * @return the number of jobs running
     */
    private synchronized int getGenerationJobsRunning() {
        return generationJobsStarted - generationJobsEnded;
    }
}
