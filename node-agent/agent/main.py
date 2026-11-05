import os
from fastapi import FastAPI
from agent.simulator import NodeTelemetrySimulator

app = FastAPI(
    title="CloudScale Simulated Node Agent",
    description="Reports simulated telemetry and heartbeats to the control plane"
)

HOSTNAME = os.getenv("NODE_HOSTNAME", "worker-sim-01")
simulator = NodeTelemetrySimulator(hostname=HOSTNAME)

@app.get("/health", tags=["Health"])
async def health():
    return {
        "status": "UP",
        "service": "cloudscale-node-agent",
        "hostname": HOSTNAME,
        "mode": "SIMULATED"
    }

@app.get("/metrics/sample", tags=["Telemetry"])
async def get_metric_sample():
    return simulator.sample_metrics()

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("agent.main:app", host="0.0.0.0", port=int(os.getenv("NODE_AGENT_PORT", "8090")), reload=True)
