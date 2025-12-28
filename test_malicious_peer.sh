#!/bin/bash

echo "=== Testing Malicious Peer Detection ==="
cd "$(dirname "$0")"

# Compile first
echo "Compiling..."
mkdir -p build
javac -cp build -d build src/ds/assignment/tom/*.java src/ds/assignment/common/utils/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo ""
echo "=== Test 1: History Rewriting Attack ==="
echo "Starting 3 normal peers..."

# Start normal peers in background
java -cp build ds.assignment.tom.ChatPeer p1 8081 p2:localhost:8082 p3:localhost:8083 > /tmp/p1.log 2>&1 &
P1_PID=$!

java -cp build ds.assignment.tom.ChatPeer p2 8082 p1:localhost:8081 p3:localhost:8083 > /tmp/p2.log 2>&1 &
P2_PID=$!

java -cp build ds.assignment.tom.ChatPeer p3 8083 p1:localhost:8081 p2:localhost:8082 > /tmp/p3.log 2>&1 &
P3_PID=$!

echo "Waiting for peers to start and exchange some messages..."
sleep 3

# Create a simple Java program to send malicious messages
cat > MaliciousTest.java << 'EOF'
import ds.assignment.tom.*;
import java.io.*;
import java.net.*;

public class MaliciousTest {
    public static void main(String[] args) throws Exception {
        System.out.println("[MALICIOUS] Starting history rewriting attack...");
        
        // Wait for peers to start and generate some legitimate messages
        Thread.sleep(5000);
        
        // First send a legitimate message to establish history
        Message legitMsg = new Message("LEGIT-MESSAGE", 10, "MALICIOUS", 1);
        System.out.printf("[MALICIOUS] Sending legitimate message: %s with timestamp %d%n", 
                         legitMsg.getContent(), legitMsg.getLamportTimestamp());
        sendMaliciousMessage(legitMsg, "localhost", 8081);
        
        Thread.sleep(2000);
        
        // Now try to rewrite history with an older timestamp
        Message maliciousMsg1 = new Message("REWRITE-HISTORY", 5, "MALICIOUS", 2);
        System.out.printf("[MALICIOUS] ATTACK: Trying to rewrite history with timestamp %d (should be rejected)%n", 
                         maliciousMsg1.getLamportTimestamp());
        sendMaliciousMessage(maliciousMsg1, "localhost", 8081);
        
        Thread.sleep(1000);
        
        // Try another history rewrite with same timestamp as before
        Message maliciousMsg2 = new Message("DUPLICATE-TIMESTAMP", 10, "MALICIOUS", 3);
        System.out.printf("[MALICIOUS] ATTACK: Trying to reuse timestamp %d (should be rejected)%n", 
                         maliciousMsg2.getLamportTimestamp());
        sendMaliciousMessage(maliciousMsg2, "localhost", 8081);
        
        System.out.println("[MALICIOUS] History rewriting attack completed - check peers for security alerts");
    }
    
    static void sendMaliciousMessage(Message msg, String host, int port) {
        try (Socket socket = new Socket(host, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            
            System.out.printf("[MALICIOUS] Sending: %s with timestamp %d%n", 
                             msg.getContent(), msg.getLamportTimestamp());
            out.writeObject(msg);
            out.flush();
            
        } catch (Exception e) {
            System.out.println("[MALICIOUS] Failed to send: " + e.getMessage());
        }
    }
}
EOF

# Compile and run malicious test
javac -cp build MaliciousTest.java
echo ""
echo "Now launching history rewriting attack..."
java -cp build:. MaliciousTest

echo ""
echo "=== History Attack Results ==="
echo "Checking peer p1 logs for security alerts..."
grep -i "security\|malicious" /tmp/p1.log || echo "No security alerts found in p1 logs"

sleep 2

echo ""
echo "=== Test 2: Future Writing Attack ==="

# Create future writing attack
cat > FutureAttack.java << 'EOF'
import ds.assignment.tom.*;
import java.io.*;
import java.net.*;

public class FutureAttack {
    public static void main(String[] args) throws Exception {
        System.out.println("[MALICIOUS] Starting future writing attack...");
        
        // Create messages with timestamps far in the future
        Message futureMsg1 = new Message("FUTURE-HACK-1", 99999999, "EVIL-PEER", 1);
        Message futureMsg2 = new Message("FUTURE-HACK-2", 99999998, "EVIL-PEER", 2);
        
        // Try to send to multiple peers
        sendMaliciousMessage(futureMsg1, "localhost", 8081);
        sendMaliciousMessage(futureMsg1, "localhost", 8082);
        Thread.sleep(1000);
        sendMaliciousMessage(futureMsg2, "localhost", 8083);
        
        System.out.println("[MALICIOUS] Future attack completed - check for security alerts");
    }
    
    static void sendMaliciousMessage(Message msg, String host, int port) {
        try (Socket socket = new Socket(host, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            
            System.out.printf("[MALICIOUS] Sending FUTURE message: %s with timestamp %d%n", 
                             msg.getContent(), msg.getLamportTimestamp());
            out.writeObject(msg);
            out.flush();
            
        } catch (Exception e) {
            System.out.println("[MALICIOUS] Failed to send: " + e.getMessage());
        }
    }
}
EOF

javac -cp build FutureAttack.java
echo ""
echo "Now launching future writing attack..."
java -cp build:. FutureAttack

echo ""
echo "=== Future Attack Results ==="
echo "Checking all peer logs for security alerts..."
grep -i "security\|evil" /tmp/p*.log || echo "No security alerts found"

echo ""
echo "=== Waiting 5 seconds to observe behavior ==="
sleep 5

echo ""
echo "=== Final Results Summary ==="
echo ""
echo "=== P1 Log (last 10 lines) ==="
tail -10 /tmp/p1.log

echo ""
echo "=== All Security Alerts ==="
grep -i "security\|breach" /tmp/p*.log || echo "No security alerts detected"

echo ""
echo "=== Cleaning up ==="
kill $P1_PID $P2_PID $P3_PID 2>/dev/null
rm -f MaliciousTest.java MaliciousTest.class FutureAttack.java FutureAttack.class /tmp/p*.log

echo ""
echo "=== Test Complete ==="
echo "✅ Look for 'SECURITY BREACH' messages above"
echo "✅ History rewriting: Should reject timestamps ≤ previously seen ones"
echo "✅ Future writing: Should reject timestamps far in the future"