package ds.assignment.p2p;

import ds.assignment.common.utils.PoissonGenerator;
import java.util.Random;
import java.util.Map;

/**
 * Manages anti-entropy algorithm for P2P value synchronization
 * Handles periodic neighbor selection and synchronization timing
 */
public class AntiEntropyManager {
    
    private final String peerId;
    private final Random random = new Random();
    private final P2PNetworking networking;
    private volatile boolean running = true;
    
    public AntiEntropyManager(String peerId, P2PNetworking networking) {
        this.peerId = peerId;
        this.networking = networking;
    }
    
    /**
     * Main anti-entropy loop using Poisson distribution for timing
     */
    public void runAntiEntropyLoop(P2PPeerCore peerCore) {
        while (running && networking.isRunning()) {
            try {
                // Poisson distribution: 2 events per minute = 2/60 per second
                double interval = PoissonGenerator.getNextInterval(2.0 / 60.0);
                Thread.sleep((long) (interval * 1000));
                
                performAntiEntropySync(peerCore);
                
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    /**
     * Perform one anti-entropy synchronization with random neighbor
     */
    private void performAntiEntropySync(P2PPeerCore peerCore) {
        Map<String, String> neighbors = networking.getNeighbors();
        
        if (!neighbors.isEmpty()) {
            // Choose random neighbor from map
            String[] neighborIds = neighbors.keySet().toArray(new String[0]);
            String randomNeighbor = neighborIds[random.nextInt(neighborIds.length)];
            String neighborAddress = neighbors.get(randomNeighbor);
            
            double currentValue = peerCore.getCurrentValue();
            double newValue = networking.synchronizeWith(randomNeighbor, neighborAddress, currentValue);
            peerCore.updateValue(newValue);
        }
    }
    
    public void stop() {
        running = false;
    }
    
    public boolean isRunning() {
        return running;
    }
}