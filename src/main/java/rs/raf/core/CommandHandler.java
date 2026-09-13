package rs.raf.core;

import lombok.extern.slf4j.Slf4j;
import rs.raf.crawler.DirectoryCrawler;
import rs.raf.job.JobDispatcher;
import rs.raf.job.JobQueue;
import rs.raf.job.StopJob;
import rs.raf.job.WebJob;
import rs.raf.result.ResultRetrieverImpl;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Slf4j
public class CommandHandler {
    private final JobQueue jobQueue;
    private final DirectoryCrawler directoryCrawler;
    private final ResultRetrieverImpl resultRetriever;

    public CommandHandler() {
        this.jobQueue = new JobQueue();
        this.resultRetriever = new ResultRetrieverImpl(10);
        this.directoryCrawler = new DirectoryCrawler(this.jobQueue);
    }

    public void addDir(String param, int paramCount) {
        if (isParamValid(paramCount, 1)) {
            this.directoryCrawler.addPath(param);
        }
    }

    public void addWeb(String param, int paramCount) {
        if (isParamValid(paramCount, 1)) {
            try {
                jobQueue.addJob(new WebJob(param, Integer.parseInt(App.getProperty(App.HOP_COUNT)), jobQueue));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void getResult(String param, int paramCount) {
        if (isParamValid(paramCount, 1)) {
            Future<Map<String, Map<String, Integer>>> future = this.resultRetriever.retrieveResult(param, "get");
            log.info("Getting results...");
            try {
                future.get().forEach((k, v) -> log.info("{} : {}", k, v));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                log.error("Failed to get result", e);
            }
        }
    }

    public void queryResult(String param, int paramCount) {
        if (isParamValid(paramCount, 1)) {
            this.resultRetriever.retrieveResult(param, "query");
        }
    }

    public void clearWebSummary(int paramCount) {
        if (isParamValid(paramCount, 0)) {
            this.resultRetriever.retrieveResult("", "cws");
        }
    }

    public void clearFileSummary(int paramCount) {
        if (isParamValid(paramCount, 0)) {
            this.resultRetriever.retrieveResult("", "cfs");
        }
    }

    public void stop() {
        log.info("stopping...");
        this.resultRetriever.shutdownNow();
        this.directoryCrawler.stop();
        try {
            jobQueue.addJob(new StopJob());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isParamValid(int paramCount, int expectedParamCount) {
        if (paramCount != expectedParamCount) {
            log.warn("Expected: {} parameters, but got: {}", expectedParamCount, paramCount);
            return false;
        }
        return true;
    }

    public void start() {
        this.directoryCrawler.start();
        new JobDispatcher(this.jobQueue, this.resultRetriever).start();
    }
}
