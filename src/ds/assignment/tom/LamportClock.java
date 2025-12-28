package ds.assignment.tom;

public class LamportClock {
    private volatile long clock;
    
    public LamportClock() {
        this.clock = 0;
    }
    
    public synchronized long tick() {
        return ++clock;
    }
    
    public synchronized long getClock() {
        return clock;
    }
    
    public synchronized void update(long receivedClock) {
        clock = Math.max(clock, receivedClock) + 1;
    }
}