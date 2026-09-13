package rs.raf.pools;

import lombok.Getter;

import java.util.concurrent.ForkJoinPool;

@Getter
public class FileThreadPool {

    private final ForkJoinPool pool = new ForkJoinPool();
}
