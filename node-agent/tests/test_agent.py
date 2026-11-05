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
