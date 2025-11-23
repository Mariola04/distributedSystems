# Distributed Systems Assignment 2025/26 FCUP

Implementation of three distributed systems scenarios using Java and raw sockets.

## Assignment Overview

This project implements three fundamental distributed systems concepts:

### 1. Token Ring Algorithm (Mutual Exclusion)
- **5 peers** (p1-p5) connected in a ring topology
- **1 calculator server** providing arithmetic operations (add, sub, mul, div)
- **Token-based mutual exclusion** for server access
- **Poisson request generation** (4 requests/minute per peer)
- **[EXTRA MARKS]** Failure detection and recovery using socket timeouts

### 2. P2P Data Aggregation (TBD)
- 6 peers in mesh topology
- Anti-entropy value synchronization
- Network size estimation

### 3. Total Order Multicast Chat (TBD)
- 6 peers with Lamport clocks
- Totally-ordered message delivery
- Distributed chat application

## Technology Stack

- **Language:** Java 17
- **Communication:** Raw TCP sockets
- **Build System:** Makefile (no Maven dependency)
- **Failure Detection:** Socket timeout monitoring
- **Random Generation:** Professor's Poisson process implementation

## Quick Start

### Prerequisites
- Java 17+

### 1. Build Project
```bash
# Compile Java files
make compile
```

### 2. Run Token Ring Example
```bash
# Terminal 1: Start calculator server
make run-tring-server

# Terminal 2-6: Start peers (in separate terminals)
make run-tring-p1
make run-tring-p2
make run-tring-p3
make run-tring-p4
make run-tring-p5
```

## Project Structure

```
src/ds/assignment/
├── common/                    # Shared utilities
│   └── utils/
│       └── PoissonGenerator.java  # Professor's Poisson implementation
├── tring/                    # Token Ring Implementation
│   ├── TokenRingPeer.java    # Main peer implementation
│   ├── CalculatorServer.java # Socket-based calculator server
│   └── SimpleFailureRecovery.java # [EXTRA MARKS] Recovery strategies
├── p2p/                      # P2P Network (TODO)
└── tom/                      # Total Order Multicast (TODO)
```

## Implementation Status

**Token Ring Algorithm - COMPLETED**
- DONE: **Project Structure** - Clean separation of concerns  
- DONE: **Poisson Generator** - Based on professor's implementation
- DONE: **Build System** - Makefile with socket-based compilation
- DONE: **Token Ring Core** - Full peer implementation
- DONE: **Failure Recovery** - [EXTRA MARKS] Socket timeout recovery
- DONE: **Testing Suite** - Comprehensive automated tests

**Next Steps**
- TODO: **P2P Network** - Not started
- TODO: **Total Order Multicast** - Not started

## Key Features

### Token Ring Algorithm
- **Mutual Exclusion:** Only token holder can access calculator
- **Random Requests:** Poisson-distributed calculator operations  
- **Socket Communication:** Simple, reliable TCP connections
- **Failure Resilience:** Socket timeout detection and recovery
- **Ring Reconfiguration:** Alternative peer forwarding

### Testing Suite
```bash
make test-all           # Run all tests
make test-basic         # 30-second basic functionality
make test-failure       # 50-second failure recovery [EXTRA MARKS]
make test-calculator    # Calculator operations verification
make test-poisson       # Poisson distribution validation
```

## Development Commands

```bash
# Build commands
make compile          # Compile Java sources
make clean           # Remove build files
make rebuild         # Clean + compile

# Token Ring
make run-tring-server    # Calculator server (port 9000)
make run-tring-p1        # Peer p1 (port 8001)
make run-tring-p2        # Peer p2 (port 8002)
make run-tring-p3        # Peer p3 (port 8003)
make run-tring-p4        # Peer p4 (port 8004)
make run-tring-p5        # Peer p5 (port 8005)

# Testing
make test-all         # Run all token ring tests
make test-basic       # Basic functionality (30s)
make test-failure     # Failure recovery [EXTRA MARKS] (50s)
make test-calculator  # Calculator operations
make test-poisson     # Poisson generation

# Utilities
make help            # Show all available commands
make check-sources   # Verify source files exist
```

## Architecture Decisions

### Why Raw Sockets over gRPC?
- **Simplicity** - No complex dependencies or protobuf compilation
- **Professor's approach** - Matches calculatormulti example exactly  
- **Minimal setup** - Just Java, no additional tools required
- **Clear debugging** - Easy to trace socket connections

### Socket-Based Failure Detection
- **Timeout strategy** - Connect timeouts detect failed peers
- **Alternative routing** - Try next available peer in ring
- **Token regeneration** - Create new token if all peers fail
- **[EXTRA MARKS]** - Robust failure recovery implementation

### File Organization
- **Single responsibility** - Each class has one clear purpose
- **Minimal files** - Small, focused implementations (50-100 lines)
- **Professor's style** - Follows provided examples closely

## Assignment References

Based on practical assignment requirements:
- **Textbook:** van Steen & Tanenbaum - Distributed Systems
- **Token Ring:** Chapter 6, "Token Ring Algorithm"
- **Professor examples:** Located in `professor/` folder
- **Deadline:** January 5th, 2026

---

**Author:** Mario Minhava
**Course:** Distributed Systems 2025/26 - FCUP