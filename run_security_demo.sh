#!/bin/bash

echo "=== Security Demo: Malicious Peer Detection ==="
cd "$(dirname "$0")"

# Compile first
echo "Compiling..."
mkdir -p build
javac -cp build -d build src/ds/assignment/tom/*.java src/ds/assignment/common/utils/*.java

if [ $? -ne 0 ]; then
    echo "Compilation failed!"
    exit 1
fi

echo "Compilation successful!"
echo ""

# Create the malicious attack scripts
cat > attack_history.java << 'EOF'
import ds.assignment.tom.*;
import java.io.*;
import java.net.*;

public class attack_history {
    public static void main(String[] args) throws Exception {
        System.out.println("=== MALICIOUS PEER: History Rewriting Attack ===");
        System.out.println("Waiting for network to establish...");
        Thread.sleep(8000);
        
        // Send legitimate message first
        Message legitMsg = new Message("LEGITIMATE", 15, "ATTACKER", 1);
        System.out.printf("✅ Sending legitimate message: '%s' timestamp=%d%n", 
                         legitMsg.getContent(), legitMsg.getLamportTimestamp());
        sendMessage(legitMsg, 8081);
        Thread.sleep(2000);
        
        // Now attack with old timestamp
        System.out.println("\n🚨 LAUNCHING HISTORY REWRITING ATTACK...");
        Message attackMsg1 = new Message("REWRITE-HISTORY", 5, "ATTACKER", 2);
        System.out.printf("⚠️  Attacking with OLD timestamp: '%s' timestamp=%d (should be REJECTED)%n", 
                         attackMsg1.getContent(), attackMsg1.getLamportTimestamp());
        sendMessage(attackMsg1, 8081);
        
        Thread.sleep(1000);
        
        // Try duplicate timestamp
        Message attackMsg2 = new Message("DUPLICATE-TIME", 15, "ATTACKER", 3);
        System.out.printf("⚠️  Attacking with DUPLICATE timestamp: '%s' timestamp=%d (should be REJECTED)%n", 
                         attackMsg2.getContent(), attackMsg2.getLamportTimestamp());
        sendMessage(attackMsg2, 8081);
        
        System.out.println("\n🔍 Check other terminals for 'SECURITY BREACH' messages!");
        System.out.println("Press Ctrl+C to stop this attack terminal");
        Thread.sleep(Long.MAX_VALUE);
    }
    
    static void sendMessage(Message msg, int port) {
        try (Socket socket = new Socket("localhost", port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(msg);
            out.flush();
        } catch (Exception e) {
            System.out.println("❌ Send failed: " + e.getMessage());
        }
    }
}
EOF

cat > attack_future.java << 'EOF'
import ds.assignment.tom.*;
import java.io.*;
import java.net.*;

public class attack_future {
    public static void main(String[] args) throws Exception {
        System.out.println("=== MALICIOUS PEER: Future Writing Attack ===");
        System.out.println("Waiting for network to establish...");
        Thread.sleep(10000);
        
        System.out.println("\n🚨 LAUNCHING FUTURE WRITING ATTACK...");
        
        Message futureMsg1 = new Message("FUTURE-HACK-1", 999999, "EVIL-PEER", 1);
        System.out.printf("⚠️  Attacking with FUTURE timestamp: '%s' timestamp=%d%n", 
                         futureMsg1.getContent(), futureMsg1.getLamportTimestamp());
        
        // Attack all peers
        sendMessage(futureMsg1, 8081);
        sendMessage(futureMsg1, 8082);
        sendMessage(futureMsg1, 8083);
        
        Thread.sleep(2000);
        
        Message futureMsg2 = new Message("TIME-TRAVELER", 888888, "EVIL-PEER", 2);
        System.out.printf("⚠️  Another future attack: '%s' timestamp=%d%n", 
                         futureMsg2.getContent(), futureMsg2.getLamportTimestamp());
        
        sendMessage(futureMsg2, 8081);
        sendMessage(futureMsg2, 8082);
        
        System.out.println("\n🔍 Check other terminals for 'SECURITY BREACH' messages!");
        System.out.println("Press Ctrl+C to stop this attack terminal");
        Thread.sleep(Long.MAX_VALUE);
    }
    
    static void sendMessage(Message msg, int port) {
        try (Socket socket = new Socket("localhost", port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(msg);
            out.flush();
        } catch (Exception e) {
            System.out.println("❌ Send failed: " + e.getMessage());
        }
    }
}
EOF

# Compile attack scripts
javac -cp build attack_history.java attack_future.java

echo "🚀 Launching Security Demo in separate terminals..."
echo ""

# Launch normal chat peers
echo "📱 Opening normal chat peers..."
xterm -T "Chat Peer p1 (Normal)" -e "java -cp build ds.assignment.tom.ChatPeer p1 8081 p2:localhost:8082 p3:localhost:8083; bash" &
sleep 1

xterm -T "Chat Peer p2 (Normal)" -e "java -cp build ds.assignment.tom.ChatPeer p2 8082 p1:localhost:8081 p3:localhost:8083; bash" &
sleep 1

xterm -T "Chat Peer p3 (Normal)" -e "java -cp build ds.assignment.tom.ChatPeer p3 8083 p1:localhost:8081 p2:localhost:8082; bash" &
sleep 1

# Launch attack terminals
echo "🚨 Opening attack terminals..."
xterm -T "MALICIOUS: History Rewriting Attack" -fg red -bg black -e "java -cp build:. attack_history; bash" &
sleep 1

xterm -T "MALICIOUS: Future Writing Attack" -fg red -bg black -e "java -cp build:. attack_future; bash" &

echo ""
echo "=== SECURITY DEMO LAUNCHED ==="
echo ""
echo "👀 WHAT TO WATCH FOR:"
echo "   • Normal peers exchange words normally"
echo "   • Red attack terminals will launch attacks after ~8-10 seconds"
echo "   • Look for 'SECURITY BREACH' messages in normal peer terminals"
echo "   • Malicious messages should be REJECTED, not delivered"
echo ""
echo "🎯 EXPECTED BEHAVIOR:"
echo "   ✅ Future attacks: Should see 'SECURITY BREACH: writing in future'"
echo "   ✅ History attacks: Should see 'SECURITY BREACH: rewriting history'"
echo "   ✅ Normal messages: Continue to be delivered in order"
echo ""
echo "🛑 Close all terminals when done observing"
echo "   Use 'killall xterm' to close all at once"

# Cleanup function
cleanup() {
    echo "Cleaning up..."
    rm -f attack_history.java attack_history.class attack_future.java attack_future.class
}

trap cleanup EXIT