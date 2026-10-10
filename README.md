# Keyword Scanner

Concurrent keyword counter for text corpora on disk and for web pages. It is a console application: you type commands, jobs run in background thread pools, and you ask for results when you need them.

## Requirements

* Java 21
* Maven 3.9 or newer

## Build and run

```bash
mvn clean package
```

```bash
mvn exec:java -Dexec.mainClass=rs.raf.Main
```

Run it from the project root, because the configuration is read from `./src/main/resources/app.properties`.

## Configuration

`src/main/resources/app.properties`:

* `keywords` is the comma separated list of words to count.
* `file_corpus_prefix` is the folder name prefix that marks a corpus.
* `dir_crawler_sleep_time` is the pause between directory scans, in ms.
* `file_scanning_size_limit` is the size at which a file job splits in two, in bytes.
* `hop_count` is how many link hops the web scanner follows.
* `url_refresh_time` is how long a visited url stays visited, in ms.

## Commands

* `ad <path>` watches a directory tree for corpus folders.
* `aw <url>` scans a web page and follows its links.
* `get <query>` waits for the result and prints it.
* `query <query>` prints the result only if the scan already finished.
* `cfs` clears the file summary.
* `cws` clears the web summary.
* `stop` shuts down all pools and exits.

A query is `file|<corpus path>`, `web|<domain>`, `file|summary` or `web|summary`.

Example session:

```
ad example/data
aw https://example.com
get file|summary
stop
```

## How it works

* `DirectoryCrawler` scans the watched trees on a schedule and queues a job for every new or changed corpus folder.
* `JobDispatcher` takes jobs off the queue and submits them to a fork join pool for files and a fixed pool for web pages.
* `FileJob` splits a corpus recursively until a chunk fits inside `file_scanning_size_limit`.
* `WebJob` fetches a page with jsoup, counts keywords, and queues the links it found while hops remain.
* `ResultRetrieverImpl` keeps the pending results and builds the per corpus, per domain and summary views.

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
