package ds.assignment.tom;

import ds.assignment.common.utils.PoissonGenerator;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatPeer {
    private final String peerId;
    private final int port;
    private final Map<String, String> peerAddresses;
    private final SimpleTotallyOrderedMulticast tom;
    private final WordDictionary dictionary;
    private final ExecutorService executor;
    private final List<String> printedWords;
    private long sequenceNumber;
    private volatile boolean running;
    
    public ChatPeer(String peerId, int port, Map<String, String> peerAddresses) {
        this.peerId = peerId;
        this.port = port;
        this.peerAddresses = new HashMap<>(peerAddresses);
        this.tom = new SimpleTotallyOrderedMulticast(peerId, peerAddresses.keySet(), this::processMessage);
        this.dictionary = new WordDictionary();
        this.executor = Executors.newCachedThreadPool();
        this.printedWords = Collections.synchronizedList(new ArrayList<>());
        this.sequenceNumber = 0;
        this.running = false;
    }
    
    public void start() {
        running = true;
        
        executor.submit(this::startServer);
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        
        executor.submit(this::wordGenerationLoop);
        
        System.out.printf("[%s] Peer started on port %d%n", peerId, port);
        System.out.printf("[%s] Known peers: %s%n", peerId, peerAddresses.keySet());
    }
    
    private void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.printf("[%s] Server listening on port %d%n", peerId, port);
            
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executor.submit(() -> handleClient(clientSocket));
                } catch (IOException e) {
                    if (running) {
                        System.err.printf("[%s] Error accepting connection: %s%n", peerId, e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.printf("[%s] Server error: %s%n", peerId, e.getMessage());
        }
    }
    
    private void handleClient(Socket clientSocket) {
        try (ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {
            Message message = (Message) in.readObject();
            tom.receiveMessage(message);
        } catch (IOException | ClassNotFoundException e) {
            System.err.printf("[%s] Error handling client: %s%n", peerId, e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.printf("[%s] Error closing client socket: %s%n", peerId, e.getMessage());
            }
        }
    }
    
    private void wordGenerationLoop() {
        try {
            while (running) {
                double interval = PoissonGenerator.getNextInterval(1.0 / 60.0);
                Thread.sleep((long) (interval * 1000));
                
                String word = dictionary.getRandomWord();
                Message message = tom.createMessage(word, ++sequenceNumber);
                
                System.out.printf("[%s] Generated word: '%s' with timestamp %d%n", 
                                 peerId, word, message.getLamportTimestamp());
                
                multicastMessage(message);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void multicastMessage(Message message) {
        tom.receiveMessage(message);
        
        for (Map.Entry<String, String> peer : peerAddresses.entrySet()) {
            String targetPeerId = peer.getKey();
            String address = peer.getValue();
            
            if (!targetPeerId.equals(peerId)) {
                executor.submit(() -> sendMessageToPeer(message, address));
            }
        }
    }
    
    private void sendMessageToPeer(Message message, String address) {
        String[] parts = address.split(":");
        String host = parts[0];
        int targetPort = Integer.parseInt(parts[1]);
        
        try (Socket socket = new Socket(host, targetPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            
            out.writeObject(message);
            out.flush();
            
        } catch (IOException e) {
            System.err.printf("[%s] Failed to send message to %s: %s%n", 
                             peerId, address, e.getMessage());
        }
    }
    
    private void processMessage(Message message) {
        synchronized (printedWords) {
            printedWords.add(message.getContent());
            System.out.printf("[%s] DELIVERED: '%s' (from %s, timestamp %d) - Total words: %d%n",
                             peerId, message.getContent(), message.getSenderId(), 
                             message.getLamportTimestamp(), printedWords.size());
        }
    }
    
    public void stop() {
        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public List<String> getPrintedWords() {
        synchronized (printedWords) {
            return new ArrayList<>(printedWords);
        }
    }
    
    public String getPeerId() {
        return peerId;
    }
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java ChatPeer <peer_id> <port> [peer1:host:port] [peer2:host:port] ...");
            System.exit(1);
        }
        
        String peerId = args[0];
        int port = Integer.parseInt(args[1]);
        
        Map<String, String> peerAddresses = new HashMap<>();
        for (int i = 2; i < args.length; i++) {
            String[] parts = args[i].split(":");
            if (parts.length == 3) {
                String peerName = parts[0];
                String address = parts[1] + ":" + parts[2];
                peerAddresses.put(peerName, address);
            }
        }
        
        ChatPeer peer = new ChatPeer(peerId, port, peerAddresses);
        
        Runtime.getRuntime().addShutdownHook(new Thread(peer::stop));
        
        peer.start();
        
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}