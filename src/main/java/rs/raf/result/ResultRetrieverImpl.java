package rs.raf.result;

import lombok.extern.slf4j.Slf4j;
import rs.raf.job.FileJob;
import rs.raf.job.Job;
import rs.raf.job.ScanType;
import rs.raf.job.WebJob;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
public class ResultRetrieverImpl implements ResultRetriever {
    private static final String QUERY = "query";
    private static final String KEY_VALUE_FORMAT = "{} : {}";

    private final ExecutorService pool;
    private final Map<String, Future<Map<String, Integer>>> fileResults = new ConcurrentHashMap<>();
    private final Map<String, Future<Map<String, Integer>>> webResults = new ConcurrentHashMap<>();

    private final Map<String, Map<String, Integer>> fileSummaryResults = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Integer>> webSummaryResults = new ConcurrentHashMap<>();

    public ResultRetrieverImpl(int n) {
        this.pool = Executors.newFixedThreadPool(n);
    }

    public Future<Map<String, Map<String, Integer>>> retrieveResult(String query, String command) {
        return pool.submit(new ResultJob(query, this, command));
    }

    public void shutdownNow() {
        pool.shutdownNow();
    }

    @Override
    public Map<String, Map<String, Integer>> singleResult(String query, String command) {
        String[] params = query.split("\\|");
        if (params.length != 2) {
            log.warn("Wrong query format: {}", query);
            return Map.of();
        }
        String type = params[0];
        String path = params[1];
        if (type.equals("file")) {
            return singleFileResult(path, command);
        }
        if (type.equals("web")) {
            return singleWebResult(path, command);
        }
        return Map.of();
    }

    private Map<String, Map<String, Integer>> singleFileResult(String path, String command) {
        Future<Map<String, Integer>> future = fileResults.get(path);
        if (future == null) {
            log.info("File {} is still being scanned", path);
            return Map.of();
        }
        if (command.equals(QUERY) && !future.isDone()) {
            log.info("{} is still being scanned", path);
            return Map.of();
        }
        Map<String, Map<String, Integer>> result = new ConcurrentHashMap<>();
        result.put(path, await(future));
        if (command.equals(QUERY)) {
            log.info("{}", result);
        }
        return result;
    }

    private Map<String, Map<String, Integer>> singleWebResult(String domain, String command) {
        Map<String, Integer> domainSum = new ConcurrentHashMap<>();
        boolean isDomainPresent = false;
        for (Map.Entry<String, Future<Map<String, Integer>>> entry : webResults.entrySet()) {
            String key = entry.getKey();
            try {
                if (!domain.equals(URI.create(key).getHost())) {
                    continue;
                }
                isDomainPresent = true;
                if (command.equals(QUERY) && !entry.getValue().isDone()) {
                    log.info("Webpage {} is still being scanned", key);
                    return Map.of();
                }
                entry.getValue().get().forEach((k, v) -> domainSum.merge(k, v, Integer::sum));
            } catch (IllegalArgumentException | ExecutionException e) {
                log.error(KEY_VALUE_FORMAT, key, e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Map.of();
            }
        }
        if (!isDomainPresent) {
            log.info("Domain is not present: {}", domain);
            return Map.of();
        }

        Map<String, Map<String, Integer>> result = new ConcurrentHashMap<>();
        result.put(domain, domainSum);
        if (command.equals(QUERY)) {
            log.info("{}", result);
        }
        return result;
    }

    @Override
    public Map<String, Map<String, Integer>> summaryResult(String query, String command) {
        String[] params = query.split("\\|");
        if (params.length != 2) {
            log.warn("Wrong query format: {}", query);
            return Map.of();
        }
        String type = params[0];
        if (type.equals("file")) {
            return fileSummary(command);
        }
        if (type.equals("web")) {
            return webSummary(command);
        }
        return Map.of();
    }

    private Map<String, Map<String, Integer>> fileSummary(String command) {
        if (fileSummaryResults.isEmpty()) {
            if (command.equals(QUERY) && fileResults.values().stream().anyMatch(f -> !f.isDone())) {
                log.info("File summary not ready");
                return Map.of();
            }
            fileResults.forEach((key, future) -> fileSummaryResults.put(key, await(future)));
        }
        if (command.equals(QUERY)) {
            printSummary(fileSummaryResults);
        }
        return fileSummaryResults;
    }

    private Map<String, Map<String, Integer>> webSummary(String command) {
        if (!webSummaryResults.isEmpty()) {
            if (command.equals(QUERY)) {
                printSummary(webSummaryResults);
            }
            return webSummaryResults;
        }

        if (command.equals(QUERY)) {
            for (Map.Entry<String, Future<Map<String, Integer>>> entry : webResults.entrySet()) {
                if (!entry.getValue().isDone()) {
                    log.info("Webpage {} is still being scanned", entry.getKey());
                    return Map.of();
                }
            }
        }

        for (Map.Entry<String, Future<Map<String, Integer>>> entry : webResults.entrySet()) {
            String key = entry.getKey();
            try {
                String domain = URI.create(key).getHost();
                Map<String, Integer> domainMap = webSummaryResults.computeIfAbsent(domain, d -> new ConcurrentHashMap<>());
                entry.getValue().get().forEach((k, v) -> domainMap.merge(k, v, Integer::sum));
            } catch (IllegalArgumentException | ExecutionException e) {
                log.error(KEY_VALUE_FORMAT, key, e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Map.of();
            }
        }

        if (command.equals(QUERY)) {
            printSummary(webSummaryResults);
        }
        return webSummaryResults;
    }

    private static void printSummary(Map<String, Map<String, Integer>> summary) {
        summary.forEach((k, v) -> log.info(KEY_VALUE_FORMAT, k, v));
    }

    private static Map<String, Integer> await(Future<Map<String, Integer>> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void addCorpusResult(Job submittedJob, Future<Map<String, Integer>> jobFutureResult) {
        switch (submittedJob.getType()) {
            case FILE -> fileResults.put(((FileJob) submittedJob).getPath(), jobFutureResult);
            case WEB -> webResults.put(((WebJob) submittedJob).getPath(), jobFutureResult);
            default -> {
                // no result to store
            }
        }
    }

    @Override
    public void clearSummary(ScanType summaryType) {
        if (summaryType == ScanType.FILE) {
            this.fileSummaryResults.clear();
        }
        if (summaryType == ScanType.WEB) {
            this.webSummaryResults.clear();
        }
    }
}
