#!/bin/bash

# Test script: Poisson Request Generation
# Verifies that requests follow Poisson distribution (4 per minute)

echo "=== Testing Poisson Request Generation ==="
cd "$(dirname "$0")/.."

# Cleanup function
cleanup() {
    echo "Cleaning up processes..."
    pkill -f "CalculatorServer"
    pkill -f "TokenRingPeer"
    wait
}

trap cleanup EXIT

# Start calculator server
echo "Starting calculator server..."
make run-tring-server &
SERVER_PID=$!
sleep 2

# Start one peer to monitor request generation
echo "Starting single peer p1 to monitor request generation..."
echo "Expected: ~4 requests per minute (1 every 15 seconds on average)"
make run-tring-p1 &
P1_PID=$!

echo "Monitoring for 60 seconds..."
echo "Count the 'Generated' messages - should be around 4 requests"
echo "Intervals should vary (not exactly 15 seconds each)"

# Let it run for 1 minute
sleep 60

echo "Poisson generation test completed!"
echo "Manual verification:"
echo "1. Count 'Generated' messages (should be ~4)"
echo "2. Check intervals vary (Poisson distribution)"
echo "3. Operations should be random (add, sub, mul, div)"