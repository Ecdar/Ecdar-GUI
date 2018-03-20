package ecdar.mutation;

import java.util.ArrayList;
import java.util.List;

public class AdjustableConcurrentJobsDriver {
    private final AdjustableConcurrentJobsHandler handler;
    private final List<Runnable> jobs = new ArrayList<>();
    private int generationJobsStarted;
    private int generationJobsEnded;

    /**
     * Constructs.
     * @param handler handler to run jobs with
     */
    public AdjustableConcurrentJobsDriver(final AdjustableConcurrentJobsHandler handler) {
        this.handler = handler;
    }

    /**
     * Starts the jobs.
     * If no jobs are currently running, this method resets counters for how many jobs are running.
     */
    public synchronized void start() {
        if (getGenerationJobsRunning() == 0) {
            generationJobsStarted = 0;
            generationJobsEnded = 0;
        }

        updateJobs();
    }

    public synchronized void addJobs(final List<Runnable> jobs) {
        this.jobs.addAll(jobs);
    }

    public synchronized void addJob(final Runnable job) {
        jobs.add(job);
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
        if (generationJobsEnded >= jobs.size()) {
            jobs.clear();
            handler.onAllJobsSuccessfullyDone();
            return;
        }

        handler.writeProgress(generationJobsEnded, jobs.size());

        // while we have not reach the maximum allowed threads and there are still jobs to start
        while (getGenerationJobsRunning() < handler.getMaxConcurrentJobs() &&
                generationJobsStarted < jobs.size()) {
            jobs.get(generationJobsStarted).run();
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
