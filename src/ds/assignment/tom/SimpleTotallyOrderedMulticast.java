package ds.assignment.tom;

import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

public class SimpleTotallyOrderedMulticast {
    private final String peerId;
    private final LamportClock clock;
    private final PriorityBlockingQueue<Message> messageQueue;
    private final Set<String> peers;
    private final MessageProcessor processor;
    private final SecurityValidator validator;
    
    public interface MessageProcessor {
        void process(Message message);
    }
    
    public SimpleTotallyOrderedMulticast(String peerId, Set<String> peers, MessageProcessor processor) {
        this.peerId = peerId;
        this.clock = new LamportClock();
        this.messageQueue = new PriorityBlockingQueue<>();
        this.peers = new HashSet<>(peers);
        this.peers.add(peerId);
        this.processor = processor;
        this.validator = new SecurityValidator(peerId);
    }
    
    public synchronized Message createMessage(String content, long sequenceNumber) {
        long timestamp = clock.tick();
        return new Message(content, timestamp, peerId, sequenceNumber);
    }
    
    public synchronized void receiveMessage(Message message) {
        if (!validator.validateMessage(message, clock.getClock())) {
            System.err.printf("[%s] SECURITY ALERT: Invalid message from %s with timestamp %d%n", 
                             peerId, message.getSenderId(), message.getLamportTimestamp());
            return;
        }
        
        if (!message.getSenderId().equals(peerId)) {
            clock.update(message.getLamportTimestamp());
        }
        
        messageQueue.offer(message);
        tryDeliverMessages();
    }
    
    private void tryDeliverMessages() {
        Message nextMessage = messageQueue.peek();
        if (nextMessage != null) {
            messageQueue.poll();
            processor.process(nextMessage);
            
            if (!messageQueue.isEmpty()) {
                tryDeliverMessages();
            }
        }
    }
    
    public LamportClock getClock() {
        return clock;
    }
    
    public String getPeerId() {
        return peerId;
    }
}