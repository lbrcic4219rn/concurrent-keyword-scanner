package rs.raf.pools;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

public class FileThreadPool {

    private final ForkJoinPool pool = new ForkJoinPool();

    public <T> ForkJoinTask<T> submit(ForkJoinTask<T> task) {
        return pool.submit(task);
    }

    public void shutdownNow() {
        pool.shutdownNow();
    }
}
