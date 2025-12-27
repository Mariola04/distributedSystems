package ds.assignment.p2p;

import ds.assignment.common.utils.PoissonGenerator;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * P2P peer that implements Anti-Entropy algorithm for data aggregation
 * Following PDF specifications exactly
 */
public class P2PPeer {
    
    private final String peerId;
    private final int port;
    private final Map<String, String> neighbors = new ConcurrentHashMap<>();
    private volatile double currentValue;
    private final Random random = new Random();
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    private final long startTime = System.currentTimeMillis();
    private volatile boolean converged = false;
    private final double convergenceThreshold;
    private final double targetValue;
    
    public P2PPeer(String peerId, int port) {
        this.peerId = peerId;
        this.port = port;
        // Generate initial random value in range (0,1)
        this.currentValue = random.nextDouble();
        this.targetValue = -1; // Unknown for random start
        this.convergenceThreshold = 0.01;
        System.out.printf("%s: Initial value = %.6f%n", peerId, currentValue);
    }
    
    public P2PPeer(String peerId, int port, double initialValue) {
        this.peerId = peerId;
        this.port = port;
        this.currentValue = initialValue;
        this.targetValue = -1; // Will be set by setNetworkSize
        this.convergenceThreshold = 0.01;
        System.out.printf("%s: Initial value = %.6f%n", peerId, initialValue);
    }
    
    public P2PPeer(String peerId, int port, double initialValue, int networkSize) {
        this.peerId = peerId;
        this.port = port;
        this.currentValue = initialValue;
        this.targetValue = 1.0 / networkSize;
        this.convergenceThreshold = 0.01;
        System.out.printf("%s: Initial value = %.6f, target = %.6f%n", peerId, initialValue, targetValue);
    }
    
    public void start() throws Exception {
        serverSocket = new ServerSocket(port);
        System.out.printf("%s: Started on port %d, waiting for peers to register%n", peerId, port);
        
        // Thread waiting for others to connect and register themselves
        new Thread(this::serverLoop).start();
        
        // Anti-Entropy synchronization thread
        new Thread(this::antiEntropyLoop).start();
    }
    
    private void serverLoop() {
        while (running) {
            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                
                String message = in.readLine();
                if (message != null) {
                    String[] parts = message.split(" ");
                    
                    if ("REGISTER".equals(parts[0]) && parts.length == 3) {
                        // Peer registration: REGISTER <peer-id> <host:port>
                        String neighborId = parts[1];
                        String neighborAddress = parts[2];
                        neighbors.put(neighborId, neighborAddress);
                        System.out.printf("%s: Peer %s registered from %s%n", peerId, neighborId, neighborAddress);
                        out.println("REGISTERED");
                        
                    } else if ("SYNC".equals(parts[0]) && parts.length == 2) {
                        // Value synchronization: SYNC <value>
                        double neighborValue = Double.parseDouble(parts[1]);
                        double oldValue = currentValue;
                        currentValue = (currentValue + neighborValue) / 2.0;
                        System.out.printf("%s: Synchronized %.6f + %.6f = %.6f%n", peerId, oldValue, neighborValue, currentValue);
                        checkConvergence();
                        out.println(String.valueOf(currentValue));
                    }
                }
            } catch (Exception e) {
                if (running) System.err.printf("%s: Server error: %s%n", peerId, e.getMessage());
            }
        }
    }
    
    private void antiEntropyLoop() {
        while (running) {
            try {
                // Poisson distribution: 2 events per minute = 2/60 per second
                double interval = PoissonGenerator.getNextInterval(2.0 / 60.0);
                Thread.sleep((long) (interval * 1000));
                
                if (!neighbors.isEmpty()) {
                    // Choose random neighbor from map
                    String[] neighborIds = neighbors.keySet().toArray(new String[0]);
                    String randomNeighbor = neighborIds[random.nextInt(neighborIds.length)];
                    String neighborAddress = neighbors.get(randomNeighbor);
                    
                    synchronizeWith(randomNeighbor, neighborAddress);
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private void synchronizeWith(String neighborId, String neighborAddress) {
        try {
            String[] addressParts = neighborAddress.split(":");
            String host = addressParts[0];
            int neighborPort = Integer.parseInt(addressParts[1]);
            
            try (Socket socket = new Socket(host, neighborPort);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                
                double oldValue = currentValue;
                out.println("SYNC " + currentValue);
                String response = in.readLine();
                if (response != null) {
                    double newValue = Double.parseDouble(response);
                    currentValue = newValue;
                    System.out.printf("%s: Anti-Entropy with %s: %.6f -> %.6f%n", peerId, neighborId, oldValue, newValue);
                    checkConvergence();
                }
            }
        } catch (Exception e) {
            System.err.printf("%s: Failed to sync with %s: %s%n", peerId, neighborId, e.getMessage());
        }
    }
    
    private void checkConvergence() {
        if (targetValue > 0 && !converged) {
            double diff = Math.abs(currentValue - targetValue);
            if (diff < convergenceThreshold) {
                converged = true;
                long convergenceTime = System.currentTimeMillis() - startTime;
                System.out.printf("CONVERGENCE: %s reached target %.6f in %d ms%n", peerId, targetValue, convergenceTime);
                
                // Write to convergence log file
                try {
                    java.io.FileWriter fw = new java.io.FileWriter("convergence.log", true);
                    fw.write(String.format("%s,%d,%.6f,%.6f,%d%n", 
                            peerId, System.currentTimeMillis(), currentValue, targetValue, convergenceTime));
                    fw.close();
                } catch (Exception e) {
                    // Ignore file errors
                }
            }
        }
    }
    
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception e) {
            System.err.printf("%s: Error stopping: %s%n", peerId, e.getMessage());
        }
    }
    
    /**
     * Connect to another peer and add them to local neighbor map
     */
    public void connectToPeer(String targetPeerId, String targetHost, int targetPort) {
        try (Socket socket = new Socket(targetHost, targetPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            String myAddress = "localhost:" + port;
            out.println("REGISTER " + peerId + " " + myAddress);
            String response = in.readLine();
            
            if ("REGISTERED".equals(response)) {
                String targetAddress = targetHost + ":" + targetPort;
                neighbors.put(targetPeerId, targetAddress);
                System.out.printf("%s: Successfully connected to %s at %s:%d%n", peerId, targetPeerId, targetHost, targetPort);
            }
        } catch (Exception e) {
            System.err.printf("%s: Failed to connect to %s: %s%n", peerId, targetPeerId, e.getMessage());
        }
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java P2PPeer <peer-id> <port> [initial-value] [network-size] [target-peers...]");
            System.err.println("Example: java P2PPeer p2 8002 0.0 6 p1:localhost:8001 p3:localhost:8003");
            System.exit(1);
        }
        
        String peerId = args[0];
        int port = Integer.parseInt(args[1]);
        
        P2PPeer peer;
        int targetStartIndex;
        
        if (args.length > 3 && !args[2].contains(":") && !args[3].contains(":")) {
            // Third argument is initial value, fourth is network size
            double initialValue = Double.parseDouble(args[2]);
            int networkSize = Integer.parseInt(args[3]);
            peer = new P2PPeer(peerId, port, initialValue, networkSize);
            targetStartIndex = 4;
        } else if (args.length > 2 && !args[2].contains(":")) {
            // Third argument is initial value
            double initialValue = Double.parseDouble(args[2]);
            peer = new P2PPeer(peerId, port, initialValue);
            targetStartIndex = 3;
        } else {
            // No initial value, use random
            peer = new P2PPeer(peerId, port);
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