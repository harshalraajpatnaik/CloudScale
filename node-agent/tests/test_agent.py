import pytest
from httpx import AsyncClient, ASGITransport
from agent.main import app

@pytest.mark.asyncio
async def test_agent_health():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "UP"
        assert data["mode"] == "SIMULATED"

@pytest.mark.asyncio
async def test_agent_metric_sample():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get("/metrics/sample")
        assert response.status_code == 200
        data = response.json()
        assert "cpu_utilization_percent" in data
        assert "memory_utilization_percent" in data
        assert data["is_simulated"] is True

@pytest.mark.asyncio
async def test_push_telemetry_simulation():
    from agent.publisher import push_telemetry
    from unittest.mock import AsyncMock, patch

    mock_client = AsyncMock()
    mock_client.post.return_value.status_code = 201

    sample = {
        "cpu_utilization_percent": 45.5,
        "memory_utilization_percent": 60.2,
        "disk_read_bytes_sec": 1000,
        "disk_write_bytes_sec": 2000,
        "network_rx_bytes_sec": 3000,
        "network_tx_bytes_sec": 4000,
        "active_workload_count": 2
    }

    success = await push_telemetry(mock_client, "http://localhost:8080", "node-123", sample, "test-token")
    assert success is True
    mock_client.post.assert_called_once()
