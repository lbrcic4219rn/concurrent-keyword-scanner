# Keyword Scanner

Concurrent keyword counter for text corpora on disk and for web pages. It is a console application: you type commands, jobs run in background thread pools, and you ask for results when you need them.

## Requirements

- Java 21
- Maven 3.9 or newer

## Build and run

```bash
mvn clean package
```

```bash
mvn exec:java -Dexec.mainClass=rs.raf.Main
```

Run it from the project root, because the configuration is read from `./src/main/resources/app.properties`.

## Configuration

Properties live in `src/main/resources/app.properties`.

| Property | Meaning |
| --- | --- |
| `keywords` | Comma separated words to count |
| `file_corpus_prefix` | Folder name prefix that marks a corpus |
| `dir_crawler_sleep_time` | Pause between directory scans, in ms |
| `file_scanning_size_limit` | Size at which a file job splits in two, in bytes |
| `hop_count` | How many link hops the web scanner follows |
| `url_refresh_time` | How long a visited url stays visited, in ms |

## Commands

| Command | Meaning |
| --- | --- |
| `ad <path>` | Watch a directory tree for corpus folders |
| `aw <url>` | Scan a web page and follow its links |
| `get <query>` | Wait for the result and print it |
| `query <query>` | Print the result only if the scan already finished |
| `cfs` | Clear the file summary |
| `cws` | Clear the web summary |
| `stop` | Shut down all pools and exit |

A query is `file|<corpus path>`, `web|<domain>`, `file|summary` or `web|summary`.

Example session:

```
ad example/data
aw https://example.com
get file|summary
stop
```

## How it works

- `DirectoryCrawler` scans the watched trees on a schedule and queues a job for every new or changed corpus folder.
- `JobDispatcher` takes jobs off the queue and submits them to a fork join pool for files and a fixed pool for web pages.
- `FileJob` splits a corpus recursively until a chunk fits inside `file_scanning_size_limit`.
- `WebJob` fetches a page with jsoup, counts keywords, and queues the links it found while hops remain.
- `ResultRetrieverImpl` keeps the pending results and builds the per corpus, per domain and summary views.

## Project layout

```
src/main/java/rs/raf
  core      app startup, config, command loop
  crawler   directory crawler
  job       job queue, dispatcher, file and web jobs
  pools     thread pool wrappers
  result    result storage and retrieval
```

## Logging

Logging goes through SLF4J with Logback. The console pattern lives in `src/main/resources/logback.xml`. Set the root level to `DEBUG` there for more detail.
