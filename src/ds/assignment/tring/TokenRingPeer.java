package ds.assignment.tring;

import ds.assignment.common.utils.PoissonGenerator;
import java.io.*;
import java.net.*;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Token Ring peer using raw sockets 
 */
public class TokenRingPeer {
    
    private final String peerId;
    private final int port;
    private final String nextPeerHost;
    private final int nextPeerPort;
    private final String calculatorHost;
    private final int calculatorPort;
    
    private final BlockingQueue<String> requestQueue = new LinkedBlockingQueue<>();
    private final Random random = new Random();
    private final SimpleFailureRecovery failureRecovery;
    private ServerSocket serverSocket;
    private volatile boolean running = true;
    
    public TokenRingPeer(String peerId, int port, String nextPeerAddress, String calculatorAddress) {
        this.peerId = peerId;
        this.port = port;
        
        String[] nextParts = nextPeerAddress.split(":");
        this.nextPeerHost = nextParts[0];
        this.nextPeerPort = Integer.parseInt(nextParts[1]);
        
        String[] calcParts = calculatorAddress.split(":");
        this.calculatorHost = calcParts[0];
        this.calculatorPort = Integer.parseInt(calcParts[1]);
        
        this.failureRecovery = new SimpleFailureRecovery(peerId);
    }
    
    public void start() throws Exception {
        serverSocket = new ServerSocket(port);
        System.out.println(peerId + " started on port " + port);
        
        // Start request generator thread
        new Thread(this::generateRequests).start();
        
        // Start token listener
        new Thread(this::listenForToken).start();
        
        // Initialize token if peer p1
        if ("p1".equals(peerId)) {
            Thread.sleep(2000); // Let other peers start
            forwardToken();
        }
    }
    
    private void generateRequests() {
        while (running) {
            try {
                double interval = PoissonGenerator.getNextInterval(4.0 / 60.0); // 4 per minute
                Thread.sleep((long) (interval * 1000));
                
                String[] ops = {"add", "sub", "mul", "div"};
                String op = ops[random.nextInt(ops.length)];
                double arg1 = random.nextDouble() * 100;
                double arg2 = random.nextDouble() * 100;
                if ("div".equals(op) && Math.abs(arg2) < 0.001) arg2 = 1.0;
                
                String request = String.format("%s:%.2f:%.2f", op, arg1, arg2);
                requestQueue.offer(request);
                System.out.println(peerId + ": Generated " + request);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private void listenForToken() {
        while (running) {
            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                
                String message = in.readLine();
                if ("TOKEN".equals(message)) {
                    System.out.println(peerId + ": Received token");
                    processRequests();
                    forwardToken();
                }
            } catch (Exception e) {
                if (running) System.err.println(peerId + ": Token listener error: " + e.getMessage());
            }
        }
    }
    
    private void processRequests() {
        while (!requestQueue.isEmpty()) {
            String request = requestQueue.poll();
            if (request != null) {
                try (Socket socket = new Socket(calculatorHost, calculatorPort);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    
                    out.println(request);
                    String response = in.readLine();
                    System.out.println(peerId + ": " + request + " = " + response);
                    
                } catch (Exception e) {
                    System.err.println(peerId + ": Calculator error: " + e.getMessage());
                }
            }
        }
    }
    
    private void forwardToken() {
        String primaryNext = nextPeerHost + ":" + nextPeerPort;
        
        // Try normal forwarding first, then use failure recovery
        try (Socket socket = new Socket(nextPeerHost, nextPeerPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            
            out.println("TOKEN");
            System.out.println(peerId + ": Forwarded token to " + primaryNext);
            
        } catch (Exception e) {
            System.err.println(peerId + ": Primary forward failed: " + e.getMessage());
            // Use failure recovery - [EXTRA MARKS]
            boolean recovered = failureRecovery.forwardTokenWithRecovery(primaryNext);
            if (!recovered) {
                System.err.println(peerId + ": Failed to recover - token may be lost");
            }
        }
    }
    
    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception e) {
            System.err.println("Error stopping peer: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Usage: java TokenRingPeer <peer-id> <port> <next-peer-address> <calculator-address>");
            System.err.println("Example: java TokenRingPeer p1 8001 localhost:8002 localhost:9000");
            System.exit(1);
        }
        
        TokenRingPeer peer = new TokenRingPeer(args[0], Integer.parseInt(args[1]), args[2], args[3]);
        
        Runtime.getRuntime().addShutdownHook(new Thread(peer::stop));
        
        peer.start();
        Thread.currentThread().join();
    }
}