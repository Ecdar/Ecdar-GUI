package ecdar.mutation;

import java.util.ArrayList;
import java.util.List;

public class AdjustableConcurrentJobsDriver {
    private final AdjustableConcurrentJobsHandler handler;
    private final List<Runnable> jobs = new ArrayList<>();
    private int jobsStarted;
    private int jobsEnded;

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
        if (getJobsRunning() == 0) {
            jobsStarted = 0;
            jobsEnded = 0;
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
        jobsEnded++;
        updateJobs();
    }

    /**
     * Updates what test-case generation jobs to run.
     */
    private synchronized void updateJobs() {
        if (handler.shouldStop()) {
            if (getJobsRunning() == 0) {
                handler.onStopped();
            }

            return;
        }

        // If we are done, clean up and move on
        if (jobsEnded >= jobs.size()) {
            jobs.clear();
            handler.onAllJobsSuccessfullyDone();
            return;
        }

        handler.onProgressRemaining(getJobsRemaining());

        // while we have not reach the maximum allowed threads and there are still jobs to start
        while (getJobsRunning() < handler.getMaxConcurrentJobs() &&
                jobsStarted < jobs.size()) {
            jobs.get(jobsStarted).run();
            jobsStarted++;
        }
    }


    /**
     * Gets the number of generation jobs currently running.
     * @return the number of jobs running
     */
    private synchronized int getJobsRunning() {
        return jobsStarted - jobsEnded;
    }

    private synchronized int getJobsRemaining() {
        return jobs.size() - jobsEnded;
    }
}
