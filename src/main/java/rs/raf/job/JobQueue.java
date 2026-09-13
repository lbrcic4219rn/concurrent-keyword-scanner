package rs.raf.job;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class JobQueue {
    private final BlockingQueue<Job> jobs;

    public JobQueue() {
        this.jobs = new LinkedBlockingQueue<>();
    }

    public void addJob(Job job) throws InterruptedException {
        this.jobs.put(job);
    }

    public Job takeJob() throws InterruptedException {
        return this.jobs.take();
    }
}
