package rs.raf.result;

import lombok.RequiredArgsConstructor;
import rs.raf.job.ScanType;

import java.util.Map;
import java.util.concurrent.Callable;

@RequiredArgsConstructor
public class ResultJob implements Callable<Map<String, Map<String, Integer>>> {

    private final String query;
    private final ResultRetriever resultRetriever;
    private final String command;

    @Override
    public Map<String, Map<String, Integer>> call() {
        switch (command) {
            case "cfs" -> {
                resultRetriever.clearSummary(ScanType.FILE);
                return Map.of();
            }
            case "cws" -> {
                resultRetriever.clearSummary(ScanType.WEB);
                return Map.of();
            }
            case "get", "query" -> {
                if (isSummary()) {
                    return resultRetriever.summaryResult(query, command);
                }
                return resultRetriever.singleResult(query, command);
            }
            default -> {
                return Map.of();
            }
        }
    }

    private boolean isSummary() {
        String[] params = this.query.split("\\|");
        if (params.length != 2) {
            return false;
        }
        return params[1].contains("summary") && (params[0].equals("web") || params[0].equals("file"));
    }
}
