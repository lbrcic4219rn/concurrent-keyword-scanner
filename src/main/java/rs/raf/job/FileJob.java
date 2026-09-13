package rs.raf.job;

import lombok.extern.slf4j.Slf4j;
import lombok.Getter;
import rs.raf.core.App;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serial;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;

@Slf4j
public class FileJob extends RecursiveTask<Map<String, Integer>> implements Job {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    @Getter
    private final String path;
    private final transient Map<String, Integer> jobResult;
    private final String[] keywords;
    private int left;
    private final int right;

    public FileJob(String path, int left, int right) {
        this.path = path;
        this.jobResult = new ConcurrentHashMap<>();
        this.left = left;
        this.right = right;
        this.keywords = App.getProperty(App.KEYWORDS).split(",");
    }

    @Override
    protected Map<String, Integer> compute() {
        File[] fileList = new File(this.path).listFiles();
        if (fileList == null) {
            return this.jobResult;
        }

        long corpusSize = 0;
        for (int i = left; i <= right; i++) {
            corpusSize += fileList[i].length();
        }

        if (corpusSize <= Long.parseLong(App.getProperty(App.FILE_SCANNING_SIZE_LIMIT)) || left == right) {
            countKeywords(fileList);
        } else {
            int mid = (right - left) / 2 + left;

            FileJob leftTask = new FileJob(this.path, left, mid);
            leftTask.fork();

            left = mid + 1;

            Map<String, Integer> rightResult = this.compute();
            Map<String, Integer> leftResult = leftTask.join();
            leftResult.forEach((k, v) -> rightResult.merge(k, v, Integer::sum));
            this.jobResult.putAll(rightResult);
        }
        return this.jobResult;
    }

    private void countKeywords(File[] files) {
        for (int i = left; i <= right; i++) {
            try (Scanner sc = new Scanner(files[i])) {
                while (sc.hasNextLine()) {
                    countKeywordsInLine(sc.nextLine());
                }
            } catch (FileNotFoundException e) {
                log.error("File not found: {}", e.getMessage());
            }
        }
    }

    private void countKeywordsInLine(String line) {
        for (String word : WHITESPACE.split(line)) {
            for (String keyword : this.keywords) {
                if (word.equals(keyword)) {
                    this.jobResult.merge(word, 1, Integer::sum);
                }
            }
        }
    }

    @Override
    public ScanType getType() {
        return ScanType.FILE;
    }
}
