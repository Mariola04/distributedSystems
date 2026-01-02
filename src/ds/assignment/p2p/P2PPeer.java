package ds.assignment.p2p;

/**
 * P2P peer that implements Anti-Entropy algorithm for data aggregation
 * Following PDF specifications exactly
 * 
 * This class now serves as a wrapper around the modular P2PPeerCore implementation
 * for backward compatibility with existing scripts and usage patterns
 */
public class P2PPeer {
    
    private final P2PPeerCore peerCore;
    
    public P2PPeer(String peerId, int port) {
        this.peerCore = new P2PPeerCore(peerId, port);
    }
    
    public P2PPeer(String peerId, int port, double initialValue) {
        this.peerCore = new P2PPeerCore(peerId, port, initialValue);
    }
    
    public P2PPeer(String peerId, int port, double initialValue, int networkSize) {
        this.peerCore = new P2PPeerCore(peerId, port, initialValue, networkSize);
    }
    
    public void start() throws Exception {
        peerCore.start();
    }
    
    public void stop() {
        peerCore.stop();
    }
    
    public void connectToPeer(String targetPeerId, String targetHost, int targetPort) {
        peerCore.connectToPeer(targetPeerId, targetHost, targetPort);
    }
    
    public static void main(String[] args) throws Exception {
        // Delegate to the core implementation
        P2PPeerCore.main(args);
    }
}