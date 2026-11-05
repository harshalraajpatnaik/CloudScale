import math
import random
import time
from typing import Dict, Any

class NodeTelemetrySimulator:
    """
    Simulates realistic host telemetry:
    - Diurnal workload cycle (sinusoidal day/night shift)
    - Occasional Poisson-distributed metric spikes
    - Memory accumulation simulating real workloads
    """
    def __init__(self, hostname: str, total_cpu: int = 16, total_mem_mb: int = 32768):
        self.hostname = hostname
        self.total_cpu = total_cpu
        self.total_mem_mb = total_mem_mb
        self.allocated_cpu = 0
        self.allocated_mem_mb = 0
        self.active_workloads = 0
        self.start_time = time.time()

    def update_allocation(self, allocated_cpu: int, allocated_mem_mb: int, workload_count: int):
        self.allocated_cpu = min(allocated_cpu, self.total_cpu)
        self.allocated_mem_mb = min(allocated_mem_mb, self.total_mem_mb)
        self.active_workloads = workload_count

    def sample_metrics(self) -> Dict[str, Any]:
        elapsed_hours = (time.time() - self.start_time) / 3600.0
        # Diurnal base wave (simulating 24h work-cycle with peak at midday)
        diurnal_factor = 0.5 + 0.3 * math.sin((elapsed_hours * 2 * math.pi) / 24.0)

        # Baseline utilization proportional to allocated resources
        cpu_alloc_ratio = self.allocated_cpu / max(self.total_cpu, 1)
        mem_alloc_ratio = self.allocated_mem_mb / max(self.total_mem_mb, 1)

        # Base utilization + noise
        base_cpu = 10.0 + (cpu_alloc_ratio * 65.0 * diurnal_factor) + random.uniform(-2.5, 3.5)
        base_mem = 15.0 + (mem_alloc_ratio * 70.0) + random.uniform(-1.0, 1.5)

        # 5% probability of a burst spike
        if random.random() < 0.05:
            base_cpu += random.uniform(15.0, 25.0)

        cpu_percent = round(max(2.0, min(99.5, base_cpu)), 2)
        mem_percent = round(max(5.0, min(98.0, base_mem)), 2)

        return {
            "hostname": self.hostname,
            "cpu_utilization_percent": cpu_percent,
            "memory_utilization_percent": mem_percent,
            "disk_read_bytes_sec": random.randint(1024, 524288),
            "disk_write_bytes_sec": random.randint(2048, 1048576),
            "network_rx_bytes_sec": random.randint(5000, 2097152),
            "network_tx_bytes_sec": random.randint(4000, 1572864),
            "active_workload_count": self.active_workloads,
            "is_simulated": True
        }
