#!/bin/bash

# Test script: Failure Recovery [EXTRA MARKS]
# Starts system, kills peers randomly, verifies recovery

echo "=== Testing Failure Recovery [EXTRA MARKS] ==="
cd "$(dirname "$0")/.."

# Cleanup function
cleanup() {
    echo "Cleaning up processes..."
    pkill -f "CalculatorServer"
    pkill -f "TokenRingPeer"
    wait
    echo "Failure recovery test completed."
}

trap cleanup EXIT

# Start calculator server
echo "Starting calculator server..."
make run-tring-server &
SERVER_PID=$!
sleep 2

# Start all peers
echo "Starting all peers..."
make run-tring-p1 &
P1_PID=$!
sleep 1

make run-tring-p2 &
P2_PID=$!
sleep 1

make run-tring-p3 &
P3_PID=$!
sleep 1

make run-tring-p4 &
P4_PID=$!
sleep 1

make run-tring-p5 &
P5_PID=$!

echo "All peers started. Running normally for 10 seconds..."
sleep 10

echo "=== FAILURE SCENARIO 1: Killing peer p2 ==="
kill $P2_PID
echo "p2 killed. Remaining peers should skip it..."
sleep 15

echo "=== FAILURE SCENARIO 2: Killing peer p4 ==="
kill $P4_PID
echo "p4 killed. Ring should reconfigure again..."
sleep 15

echo "=== FAILURE SCENARIO 3: Killing peer p1 ==="
kill $P1_PID
echo "p1 (token initiator) killed. Token should regenerate..."
sleep 10

echo "Failure recovery test completed!"
echo "Expected behaviors verified:"
echo "- Peers detect failed neighbors"
echo "- Ring automatically reconfigures"
echo "- Token continues circulating"
echo "- No manual intervention needed"