package rs.raf.result;

import rs.raf.job.Job;
import rs.raf.job.ScanType;

import java.util.Map;
import java.util.concurrent.Future;

public interface ResultRetriever {
    Map<String, Map<String, Integer>> singleResult(String query, String command);

    Map<String, Map<String, Integer>> summaryResult(String query, String command);

    void addCorpusResult(Job submittedJob, Future<Map<String, Integer>> jobFutureResult);

    void clearSummary(ScanType summaryType);
}
