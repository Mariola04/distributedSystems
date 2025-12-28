package ds.assignment.tom;

import java.io.Serializable;

public class Message implements Serializable, Comparable<Message> {
    private final String content;
    private final long lamportTimestamp;
    private final String senderId;
    private final long sequenceNumber;
    
    public Message(String content, long lamportTimestamp, String senderId, long sequenceNumber) {
        this.content = content;
        this.lamportTimestamp = lamportTimestamp;
        this.senderId = senderId;
        this.sequenceNumber = sequenceNumber;
    }
    
    public String getContent() {
        return content;
    }
    
    public long getLamportTimestamp() {
        return lamportTimestamp;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public long getSequenceNumber() {
        return sequenceNumber;
    }
    
    @Override
    public int compareTo(Message other) {
        int timestampComparison = Long.compare(this.lamportTimestamp, other.lamportTimestamp);
        if (timestampComparison != 0) {
            return timestampComparison;
        }
        return this.senderId.compareTo(other.senderId);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Message message = (Message) obj;
        return lamportTimestamp == message.lamportTimestamp && 
               senderId.equals(message.senderId) &&
               sequenceNumber == message.sequenceNumber;
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(lamportTimestamp) ^ senderId.hashCode() ^ Long.hashCode(sequenceNumber);
    }
    
    @Override
    public String toString() {
        return String.format("Message{content='%s', timestamp=%d, sender='%s', seq=%d}", 
                            content, lamportTimestamp, senderId, sequenceNumber);
    }
}