import httpx
from typing import Dict, Any, Optional

async def push_telemetry(
    client: httpx.AsyncClient,
    control_plane_url: str,
    node_id: str,
    metrics: Dict[str, Any],
    auth_token: Optional[str] = None
) -> bool:
    """
    Pushes simulated telemetry samples to the control plane.
    """
    url = f"{control_plane_url.rstrip('/')}/api/v1/metrics/nodes/{node_id}"
    headers = {"Content-Type": "application/json"}
    if auth_token:
        headers["Authorization"] = f"Bearer {auth_token}"

    payload = {
        "cpuUtilizationPercent": metrics["cpu_utilization_percent"],
        "memoryUtilizationPercent": metrics["memory_utilization_percent"],
        "diskReadBytesSec": metrics.get("disk_read_bytes_sec", 0),
        "diskWriteBytesSec": metrics.get("disk_write_bytes_sec", 0),
        "networkRxBytesSec": metrics.get("network_rx_bytes_sec", 0),
        "networkTxBytesSec": metrics.get("network_tx_bytes_sec", 0),
        "activeWorkloadCount": metrics.get("active_workload_count", 0)
    }

    try:
        response = await client.post(url, json=payload, headers=headers, timeout=5.0)
        return response.status_code in (200, 201)
    except Exception:
        return False
