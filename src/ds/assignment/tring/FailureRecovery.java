package ds.assignment.tring;

import java.io.*;
import java.net.*;

/**
 * Simple failure recovery for [EXTRA MARKS] using socket timeouts (60 lines)
 * Detects failed peers and attempts to skip them in the ring
 */
public class FailureRecovery {
    
    private final String peerId;
    private final String[] allPeers = {"localhost:8001", "localhost:8002", "localhost:8003", "localhost:8004", "localhost:8005"};
    private static final int CONNECTION_TIMEOUT = 3000; // 3 seconds
    
    public FailureRecovery(String peerId) {
        this.peerId = peerId;
    }
    
    /**
     * Try to forward token with failure recovery
     * If direct next peer fails, try the next one in ring
     */
    public boolean forwardTokenWithRecovery(String primaryNextPeer) {
        // Try primary next peer first
        if (tryForwardToken(primaryNextPeer)) {
            return true;
        }
        
        System.out.println(peerId + ": Primary peer " + primaryNextPeer + " failed, trying alternatives...");
        
        // Find our position and try next peers in ring
        int currentIndex = findPeerIndex(getPeerPort());
        for (int attempts = 1; attempts < allPeers.length; attempts++) {
            int nextIndex = (currentIndex + attempts) % allPeers.length;
            String nextPeerAddress = allPeers[nextIndex];
            
            // Skip self
            if (nextPeerAddress.endsWith(":" + getPeerPort())) {
                continue;
            }
            
            System.out.println(peerId + ": Trying alternative peer " + nextPeerAddress);
            if (tryForwardToken(nextPeerAddress)) {
                System.out.println(peerId + ": Successfully recovered - new next peer: " + nextPeerAddress);
                return true;
            }
        }
        
        System.err.println(peerId + ": All peers failed - regenerating token in 5 seconds");
        // Wait and regenerate token (simple recovery)
        try {
            Thread.sleep(5000);
            return tryForwardToken(allPeers[(currentIndex + 1) % allPeers.length]);
        } catch (InterruptedException e) {
            return false;
        }
    }
    
    /**
     * Attempt to forward token to specific peer with timeout
     */
    private boolean tryForwardToken(String peerAddress) {
        try {
            String[] parts = peerAddress.split(":");
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(parts[0], Integer.parseInt(parts[1])), CONNECTION_TIMEOUT);
            
            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                out.println("TOKEN");
                System.out.println(peerId + ": Successfully forwarded token to " + peerAddress);
                return true;
            } finally {
                socket.close();
            }
            
        } catch (Exception e) {
            System.out.println(peerId + ": Failed to forward to " + peerAddress + ": " + e.getMessage());
            return false;
        }
    }
    
    private int findPeerIndex(int port) {
        for (int i = 0; i < allPeers.length; i++) {
            if (allPeers[i].endsWith(":" + port)) {
                return i;
            }
        }
        return 0; // Default to first peer
    }
    
    private int getPeerPort() {
        // Extract port from peer ID (p1=8001, p2=8002, etc.)
        return 8000 + Integer.parseInt(peerId.substring(1));
    }
}