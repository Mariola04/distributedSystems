package ds.assignment.common.utils;

import java.util.Random;

/**
 * Utility class for generating Poisson distributed intervals
 */
public class PoissonGenerator {
    
    private static final Random random = new Random();
    
    /**
     * Generate next interval following Poisson distribution
     * 
     * @param rate Events per time unit (lambda parameter)
     * @return Interval in time units until next event
     */
    public static double getNextInterval(double rate) {
        return -Math.log(1.0 - random.nextDouble()) / rate;
    }
    
    /**
     * Set seed for reproducible testing
     */
    public static void setSeed(long seed) {
        random.setSeed(seed);
    }
}