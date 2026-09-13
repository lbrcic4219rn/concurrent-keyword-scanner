package rs.raf.job;

import lombok.extern.slf4j.Slf4j;
import lombok.Getter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import rs.raf.core.App;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class WebJob implements Callable<Map<String, Integer>>, Job {
    @Getter
    private final String path;
    @Getter
    private final int hopCount;
    private final JobQueue jobQueue;
    private final Map<String, Integer> webPageResult;
    private final String[] keywords;

    public WebJob(String path, int hopCount, JobQueue jobQueue) {
        this.jobQueue = jobQueue;
        this.hopCount = hopCount;
        this.path = path;
        this.webPageResult = new ConcurrentHashMap<>();
        this.keywords = App.getProperty(App.KEYWORDS).split(",");
    }

    @Override
    public Map<String, Integer> call() throws InterruptedException {
        try {
            Document document = Jsoup.connect(path).get();
            if (this.hopCount > 0) {
                for (Element link : document.select("a[href]")) {
                    String url = link.attr("abs:href").trim();
                    if (!isSkippable(url) && !App.isWebPageVisited(url)) {
                        jobQueue.addJob(new WebJob(url, this.hopCount - 1, this.jobQueue));
                    }
                }
            }
            for (String word : document.text().split("\\s+")) {
                for (String keyword : this.keywords) {
                    if (word.equals(keyword)) {
                        this.webPageResult.merge(word, 1, Integer::sum);
                    }
                }
            }
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Failed to scan {}: {}", path, e.getMessage());
            return new ConcurrentHashMap<>();
        }

        log.info("{} : {}", path, webPageResult);
        return webPageResult;
    }

    private static boolean isSkippable(String url) {
        return url.contains("#")
                || url.contains("%")
                || !url.startsWith("http")
                || url.endsWith(".jpg")
                || url.endsWith(".png")
                || url.endsWith(".pdf")
                || url.contains(".php");
    }

    @Override
    public ScanType getType() {
        return ScanType.WEB;
    }
}
