package ds.assignment.tom;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SecurityValidator {
    private final String peerId;
    private final Map<String, Long> lastSeenTimestamp;
    private final Map<String, Long> expectedNextTimestamp;
    
    public SecurityValidator(String peerId) {
        this.peerId = peerId;
        this.lastSeenTimestamp = new ConcurrentHashMap<>();
        this.expectedNextTimestamp = new ConcurrentHashMap<>();
    }
    
    public boolean validateMessage(Message message, long currentClock) {
        String senderId = message.getSenderId();
        long messageTimestamp = message.getLamportTimestamp();
        
        if (senderId.equals(peerId)) {
            return true;
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
        
        lastSeenTimestamp.put(senderId, messageTimestamp);
        expectedNextTimestamp.put(senderId, messageTimestamp + 1);
        
        return true;
    }
    
    private boolean isRewritingHistory(String senderId, long timestamp) {
        Long lastSeen = lastSeenTimestamp.get(senderId);
        return lastSeen != null && timestamp <= lastSeen;
    }
    
    private boolean isWritingInFuture(String senderId, long timestamp, long currentClock) {
        final long MAX_CLOCK_DRIFT = 10;
        return timestamp > currentClock + MAX_CLOCK_DRIFT;
    }
}