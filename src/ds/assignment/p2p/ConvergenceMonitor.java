package ds.assignment.p2p;

import java.io.FileWriter;

/**
 * Monitors convergence to target value and logs convergence events
 * Handles convergence detection and result logging
 */
public class ConvergenceMonitor {
    
    private final String peerId;
    private final double targetValue;
    private final double convergenceThreshold;
    private final long startTime;
    private volatile boolean converged = false;
    
    public ConvergenceMonitor(String peerId, double targetValue, double convergenceThreshold) {
        this.peerId = peerId;
        this.targetValue = targetValue;
        this.convergenceThreshold = convergenceThreshold;
        this.startTime = System.currentTimeMillis();
    }
    
    /**
     * Check if current value has converged to target
     */
    public void checkConvergence(double currentValue) {
        if (targetValue > 0 && !converged) {
            double diff = Math.abs(currentValue - targetValue);
            if (diff < convergenceThreshold) {
                converged = true;
                long convergenceTime = System.currentTimeMillis() - startTime;
                System.out.printf("CONVERGENCE: %s reached target %.6f in %d ms%n", 
                    peerId, targetValue, convergenceTime);
                
                logConvergenceEvent(currentValue, convergenceTime);
            }
        }
    }
    
    /**
     * Log convergence event to file
     */
    private void logConvergenceEvent(double currentValue, long convergenceTime) {
        try {
            FileWriter fw = new FileWriter("convergence.log", true);
            fw.write(String.format("%s,%d,%.6f,%.6f,%d%n", 
                    peerId, System.currentTimeMillis(), currentValue, targetValue, convergenceTime));
            fw.close();
        } catch (Exception e) {
            // Ignore file errors - convergence detection is more important than logging
            System.err.printf("%s: Failed to log convergence: %s%n", peerId, e.getMessage());
        }
    }
    
    public boolean hasConverged() {
        return converged;
    }
    
    public double getTargetValue() {
        return targetValue;
    }
    
    public double getConvergenceThreshold() {
        return convergenceThreshold;
    }
    
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
}