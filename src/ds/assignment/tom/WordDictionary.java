package ds.assignment.tom;

import java.util.*;
import java.io.*;
import java.nio.file.*;

public class WordDictionary {
    private final List<String> words;
    private final Random random;
    
    public WordDictionary() {
        this.words = loadWordsFromFile();
        this.random = new Random();
    }
    
    private List<String> loadWordsFromFile() {
        List<String> loadedWords = new ArrayList<>();
        
        // Try multiple possible locations for the dictionary file
        String[] possiblePaths = {
            "src/ds/assignment/tom/dictionary.txt",
            "dictionary.txt",
            "./src/ds/assignment/tom/dictionary.txt",
            "./dictionary.txt"
        };
        
        for (String path : possiblePaths) {
            try {
                Path filePath = Paths.get(path);
                if (Files.exists(filePath)) {
                    loadedWords = Files.readAllLines(filePath);
                    System.out.println("WordDictionary: Loaded " + loadedWords.size() + " words from " + path);
                    break;
                }
            } catch (IOException e) {
                // Continue to next path
                continue;
            }
        }
        
        // Fallback to predefined words if file loading fails
        if (loadedWords.isEmpty()) {
            System.err.println("WordDictionary: Could not load dictionary.txt, using predefined words");
            loadedWords = Arrays.asList(
                "hello", "world", "distributed", "systems", "chat", "message", "peer", "network",
                "algorithm", "consensus", "multicast", "lamport", "clock", "timestamp", "order",
                "synchronization", "protocol", "communication", "process", "event", "causal",
                "concurrent", "parallel", "thread", "socket", "server", "client", "connection",
                "reliability", "fault", "tolerance", "byzantine", "agreement", "coordination",
                "election", "leader", "follower", "state", "machine", "replication", "consistency",
                "partition", "availability", "performance", "scalability", "latency", "throughput",
                "bandwidth", "overhead", "optimization", "efficiency", "deadlock", "livelock",
                "starvation", "fairness", "progress", "safety", "security", "authentication",
                "authorization", "encryption", "integrity", "confidentiality", "privacy"
            );
        }
        
        return loadedWords;
    }
    
    public String getRandomWord() {
        return words.get(random.nextInt(words.size()));
    }
    
    public void setSeed(long seed) {
        random.setSeed(seed);
    }
}