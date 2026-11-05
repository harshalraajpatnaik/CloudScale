# Infrastructure Architecture & Simulation — CloudScale

## 1. Simulation Philosophy
In Phase 1–8, CloudScale operates on developer workstations using Docker containers and simulated infrastructure nodes. 
- **Honesty in Representation**: All simulated infrastructure nodes are explicitly marked with `SIMULATED` status badges in the database and console.
- **Realistic Telemetry**: The node agent generates telemetry adhering to realistic diurnal curves (sinusoidal workload shifts reflecting work-hour peaks), random Poisson-distributed burst events, and memory accumulation curves simulating memory leaks.
- **Physical Decoupling**: The control plane interacts with the node agent through standardized REST contracts, ensuring the agent can be replaced by real KVM/libvirt or Kubernetes node daemons in future extensions without altering the control plane core.

## 2. Simulated Node Agent Architecture
The `node-agent/` service:
- Initializes $N$ virtual nodes (e.g., `worker-01`, `worker-02`, `worker-03`) with configured CPU cores and memory limits.
- Binds to simulated ports and maintains a background asyncio loop reporting heartbeats every 30 seconds.
- Computes aggregated utilization based on the actual scheduled workloads reported by the control plane plus background noise.

## 3. Extensibility Roadmap (Future Work)
- **Kubernetes Integration**: Replace the simulator with a Custom Resource Definition (CRD) controller mapping CloudScale workloads to Kubernetes Pods.
- **KVM / Libvirt Integration**: Map CloudScale workloads to QEMU/KVM virtual machines managed via the libvirt API.
- **Cloud Provider Drivers**: Plug in AWS EC2, GCP Compute Engine, and Azure VM drivers implementing the `NodeProvider` SPI.
