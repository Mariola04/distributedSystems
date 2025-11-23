#!/bin/bash

# Test script: Calculator Server functionality
# Tests all operations independently

echo "=== Testing Calculator Server ==="
cd "$(dirname "$0")/.."

# Cleanup function
cleanup() {
    echo "Cleaning up calculator server..."
    pkill -f "CalculatorServer"
    wait
}

trap cleanup EXIT

# Start calculator server
echo "Starting calculator server..."
make run-tring-server &
SERVER_PID=$!
sleep 2

echo "Testing calculator operations..."

# Test function
test_operation() {
    local op=$1
    local arg1=$2
    local arg2=$3
    local expected=$4
    
    echo "Testing: $op:$arg1:$arg2"
    result=$(echo "$op:$arg1:$arg2" | nc localhost 9000)
    echo "Result: $result"
    echo "Expected: $expected"
    echo "---"
}

# Test all operations
test_operation "add" "5.5" "2.3" "7.8"
test_operation "sub" "10.0" "3.0" "7.0" 
test_operation "mul" "4.0" "2.5" "10.0"
test_operation "div" "15.0" "3.0" "5.0"

echo "Calculator server test completed!"
echo "Verify results manually above."