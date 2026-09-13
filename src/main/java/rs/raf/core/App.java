package rs.raf.core;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class App {
    public static final String KEYWORDS = "KEYWORDS";
    public static final String FILE_CORPUS_PREFIX = "FILE_CORPUS_PREFIX";
    public static final String DIR_CRAWLER_SLEEP_TIME = "DIR_CRAWLER_SLEEP_TIME";
    public static final String FILE_SCANNING_SIZE_LIMIT = "FILE_SCANNING_SIZE_LIMIT";
    public static final String HOP_COUNT = "HOP_COUNT";
    public static final String URL_REFRESH_TIME = "URL_REFRESH_TIME";

    private static final String CONFIG_PATH = "./src/main/resources/app.properties";
    private static final Map<String, String> properties = new ConcurrentHashMap<>();
    private static final Map<String, Long> visitedPages = new ConcurrentHashMap<>();

    public static String getProperty(String key) {
        return properties.get(key);
    }

    public void readConfig() {
        try (Scanner s = new Scanner(new File(CONFIG_PATH))) {
            while (s.hasNextLine()) {
                String line = s.nextLine().trim();
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                String[] keyValue = line.split("=", 2);
                if (keyValue.length == 2) {
                    properties.put(keyValue[0].trim().toUpperCase(), keyValue[1].trim());
                }
            }
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void handleCommands() {
        CommandHandler commandHandler = new CommandHandler();
        commandHandler.start();

        try (Scanner s = new Scanner(System.in)) {
            while (true) {
                System.out.print("Enter a command: "); // NOSONAR interactive prompt, must stay on the same line as input
                String[] commandParam = s.nextLine().trim().split(" ");
                String command = commandParam[0];
                String param = commandParam.length == 2 ? commandParam[1] : "";
                int paramCount = commandParam.length - 1;

                switch (command) {
                    case "ad" -> commandHandler.addDir(param, paramCount);
                    case "aw" -> commandHandler.addWeb(param, paramCount);
                    case "get" -> commandHandler.getResult(param, paramCount);
                    case "query" -> commandHandler.queryResult(param, paramCount);
                    case "cws" -> commandHandler.clearWebSummary(paramCount);
                    case "cfs" -> commandHandler.clearFileSummary(paramCount);
                    case "stop" -> {
                        commandHandler.stop();
                        return;
                    }
                    default -> log.warn("Invalid command");
                }
            }
        }
    }

    public static synchronized boolean isWebPageVisited(String url) {
        Long lastVisit = visitedPages.get(url);
        long now = System.currentTimeMillis();
        if (lastVisit != null && now - lastVisit < Long.parseLong(properties.get(URL_REFRESH_TIME))) {
            log.info("Already visited: {}", url);
            return true;
        }
        visitedPages.put(url, now);
        return false;
    }
}
