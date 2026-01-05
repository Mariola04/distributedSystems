package ds.assignment.tom;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityValidator {
    private final String peerId;
    private final Map<String, Long> lastSeenTimestamp;
    private final Map<String, Long> expectedNextTimestamp;
    private final Set<Long> usedTimestamps;
    
    public SecurityValidator(String peerId) {
        this.peerId = peerId;
        this.lastSeenTimestamp = new ConcurrentHashMap<>();
        this.expectedNextTimestamp = new ConcurrentHashMap<>();
        this.usedTimestamps = ConcurrentHashMap.newKeySet();
    }
    
    public boolean validateMessage(Message message, long currentClock) {
        String senderId = message.getSenderId();
        long messageTimestamp = message.getLamportTimestamp();
        
        if (senderId.equals(peerId)) {
            return true;
        }
        
        // Check if this timestamp has already been used by ANY peer
        if (usedTimestamps.contains(messageTimestamp)) {
            System.err.printf("SECURITY BREACH: Peer %s attempting to reuse timestamp %d%n",
                             senderId, messageTimestamp);
            return false;
        }
        
        if (isRewritingHistory(senderId, messageTimestamp)) {
            System.err.printf("SECURITY BREACH: Peer %s attempting to rewrite history with timestamp %d%n",
                             senderId, messageTimestamp);
            return false;
        }
        
        if (isWritingInFuture(senderId, messageTimestamp, currentClock)) {
            System.err.printf("SECURITY BREACH: Peer %s writing in future with timestamp %d (current: %d)%n",
                             senderId, messageTimestamp, currentClock);
            return false;
        }
        
        // Record this timestamp as used
        usedTimestamps.add(messageTimestamp);
        lastSeenTimestamp.put(senderId, messageTimestamp);
        expectedNextTimestamp.put(senderId, messageTimestamp + 1);
        
        return true;
    }
    
    private boolean isRewritingHistory(String senderId, long timestamp) {
        Long lastSeen = lastSeenTimestamp.get(senderId);
        // Check if sender is trying to send a message with timestamp <= their last seen timestamp
        return lastSeen != null && timestamp <= lastSeen;
    }
    
    private boolean isWritingInFuture(String senderId, long timestamp, long currentClock) {
        final long MAX_CLOCK_DRIFT = 5;
        return timestamp > currentClock + MAX_CLOCK_DRIFT;
    }
}