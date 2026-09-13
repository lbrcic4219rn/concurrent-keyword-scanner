package rs.raf.pools;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public record WebThreadPool(ExecutorService pool) {

    public WebThreadPool(int size) {
        this(Executors.newFixedThreadPool(size));
    }

    public <T> Future<T> submit(Callable<T> task) {
        return pool.submit(task);
    }

    public void shutdownNow() {
        pool.shutdownNow();
    }
}
