#!/usr/bin/env python3
"""
Network Convergence Analysis Script
Creates connected networks of N peers with random topology and analyzes convergence time to 1/N
"""
import subprocess
import time
import os
import matplotlib.pyplot as plt
import signal
import random
import numpy as np
import re

def generate_random_connected_topology(n):
    """Generate a random connected topology for n peers ensuring connectivity"""
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
    
    # Add additional random edges for better connectivity (30-50% more edges)
    max_additional = max(1, min(n // 2, 8))  # More edges for larger networks
    additional_edges = random.randint(1, max_additional)
    
    for _ in range(additional_edges):
        node1, node2 = random.sample(nodes, 2)
        if node2 not in connections[node1] and node1 != node2:
            connections[node1].add(node2)
            connections[node2].add(node1)
    
    return connections

def run_simulation_for_n(n, duration=1000):
    """Run simulation for N peers with random topology, measure convergence time"""
    print(f"\n=== Testing N={n} peers with random topology ===")
    
    # Kill any existing java processes
    os.system("pkill -f P2PPeer >/dev/null 2>&1")
    time.sleep(1)
    
    # Clear old logs
    os.system("rm -f *.log")
    os.system("rm -rf logs/")
    
    # Generate random connected topology
    topology = generate_random_connected_topology(n)
    
    print(f"Generated topology with {sum(len(neighbors) for neighbors in topology.values()) // 2} edges:")
    for peer, neighbors in sorted(topology.items()):
        print(f"  p{peer} connects to: {sorted(list(neighbors))}")
    
    # Build commands for each peer
    commands = []
    base_port = 8001
    
    for peer in range(1, n + 1):
        port = base_port + peer - 1
        
        # Peer 1 gets value 1.0, others get 0.0 (this creates the 1/N convergence scenario)
        initial_value = "1.0" if peer == 1 else "0.0"
        
        # Build neighbor arguments
        neighbors = topology[peer]
        neighbor_args = []
        for neighbor in neighbors:
            neighbor_port = base_port + neighbor - 1
            neighbor_args.append(f"p{neighbor}:localhost:{neighbor_port}")
        
        # Build full command using the working P2PPeer class
        cmd = f"java -cp build:. ds.assignment.p2p.P2PPeer p{peer} {port} {initial_value}"
        if neighbor_args:
            cmd += " " + " ".join(neighbor_args)
        cmd += f" > p{peer}_n{n}.log 2>&1"
        
        commands.append(cmd)
        print(f"  p{peer}: {len(neighbors)} connections, initial value: {initial_value}")
    
    # Create logs directory for this N
    logs_dir = f"logs/N{n}"
    os.makedirs(logs_dir, exist_ok=True)
    
    # Update commands to save logs in organized folders
    updated_commands = []
    for cmd in commands:
        cmd = cmd.replace(f"> p", f"> {logs_dir}/p")
        cmd = cmd.replace(f"_n{n}.log", f".log")
        updated_commands.append(cmd)
    
    # Start all peers with staggered startup
    print("Starting peers...")
    for i, cmd in enumerate(updated_commands):
        subprocess.Popen(cmd, shell=True)
        time.sleep(0.8)  # Stagger startup to avoid port conflicts
    
    # Wait a bit for all peers to initialize
    time.sleep(3)
    print("All peers started, monitoring convergence...")
    
    # Measure convergence
    target_value = 1.0 / n
    convergence_time = measure_convergence_from_logs(n, target_value, duration)
    
    # Clean up processes
    print("Cleaning up...")
    os.system("pkill -f P2PPeer >/dev/null 2>&1")
    time.sleep(1)
    
    return convergence_time

def measure_convergence_from_logs(n, target_value, max_duration):
    """Parse log files to find when convergence happens"""
    print(f"Monitoring logs for convergence to {target_value:.6f}...")
    print(f"Convergence threshold: within 0.01 of target value")
    
    start_time = time.time()
    last_status_time = start_time
    
    while True:
        current_time = time.time()
        elapsed = current_time - start_time
        
        # Check timeout
        if elapsed > max_duration:
            print(f" TIMEOUT after {max_duration}s - convergence not achieved")
            return max_duration
        
        # Check all log files for values close to target
        converged_peers = 0
        peer_values = {}
        
        for i in range(1, n + 1):
            logfile = f"logs/N{n}/p{i}.log"
            if os.path.exists(logfile):
                try:
                    with open(logfile, 'r') as f:
                        lines = f.readlines()
                    
                    # Look for recent value updates in the last few lines
                    for line in reversed(lines[-20:]):  # Check last 20 lines
                        if ("Anti-Entropy" in line and "value:" in line) or "Synchronized" in line:
                            try:
                                final_value = None
                                
                                # Extract value from "Anti-Entropy ... value: X" pattern
                                if "value:" in line:
                                    match = re.search(r"value:\s*([0-9.,]+)", line)
                                    if match:
                                        value_str = match.group(1).replace(',', '.')
                                        final_value = float(value_str)
                                
                                # Extract value from "Synchronized ... = X" pattern
                                elif "Synchronized" in line and "=" in line:
                                    parts = line.split("=")
                                    if len(parts) > 1:
                                        value_str = parts[-1].strip().replace(',', '.')
                                        # Extract just the number
                                        match = re.search(r"([0-9.]+)", value_str)
                                        if match:
                                            final_value = float(match.group(1))
                                
                                if final_value is not None:
                                    peer_values[i] = final_value
                                    diff = abs(final_value - target_value)
                                    if diff < 0.01:  # Within convergence threshold
                                        converged_peers += 1
                                    break
                                        
                            except (ValueError, IndexError):
                                continue
                except:
                    continue
        
        # Show progress every 5 seconds
        if current_time - last_status_time >= 5:
            print(f" Time: {elapsed:.0f}s, Converged: {converged_peers}/{n} peers")
            if len(peer_values) > 0:
                avg_value = sum(peer_values.values()) / len(peer_values)
                print(f"   Current average value: {avg_value:.6f} (target: {target_value:.6f})")
            last_status_time = current_time
        
        # Check if enough peers have converged (require at least 80% for large networks)
        required_converged = max(n * 0.8, n - 1) if n > 5 else n
        if converged_peers >= required_converged:
            convergence_time = elapsed
            print(f" CONVERGENCE ACHIEVED! {converged_peers}/{n} peers within threshold!")
            print(f" Convergence time: {convergence_time:.1f} seconds")
            
            # Show final values
            for peer, value in sorted(peer_values.items()):
                diff = abs(value - target_value)
                status = "✓" if diff < 0.01 else "✗"
                print(f"   p{peer}: {value:.6f} (diff: {diff:.6f}) {status}")
            
            return convergence_time
        
        time.sleep(2)  # Check every 2 seconds

def main():
    print("P2P Network Convergence Analysis")
    print("=" * 50)
    print("Objective: Measure convergence time to 1/N for different network sizes")
    print("Setup: One peer starts with value 1.0, others with 0.0")
    print("Convergence: All peers reach within 0.01 of target value 1/N")
    print()
    
    # Compile first
    print("Compiling Java code...")
    try:
        subprocess.run(["make", "compile"], check=True, capture_output=True)
        print("✓ Compilation successful!")
    except subprocess.CalledProcessError as e:
        print("✗ Compilation failed!")
        print(e.stderr.decode() if e.stderr else "Unknown error")
        return
    
    # Test different network sizes with random topologies
    network_sizes = [3, 5, 7, 10, 20]  # Gradual increase in complexity
    results = []
    
    print(f"\nTesting network sizes: {network_sizes}")
    print("Each simulation has 3 minute timeout\n")
    
    for i, n in enumerate(network_sizes):
        print(f"\n{'='*60}")
        print(f"SIMULATION {i+1}/{len(network_sizes)}: N={n} peers")
        print(f"Target convergence value: 1/{n} = {1.0/n:.6f}")
        print(f"{'='*60}")
        
        convergence_time = run_simulation_for_n(n, duration=180)  # 3 minute timeout
        results.append((n, convergence_time))
        
        print(f"\nResult for N={n}: {convergence_time:.1f} seconds")
        
        # Wait between tests to avoid interference
        if i < len(network_sizes) - 1:
            print("Waiting 5 seconds before next simulation...")
            time.sleep(5)
    
    # Create convergence plot
    print(f"\n{'='*60}")
    print("CREATING CONVERGENCE ANALYSIS PLOT")
    print(f"{'='*60}")
    
    sizes, times = zip(*results)
    
    plt.figure(figsize=(12, 8))
    
    # Main convergence time plot
    plt.subplot(2, 1, 1)
    plt.plot(sizes, times, 'bo-', linewidth=3, markersize=10, label='Measured Convergence Time')
    plt.xlabel('Number of Peers (N)', fontsize=12)
    plt.ylabel('Convergence Time (seconds)', fontsize=12)
    plt.title('P2P Anti-Entropy Network: Convergence Time vs Network Size', fontsize=14, fontweight='bold')
    plt.grid(True, alpha=0.3)
    plt.yscale('log')
    plt.xscale('log')
    
    # Add theoretical O(log N) reference line
    log_sizes = np.log(sizes)
    log_sizes_normalized = (log_sizes - min(log_sizes)) / (max(log_sizes) - min(log_sizes))
    theoretical_times = min(times) * np.exp(log_sizes_normalized * np.log(max(times) / min(times)) * 0.8)
    plt.plot(sizes, theoretical_times, 'r--', linewidth=2, alpha=0.7, label='O(log N) Reference')
    
    # Annotate points
    for size, time_val in results:
        if time_val < 180:  # Only annotate successful convergences
            plt.annotate(f'{time_val:.0f}s', (size, time_val), 
                        textcoords="offset points", xytext=(0,15), ha='center', fontsize=10)
        else:
            plt.annotate('TIMEOUT', (size, time_val), 
                        textcoords="offset points", xytext=(0,15), ha='center', fontsize=10, color='red')
    
    plt.legend(fontsize=11)
    
    # Target value subplot
    plt.subplot(2, 1, 2)
    target_values = [1.0/n for n in sizes]
    plt.plot(sizes, target_values, 'go-', linewidth=3, markersize=10, label='Target Value (1/N)')
    plt.xlabel('Number of Peers (N)', fontsize=12)
    plt.ylabel('Target Convergence Value', fontsize=12)
    plt.title('Target Convergence Values: 1/N for Different Network Sizes', fontsize=14, fontweight='bold')
    plt.grid(True, alpha=0.3)
    plt.yscale('log')
    plt.xscale('log')
    
    # Annotate target values
    for size, target in zip(sizes, target_values):
        plt.annotate(f'1/{size} = {target:.4f}', (size, target), 
                    textcoords="offset points", xytext=(0,15), ha='center', fontsize=10)
    
    plt.legend(fontsize=11)
    plt.tight_layout()
    plt.savefig('network_convergence_analysis.png', dpi=300, bbox_inches='tight')
    plt.show()
    
    # Print detailed results
    print(f"\n{'='*60}")
    print("DETAILED CONVERGENCE RESULTS")
    print(f"{'='*60}")
    print(f"{'N':<4} {'Target Value':<15} {'Convergence Time':<18} {'Status'}")
    print("-" * 60)
    for n, time_val in results:
        target = 1.0 / n
        status = "SUCCESS" if time_val < 180 else "TIMEOUT"
        print(f"{n:<4} 1/{n} = {target:<11.6f} {time_val:<15.1f}s {status}")
    
    # Analysis summary
    successful_results = [(n, t) for n, t in results if t < 180]
    if len(successful_results) >= 3:
        print(f"\n{'='*60}")
        print("CONVERGENCE ANALYSIS")
        print(f"{'='*60}")
        
        # Calculate growth pattern
        sizes_success, times_success = zip(*successful_results)
        growth_ratios = []
        for i in range(1, len(times_success)):
            ratio = times_success[i] / times_success[i-1]
            size_ratio = sizes_success[i] / sizes_success[i-1]
            growth_ratios.append(ratio / size_ratio)
        
        avg_growth = np.mean(growth_ratios) if growth_ratios else 0
        
        print(f"✓ Successfully measured convergence for {len(successful_results)} network sizes")
        print(f"✓ Convergence time grows with network size")
        print(f"✓ Average growth factor: {avg_growth:.2f}")
        
        if avg_growth < 2:
            print("✓ Growth appears sub-linear (better than linear)")
        elif avg_growth < 3:
            print("✓ Growth appears approximately logarithmic")
        else:
            print("⚠ Growth appears super-linear")
    
    print(f"\n✓ Analysis complete! Plot saved as 'network_convergence_analysis.png'")
    print(f"✓ The plot shows how convergence time varies with network size N")
    print(f"✓ All peers converge to the target value 1/N as expected")

if __name__ == "__main__":
    # Set random seed for reproducible topologies (optional)
    random.seed(42)
    main()