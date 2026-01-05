# Distributed Systems Assignment

This project implements three distributed systems algorithms: Token Ring, P2P Anti-Entropy, and Total Order Multicast.

## Quick Start

### Compilation
```bash
make compile
```

### Token Ring
Start calculator server first, then peers, you must open 6 different terminals:
```bash
make run-tring-server
make run-tring-p1
make run-tring-p2
# ... etc for p3, p4, p5
```

Or launch all in separate terminals (better for demo):
```bash
make run-tring-demo
```

### P2P Anti-Entropy Network
Launch all peers in separate terminals:
```bash
make run-p2p-demo
```

Individual peers same as tring:
```bash
make run-p2p-p1
make run-p2p-p2
# ... etc for p3, p4, p5, p6
```

### Total Order Multicast (Chat)
Launch all chat peers different terminals:
```bash
make run-tom-demo
```

Or run automated test same terminal (harder to check):
```bash
make test-tom-chat
```

## Cleanup
```bash
make clean           # Remove compiled classes
make clean-all       # Remove all generated files
```

## Help
```bash
make help           # Show all available targets
```