# CloudScale Operations & Developer Runbook

This runbook provides step-by-step instructions for operating, developing, and verifying the **CloudScale** Intelligent Cloud Infrastructure Management Platform.

---

## 1. System Requirements & Prerequisites

| Requirement | Minimum Version | Verified Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Docker Engine** | 24.0+ | Docker Desktop 4.30+ | Local PostgreSQL & Redis persistence |
| **Java JDK** | 21 LTS | OpenJDK 25.0.5 / Temurin 21 | Spring Boot Control Plane |
| **Python** | 3.11+ | Python 3.13.1 | AI Microservice & Telemetry Agent |
| **Node.js** | 20.x+ | Node 20 / 22 LTS | Angular 19 Management Console |
| **Google Gemini Key** | API v1beta | `gemini-3.5-flash-lite` | AI-assisted infrastructure reasoning |

---

## 2. Infrastructure Services Startup (Docker Compose)

The persistence layer runs containerized PostgreSQL 16 and Redis 7.

```bash
# From repository root
cd infra
docker compose up -d

# Verify containers are healthy
docker compose ps
```

- **PostgreSQL**: Bound to `localhost:5438` (database: `cloudscale`, user: `cloudscale`, pass: `cloudscale_secret`).
- **Redis**: Bound to `localhost:6379`.

---

## 3. Starting the Java Spring Boot Control Plane

The control plane runs on port `8080` and automatically runs Flyway migrations on startup.

```bash
cd control-plane

# Compile and run
mvn spring-boot:run
```

- **API Base URL:** `http://localhost:8080/api/v1`
- **Swagger / OpenAPI:** `http://localhost:8080/swagger-ui.html`
- **Prometheus Metrics:** `http://localhost:8080/actuator/prometheus`
- **Health Check:** `http://localhost:8080/api/v1/health`

---

## 4. Starting the Python AI Service

The AI service operates on port `8000`.

```bash
cd ai-service

# Activate virtual environment
source .venv/bin/activate

# Ensure Gemini API key is configured in environment
export GEMINI_API_KEY="your-gemini-api-key"
export GEMINI_MODEL="models/gemini-3.5-flash-lite"

# Start FastAPI with Uvicorn
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

- **API Documentation:** `http://localhost:8000/docs`
- **Health Check:** `http://localhost:8000/health`

---

## 5. Starting the Telemetry Node Agent Simulator

The node agent pushes simulated host metrics and heartbeats every 30 seconds.

```bash
cd node-agent

# Activate virtual environment
source .venv/bin/activate

# Run agent with target control plane URL
python -m app.main
```

---

## 6. Starting the Angular 19 Management Console

The frontend runs on port `4200` with automated reverse-proxy to the control plane.

```bash
cd console

# Install dependencies (if not already installed)
npm install

# Start development server
npm start
```

- **Web Console:** `http://localhost:4200`
- **Default Routes:**
  - `/nodes`: Compute node registry and capacity gauges
  - `/workloads`: Workload management and lifecycle controls
  - `/metrics`: Live multi-point telemetry curves
  - `/autoscaling`: Scaling policies and trigger simulator
  - `/ai-assistant`: Gemini interactive chat, capacity forecasting, and recommendations
  - `/cost`: FinOps spend summary, node allocation, and waste detector
  - `/incidents`: SRE alerts and MTTR tracker
  - `/audit`: Immutable operational event ledger

---

## 7. Running the Automated Verification Suite

Run all checks end-to-end with a single script:

```bash
./scripts/run-checks.sh
```

This script executes:
1. Java Control Plane JUnit 5 / Spring Boot test suite (43 tests).
2. Python AI Service Pytest suite (6 tests).
3. Python Node Agent Pytest suite (3 tests).
4. Angular 19 Production Bundle build (`ng build --configuration production`).

---

## 8. Common Troubleshooting

### PostgreSQL Port Conflict (`Address already in use: 5438`)
Check for existing Docker containers:
```bash
docker ps | grep 5438
# If another process is using port 5438:
docker stop <container-id>
```

### Mockito Byte Buddy Dynamic Agent on JDK 25+
Ensure `control-plane/pom.xml` has `<argLine>-XX:+EnableDynamicAgentLoading</argLine>` in `maven-surefire-plugin` configuration and `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` is set to `mock-maker-subclass`.

### Gemini API Model Deprecation (HTTP 404)
Legacy models (`gemini-2.0-flash`, `gemini-2.5-flash-lite`) are deprecated for new projects. Always use `models/gemini-3.5-flash-lite` in `ai-service/app/core/config.py`.
