package rs.raf.crawler;

import lombok.extern.slf4j.Slf4j;
import rs.raf.core.App;
import rs.raf.job.FileJob;
import rs.raf.job.JobQueue;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DirectoryCrawler {
    private final JobQueue jobQueue;
    private final ScheduledExecutorService scheduler;
    private final long sleepTime;
    private final List<String> roots;
    private final Map<String, Long> timeModifiedCache;

    public DirectoryCrawler(JobQueue jobQueue) {
        this.jobQueue = jobQueue;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.sleepTime = Long.parseLong(App.getProperty(App.DIR_CRAWLER_SLEEP_TIME));
        this.roots = new CopyOnWriteArrayList<>();
        this.timeModifiedCache = new ConcurrentHashMap<>();
    }

    public void start() {
        scheduler.scheduleWithFixedDelay(this::crawl, 0, sleepTime, TimeUnit.MILLISECONDS);
    }

    public void addPath(String path) {
        this.roots.add(path);
    }

    private void crawl() {
        try {
            for (String root : roots) {
                searchFolder(root);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void searchFolder(String root) throws InterruptedException {
        File[] children = new File(root).listFiles();
        if (children == null) {
            return;
        }
        for (File f : children) {
            if (!f.isDirectory()) {
                continue;
            }
            if (f.getName().startsWith(App.getProperty(App.FILE_CORPUS_PREFIX))) {
                submitIfModified(f);
            } else {
                searchFolder(f.getAbsolutePath());
            }
        }
    }

    private void submitIfModified(File corpus) throws InterruptedException {
        String absolutePath = corpus.getAbsolutePath();
        Long cachedLastModified = this.timeModifiedCache.get(absolutePath);
        if (cachedLastModified == null || corpus.lastModified() != cachedLastModified) {
            this.timeModifiedCache.put(absolutePath, corpus.lastModified());
            File[] corpusFiles = corpus.listFiles();
            int fileCount = corpusFiles == null ? 0 : corpusFiles.length;
            this.jobQueue.addJob(new FileJob(absolutePath, 0, fileCount - 1));
        }
    }

    public void stop() {
        log.info("dir crawler stopping...");
        this.scheduler.shutdown();
        log.info("dir crawler stopped");
    }
}
