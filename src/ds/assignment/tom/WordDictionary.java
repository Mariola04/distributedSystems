package ds.assignment.tom;

import java.util.*;

public class WordDictionary {
    private final List<String> words;
    private final Random random;
    
    public WordDictionary() {
        this.words = Arrays.asList(
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
        this.random = new Random();
    }
    
    public String getRandomWord() {
        return words.get(random.nextInt(words.size()));
    }
    
    public void setSeed(long seed) {
        random.setSeed(seed);
    }
}