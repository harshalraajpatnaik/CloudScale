import pytest
from httpx import AsyncClient, ASGITransport
from app.main import app

@pytest.mark.asyncio
async def test_forecast_capacity():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        # Send 15 samples
        history = [
            {"timestamp": f"2026-11-05T10:{i:02d}:00Z", "cpuUtilizationPercent": 40.0 + (i * 1.5)}
            for i in range(15)
        ]
        response = await client.post("/api/v1/forecast/capacity", json={
            "history": history,
            "metricKey": "cpuUtilizationPercent",
            "horizonSteps": 6
        })
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert "selectedModel" in data["data"]
        assert len(data["data"]["forecast"]) == 6
        assert len(data["data"]["evaluatedModels"]) >= 2

@pytest.mark.asyncio
async def test_detect_anomalies():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        # 12 normal samples around 40% CPU, then an extreme spike at 99.5%
        telemetry = [
            {"timestamp": f"2026-11-05T12:{i:02d}:00Z", "cpuUtilizationPercent": 40.0 + (i % 3)}
            for i in range(12)
        ]
        telemetry.append({"timestamp": "2026-11-05T12:15:00Z", "cpuUtilizationPercent": 99.5})

        response = await client.post("/api/v1/anomalies/detect", json={"telemetry": telemetry})
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert data["count"] >= 1
        assert any(a["severity"] in ("CRITICAL", "WARNING") for a in data["data"])

@pytest.mark.asyncio
async def test_ai_diagnose():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post("/api/v1/ai/diagnose", json={
            "incidentData": {
                "hostname": "worker-sim-01",
                "cpu_utilization_percent": 96.5,
                "memory_utilization_percent": 88.0,
                "active_workload_count": 8
            }
        })
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert "root_cause" in data["data"]
        assert "remediation_steps" in data["data"]

@pytest.mark.asyncio
async def test_ai_recommend():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post("/api/v1/ai/recommend", json={
            "clusterContext": {
                "nodes": [
                    {"id": "node-1", "hostname": "worker-01", "cpuUtilizationPercent": 88.0, "status": "ACTIVE"}
                ],
                "workloads": []
            }
        })
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert data["count"] >= 1

@pytest.mark.asyncio
async def test_ai_chat():
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.post("/api/v1/ai/chat", json={
            "message": "Which node has the highest CPU?",
            "clusterContext": {
                "nodes": [
                    {"hostname": "worker-01", "cpuUtilizationPercent": 91.5, "allocatedCpuCores": 14, "totalCpuCores": 16}
                ]
            }
        })
        assert response.status_code == 200
        data = response.json()
        assert data["success"] is True
        assert "reply" in data
        assert len(data["reply"]) > 10
