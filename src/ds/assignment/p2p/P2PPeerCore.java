package ds.assignment.p2p;

import java.util.Random;

/**
 * Core P2P peer logic coordinating networking, anti-entropy, and convergence monitoring
 * Main entry point for P2P peer functionality
 */
public class P2PPeerCore {
    
    private final String peerId;
    private final int port;
    private volatile double currentValue;
    private final Random random = new Random();
    
    private final P2PNetworking networking;
    private final AntiEntropyManager antiEntropy;
    private final ConvergenceMonitor convergenceMonitor;
    
    public P2PPeerCore(String peerId, int port) {
        this.peerId = peerId;
        this.port = port;
        // Generate initial random value in range (0,1)
        this.currentValue = random.nextDouble();
        
        this.networking = new P2PNetworking(peerId, port);
        this.antiEntropy = new AntiEntropyManager(peerId, networking);
        this.convergenceMonitor = new ConvergenceMonitor(peerId, -1, 0.01); // No target for random start
        
        System.out.printf("%s: Initial value = %.6f%n", peerId, currentValue);
    }
    
    public P2PPeerCore(String peerId, int port, double initialValue) {
        this.peerId = peerId;
        this.port = port;
        this.currentValue = initialValue;
        
        this.networking = new P2PNetworking(peerId, port);
        this.antiEntropy = new AntiEntropyManager(peerId, networking);
        this.convergenceMonitor = new ConvergenceMonitor(peerId, -1, 0.01); // Target will be set by setNetworkSize
        
        System.out.printf("%s: Initial value = %.6f%n", peerId, initialValue);
    }
    
    public P2PPeerCore(String peerId, int port, double initialValue, int networkSize) {
        this.peerId = peerId;
        this.port = port;
        this.currentValue = initialValue;
        
        this.networking = new P2PNetworking(peerId, port);
        this.antiEntropy = new AntiEntropyManager(peerId, networking);
        
        double targetValue = 1.0 / networkSize;
        this.convergenceMonitor = new ConvergenceMonitor(peerId, targetValue, 0.01);
        
        System.out.printf("%s: Initial value = %.6f, target = %.6f%n", peerId, initialValue, targetValue);
    }
    
    public void start() throws Exception {
        networking.startServer();
        
        // Start server thread for handling incoming connections
        new Thread(() -> networking.handleIncomingConnections(this)).start();
        
        // Start anti-entropy synchronization thread
        new Thread(() -> antiEntropy.runAntiEntropyLoop(this)).start();
    }
    
    public void stop() {
        antiEntropy.stop();
        networking.stopServer();
    }
    
    /**
     * Synchronize values using averaging algorithm
     */
    public synchronized double synchronizeValues(double neighborValue) {
        double oldValue = currentValue;
        currentValue = (currentValue + neighborValue) / 2.0;
        System.out.printf("%s: Synchronized %.6f + %.6f = %.6f%n", peerId, oldValue, neighborValue, currentValue);
        convergenceMonitor.checkConvergence(currentValue);
        return currentValue;
    }
    
    /**
     * Update current value and check convergence
     */
    public synchronized void updateValue(double newValue) {
        currentValue = newValue;
        convergenceMonitor.checkConvergence(currentValue);
    }
    
    /**
     * Connect to another peer
     */
    public boolean connectToPeer(String targetPeerId, String targetHost, int targetPort) {
        return networking.connectToPeer(targetPeerId, targetHost, targetPort);
    }
    
    public synchronized double getCurrentValue() {
        return currentValue;
    }
    
    public String getPeerId() {
        return peerId;
    }
    
    public int getPort() {
        return port;
    }
    
    public ConvergenceMonitor getConvergenceMonitor() {
        return convergenceMonitor;
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java P2PPeerCore <peer-id> <port> [initial-value] [network-size] [target-peers...]");
            System.err.println("Example: java P2PPeerCore p2 8002 0.0 6 p1:localhost:8001 p3:localhost:8003");
            System.exit(1);
        }
        
        String peerId = args[0];
        int port = Integer.parseInt(args[1]);
        
        P2PPeerCore peer;
        int targetStartIndex;
        
        if (args.length > 3 && !args[2].contains(":") && !args[3].contains(":")) {
            // Third argument is initial value, fourth is network size
            double initialValue = Double.parseDouble(args[2]);
            int networkSize = Integer.parseInt(args[3]);
            peer = new P2PPeerCore(peerId, port, initialValue, networkSize);
            targetStartIndex = 4;
        } else if (args.length > 2 && !args[2].contains(":")) {
            // Third argument is initial value
            double initialValue = Double.parseDouble(args[2]);
            peer = new P2PPeerCore(peerId, port, initialValue);
            targetStartIndex = 3;
        } else {
            // No initial value, use random
            peer = new P2PPeerCore(peerId, port);
            targetStartIndex = 2;
        }
        
        Runtime.getRuntime().addShutdownHook(new Thread(peer::stop));
        
        peer.start();
        
        // Wait a bit for server to start
        Thread.sleep(2000);
        
        // Connect to specified target peers (network topology)
        for (int i = targetStartIndex; i < args.length; i++) {
            String[] targetParts = args[i].split(":");
            if (targetParts.length == 3) {
                String targetPeerId = targetParts[0];
                String targetHost = targetParts[1];
                int targetPort = Integer.parseInt(targetParts[2]);
                
                peer.connectToPeer(targetPeerId, targetHost, targetPort);
                Thread.sleep(500); // Small delay between connections
            }
        }
        
        Thread.currentThread().join();
    }
}