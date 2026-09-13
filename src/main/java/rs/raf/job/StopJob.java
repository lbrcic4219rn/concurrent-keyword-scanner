package rs.raf.job;

public class StopJob implements Job {

    @Override
    public ScanType getType() {
        return ScanType.STOP;
    }
}
