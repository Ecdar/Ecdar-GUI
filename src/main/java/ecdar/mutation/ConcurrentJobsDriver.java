package ecdar.mutation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Driver for running jobs concurrently.
 * You can add jobs to the driver at any time.
 */
public class ConcurrentJobsDriver {
    private final ConcurrentJobsHandler handler;
    private final List<Runnable> jobs = new ArrayList<>();
    private int jobsStarted;
    private int jobsEnded;

    /**
     * Constructs.
     * @param handler handler to run jobs with
     */
    public ConcurrentJobsDriver(final ConcurrentJobsHandler handler) {
        this.handler = handler;
    }

    private void clearJobs() {
        this.jobs.clear();
        jobsStarted = 0;
        jobsEnded = 0;
    }

    /**
     * Adds jobs.
     * If no other jobs are left to be run, this method resets counters for how many jobs are remaining and starts running jobs.
     * @param jobs the jobs to add
     */
    public synchronized void addJobs(final List<Runnable> jobs) {
        this.jobs.addAll(jobs);

        updateJobs();
    }

    /**
     * Adds a job.
     * If no other jobs are left to be run, this method resets counters for how many jobs are remaining and starts running jobs.
     * @param job the job to add
     */
    public synchronized void addJob(final Runnable job) {
        addJobs(Stream.of(job).collect(Collectors.toList()));
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
        if (getJobsRemaining() <= 0) {
            clearJobs();
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
