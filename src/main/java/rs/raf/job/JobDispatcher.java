package rs.raf.job;

import lombok.extern.slf4j.Slf4j;
import rs.raf.pools.FileThreadPool;
import rs.raf.pools.WebThreadPool;
import rs.raf.result.ResultRetrieverImpl;

import java.util.Map;
import java.util.concurrent.Future;

@Slf4j
public class JobDispatcher extends Thread {

    private final JobQueue jobQueue;
    private final FileThreadPool fileThreadPool;
    private final WebThreadPool webThreadPool;
    private final ResultRetrieverImpl resultRetriever;
    private volatile boolean working = true;

    public JobDispatcher(JobQueue jobQueue, ResultRetrieverImpl resultRetriever) {
        this.jobQueue = jobQueue;
        this.fileThreadPool = new FileThreadPool();
        this.webThreadPool = new WebThreadPool(10);
        this.resultRetriever = resultRetriever;
    }

    @Override
    public void run() {
        this.dispatchJobs();
    }

    public void terminate() {
        fileThreadPool.shutdownNow();
        webThreadPool.shutdownNow();
        log.info("stopping dispatcher...");
        working = false;
    }

    private void dispatchJobs() {
        try {
            while (working) {
                Job job = this.jobQueue.takeJob();
                switch (job.getType()) {
                    case FILE -> {
                        FileJob fileJob = (FileJob) job;
                        log.info("Starting file job: {}", fileJob.getPath());
                        Future<Map<String, Integer>> fileResult = fileThreadPool.submit(fileJob);
                        resultRetriever.addCorpusResult(fileJob, fileResult);
                    }
                    case WEB -> {
                        WebJob webJob = (WebJob) job;
                        log.info("Starting web job: {}", webJob.getPath());
                        Future<Map<String, Integer>> webResult = webThreadPool.submit(webJob);
                        resultRetriever.addCorpusResult(webJob, webResult);
                    }
                    case STOP -> terminate();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Job dispatcher stopped");
    }
}
