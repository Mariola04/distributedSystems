package ds.assignment.p2p;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles all network communication for P2P peers
 * Including registration, synchronization, and connection management
 */
public class P2PNetworking {
    
    private final String peerId;
    private final int port;
    private final Map<String, String> neighbors = new ConcurrentHashMap<>();
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    
    public P2PNetworking(String peerId, int port) {
        this.peerId = peerId;
        this.port = port;
    }
    
    public void startServer() throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.printf("%s: Started on port %d, waiting for peers to register%n", peerId, port);
    }
    
    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception e) {
            System.err.printf("%s: Error stopping server: %s%n", peerId, e.getMessage());
        }
    }
    
    /**
     * Server loop handling incoming connections
     */
    public void handleIncomingConnections(P2PPeerCore peerCore) {
        while (running) {
            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
                
                String message = in.readLine();
                if (message != null) {
                    String[] parts = message.split(" ");
                    
                    if ("REGISTER".equals(parts[0]) && parts.length == 3) {
                        handleRegistration(parts[1], parts[2], out);
                    } else if ("SYNC".equals(parts[0]) && parts.length == 2) {
                        handleSynchronization(parts[1], out, peerCore);
                    }
                }
            } catch (Exception e) {
                if (running) System.err.printf("%s: Server error: %s%n", peerId, e.getMessage());
            }
        }
    }
    
    private void handleRegistration(String neighborId, String neighborAddress, PrintWriter out) {
        neighbors.put(neighborId, neighborAddress);
        System.out.printf("%s: Peer %s registered from %s%n", peerId, neighborId, neighborAddress);
        out.println("REGISTERED");
    }
    
    private void handleSynchronization(String valueStr, PrintWriter out, P2PPeerCore peerCore) {
        double neighborValue = Double.parseDouble(valueStr);
        double newValue = peerCore.synchronizeValues(neighborValue);
        System.out.printf("%s: Synchronized with neighbor: %.6f%n", peerId, newValue);
        out.println(String.valueOf(newValue));
    }
    
    /**
     * Connect to another peer and register ourselves
     */
    public boolean connectToPeer(String targetPeerId, String targetHost, int targetPort) {
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
                return true;
            }
        } catch (Exception e) {
            System.err.printf("%s: Failed to connect to %s: %s%n", peerId, targetPeerId, e.getMessage());
        }
        return false;
    }
    
    /**
     * Synchronize with a specific neighbor
     */
    public double synchronizeWith(String neighborId, String neighborAddress, double currentValue) {
        try {
            String[] addressParts = neighborAddress.split(":");
            String host = addressParts[0];
            int neighborPort = Integer.parseInt(addressParts[1]);
            
            try (Socket socket = new Socket(host, neighborPort);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                
                out.println("SYNC " + currentValue);
                String response = in.readLine();
                if (response != null) {
                    double newValue = Double.parseDouble(response);
                    System.out.printf("%s: Anti-Entropy with %s: %.6f -> %.6f%n", peerId, neighborId, currentValue, newValue);
                    return newValue;
                }
            }
        } catch (Exception e) {
            System.err.printf("%s: Failed to sync with %s: %s%n", peerId, neighborId, e.getMessage());
        }
        return currentValue; // Return unchanged if sync failed
    }
    
    public Map<String, String> getNeighbors() {
        return neighbors;
    }
    
    public boolean isRunning() {
        return running;
    }
}