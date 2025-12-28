package ds.assignment.tom;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestChatApplication {
    private static final int BASE_PORT = 8080;
    private static final int NUM_PEERS = 6;
    private static final int TEST_DURATION_SECONDS = 30;
    
    public static void main(String[] args) {
        System.out.println("=== Testing Chat Application with Totally-Ordered Multicast ===");
        
        Map<String, String> peerAddresses = new HashMap<>();
        for (int i = 1; i <= NUM_PEERS; i++) {
            peerAddresses.put("p" + i, "localhost:" + (BASE_PORT + i));
        }
        
        List<ChatPeer> peers = new ArrayList<>();
        CountDownLatch startLatch = new CountDownLatch(NUM_PEERS);
        
        for (int i = 1; i <= NUM_PEERS; i++) {
            String peerId = "p" + i;
            int port = BASE_PORT + i;
            
            Map<String, String> otherPeers = new HashMap<>(peerAddresses);
            otherPeers.remove(peerId);
            
            ChatPeer peer = new ChatPeer(peerId, port, otherPeers);
            peers.add(peer);
            
            new Thread(() -> {
                peer.start();
                startLatch.countDown();
            }).start();
        }
        
        try {
            startLatch.await();
            System.out.println("All peers started. Running test for " + TEST_DURATION_SECONDS + " seconds...");
            
            Thread.sleep(TEST_DURATION_SECONDS * 1000);
            
            System.out.println("\n=== Test Results ===");
            
            List<List<String>> allWordLists = new ArrayList<>();
            for (ChatPeer peer : peers) {
                List<String> words = peer.getPrintedWords();
                allWordLists.add(words);
                System.out.printf("%s printed %d words: %s%n", 
                                 peer.getPeerId(), words.size(), 
                                 words.size() > 10 ? words.subList(0, 10) + "..." : words);
            }
            
            boolean allSame = true;
            if (!allWordLists.isEmpty()) {
                List<String> reference = allWordLists.get(0);
                for (int i = 1; i < allWordLists.size(); i++) {
                    if (!reference.equals(allWordLists.get(i))) {
                        allSame = false;
                        break;
                    }
                }
            }
            
            System.out.printf("\n=== RESULT: %s ===\n", 
                             allSame ? "SUCCESS - All peers have identical word sequences" : 
                                     "FAILURE - Word sequences differ between peers");
            
            if (!allSame) {
                System.out.println("Detailed comparison:");
                for (int i = 0; i < allWordLists.size(); i++) {
                    System.out.printf("p%d: %s%n", i + 1, allWordLists.get(i));
                }
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            for (ChatPeer peer : peers) {
                peer.stop();
            }
        }
    }
}