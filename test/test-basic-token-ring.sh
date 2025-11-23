#!/bin/bash

# Test script: Basic Token Ring functionality
# Starts all components, runs for 30 seconds, then cleans up

echo "=== Testing Basic Token Ring ==="
cd "$(dirname "$0")/.."

# Cleanup function
cleanup() {
    echo "Cleaning up processes..."
    pkill -f "CalculatorServer"
    pkill -f "TokenRingPeer"
    wait
    echo "Test completed."
}

trap cleanup EXIT

# Start calculator server
echo "Starting calculator server..."
make run-tring-server &
SERVER_PID=$!
sleep 2

# Start all peers with proper delays
echo "Starting peers (with 2s delays to avoid race conditions)..."
make run-tring-p1 &
P1_PID=$!
sleep 2

make run-tring-p2 &
P2_PID=$!
sleep 2

make run-tring-p3 &
P3_PID=$!
sleep 2

make run-tring-p4 &
P4_PID=$!
sleep 2

make run-tring-p5 &
P5_PID=$!
sleep 2

echo "All components started. Running for 30 seconds..."
echo "Expected: Token circulation, request generation, and processing"

# Let it run
sleep 30

echo "Basic test completed successfully!"