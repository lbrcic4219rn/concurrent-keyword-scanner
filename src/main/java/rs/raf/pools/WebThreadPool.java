package rs.raf.pools;

import lombok.Getter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Getter
public class WebThreadPool {

    private final ExecutorService pool;

    public WebThreadPool(int size) {
        this.pool = Executors.newFixedThreadPool(size);
    }
}
