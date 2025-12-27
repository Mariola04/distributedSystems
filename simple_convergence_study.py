#!/usr/bin/env python3
"""
Simple convergence study - just run makefile targets and measure time to convergence
"""
import subprocess
import time
import os
import matplotlib.pyplot as plt
import signal
import random

def generate_random_connected_topology(n):
    """Generate a random connected topology for n peers"""
    # Create adjacency list representation
    connections = {i: set() for i in range(1, n + 1)}
    
    # Start with a spanning tree to ensure connectivity
    nodes = list(range(1, n + 1))
    remaining = nodes[1:]
    connected = [nodes[0]]
    
    while remaining:
        # Connect random remaining node to random connected node
        new_node = random.choice(remaining)
        existing_node = random.choice(connected)
        
        connections[new_node].add(existing_node)
        connections[existing_node].add(new_node)
        
        remaining.remove(new_node)
        connected.append(new_node)
    
    # Add some random additional edges (but not too many)
    max_additional = min(n // 2, 5)  # Limit to avoid too dense graphs
    additional_edges = random.randint(0, max_additional)
    
    for _ in range(additional_edges):
        node1, node2 = random.sample(nodes, 2)
        if node2 not in connections[node1] and node1 != node2:
            connections[node1].add(node2)
            connections[node2].add(node1)
    
    return connections

def run_simulation_for_n(n, duration=120):
    """Run simulation for N peers with random topology, measure convergence time"""
    print(f"\n=== Testing N={n} peers with random topology ===")
    
    # Clear old logs
    os.system("rm -f *.log")
    
    # Generate random connected topology
    topology = generate_random_connected_topology(n)
    
    print(f"Generated topology:")
    for peer, neighbors in topology.items():
        print(f"  p{peer} connects to: {sorted(list(neighbors))}")
    
    # Build commands for each peer
    commands = []
    base_port = 8001
    
    for peer in range(1, n + 1):
        port = base_port + peer - 1
        
        # Peer 1 gets value 1.0, others get 0.0
        initial_value = "1.0" if peer == 1 else "0.0"
        
        # Build neighbor arguments
        neighbors = topology[peer]
        neighbor_args = []
        for neighbor in neighbors:
            neighbor_port = base_port + neighbor - 1
            neighbor_args.append(f"p{neighbor}:localhost:{neighbor_port}")
        
        # Build full command
        cmd = f"java -cp build:. ds.assignment.p2p.P2PPeer p{peer} {port} {initial_value}"
        if neighbor_args:
            cmd += " " + " ".join(neighbor_args)
        cmd += f" > p{peer}_n{n}.log 2>&1"
        
        commands.append(cmd)
        print(f"  p{peer}: {len(neighbors)} connections")
    
    # Create logs directory for this N
    logs_dir = f"logs/N{n}"
    os.makedirs(logs_dir, exist_ok=True)
    
    # Update commands to save logs in organized folders
    updated_commands = []
    for cmd in commands:
        # Replace "p1_n3.log" with "logs/N3/p1.log"
        old_log = f"_n{n}.log"
        new_log = f".log"
        cmd = cmd.replace(f"> p", f"> {logs_dir}/p")
        cmd = cmd.replace(old_log, new_log)
        updated_commands.append(cmd)
    
    # Start all peers
    for cmd in updated_commands:
        subprocess.Popen(cmd, shell=True)
        time.sleep(1)
    
    # Measure convergence
    target_value = 1.0 / n
    return measure_convergence_from_logs(n, target_value, duration)

def measure_convergence_from_logs(n, target_value, max_duration):
    """Parse log files to find when convergence happens"""
    print(f"Monitoring logs for convergence to {target_value:.6f}...")
    print(f"Waiting for all peers to be within 1 decimal point (0.1) of target...")
    
    start_time = time.time()
    
    while True:  # No timeout - wait for real convergence
        # Check all log files for values close to target
        converged_peers = 0
        
        for i in range(1, n + 1):
            logfile = f"logs/N{n}/p{i}.log"
            if os.path.exists(logfile):
                try:
                    with open(logfile, 'r') as f:
                        lines = f.readlines()
                        
                    # Look for recent value updates
                    for line in reversed(lines[-10:]):  # Check last 10 lines
                        if "Anti-Entropy" in line or "Synchronized" in line:
                            try:
                                final_value = None
                                
                                # Extract the final value (after "->")
                                if "->" in line:
                                    parts = line.split("->")
                                    if len(parts) > 1:
                                        value_str = parts[1].strip()
                                        # Handle comma decimal separator
                                        value_str = value_str.replace(',', '.')
                                        final_value = float(value_str)
                                        
                                # Extract value from "Synchronized" lines (after "=")
                                elif "Synchronized" in line and "=" in line:
                                    parts = line.split("=")
                                    if len(parts) > 1:
                                        value_str = parts[1].strip()
                                        # Handle comma decimal separator
                                        value_str = value_str.replace(',', '.')
                                        final_value = float(value_str)
                                
                                if final_value is not None:
                                    diff = abs(final_value - target_value)
                                    if diff < 0.1:  # Within 1 decimal point
                                        converged_peers += 1
                                        print(f"  p{i}: {final_value:.6f} (target: {target_value:.6f}, diff: {diff:.6f}) ✓")
                                        break
                                        
                            except Exception as e:
                                pass
                except:
                    pass
        
        # Check if ALL peers have converged (strict requirement)
        if converged_peers >= n:
            convergence_time = time.time() - start_time
            print(f"🎉 FULL CONVERGENCE ACHIEVED! All {n} peers within 1 decimal point!")
            print(f"⏱️  Convergence time: {convergence_time:.1f} seconds")
            
            # Kill all java processes
            os.system("pkill -f P2PPeer")
            time.sleep(2)
            
            return convergence_time
        
        # Show progress
        elapsed = time.time() - start_time
        print(f"⏳ Time: {elapsed:.0f}s, Converged: {converged_peers}/{n} peers")
        
        time.sleep(5)  # Check every 5 seconds

def main():
    print("Simple P2P Convergence Study")
    print("============================")
    
    # Compile first
    subprocess.run(["make", "compile"], check=True)
    
    # Test different network sizes with random topologies
    network_sizes = [3, 4, 7, 10]  # Full EXTRA MARKS requirement
    results = []
    
    for n in network_sizes:
        convergence_time = run_simulation_for_n(n, duration=90)
        results.append((n, convergence_time))
        
        print(f"N={n}: {convergence_time:.1f} seconds")
        
        # Wait between tests
        time.sleep(10)
    
    # Create simple plot
    sizes, times = zip(*results)
    
    plt.figure(figsize=(8, 5))
    plt.plot(sizes, times, 'bo-', linewidth=2, markersize=8)
    plt.xlabel('Network Size (N)')
    plt.ylabel('Convergence Time (seconds)')
    plt.title('P2P Anti-Entropy Convergence Time')
    plt.grid(True)
    
    for size, time_val in results:
        plt.annotate(f'{time_val:.1f}s', (size, time_val), 
                    textcoords="offset points", xytext=(0,10), ha='center')
    
    plt.savefig('simple_convergence_plot.png', dpi=150)
    plt.show()
    
    print("\nResults:")
    for n, time_val in results:
        print(f"N={n}: {time_val:.1f} seconds (target: 1/{n} = {1.0/n:.6f})")

if __name__ == "__main__":
    main()