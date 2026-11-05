# CloudScale

> **Intelligent Cloud Infrastructure Management & Predictive Resource Orchestration Platform**

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 21+](https://img.shields.io/badge/Java-21%2B%20%2F%2025-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-19%20%2F%20Material-red.svg)](https://angular.dev/)
[![Python](https://img.shields.io/badge/Python-3.13%20%2F%20FastAPI-blue.svg)](https://fastapi.tiangolo.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red.svg)](https://redis.io/)

CloudScale is a working miniature cloud control plane with actual workload scheduling, resource allocation, infrastructure monitoring, predictive autoscaling, and AI-assisted infrastructure intelligence. Designed as an enterprise-grade portfolio platform demonstrating distributed systems, resilient scheduling algorithms, multi-tenant security, and machine learning operations.

---

## Architecture at a Glance

```mermaid
flowchart TD
    UA["👤 User / Operator"]
    AC["🌐 Angular Console\n(Port 4200)"]
    CP["☕ Spring Boot Control Plane\n(Port 8080)"]
    AI["🐍 FastAPI AI Service\n(Port 8000)"]
    NA["🤖 Simulated Node Agent\n(Port 8090)"]
    PG[("🐘 PostgreSQL 16\nPrimary Storage")]
    RD[("⚡ Redis 7\nRevocation & Rate Limiting")]
    PR["📊 Prometheus\nMetrics"]
    GR["📈 Grafana\nDashboards"]

    UA --> AC
    AC -->|"REST + JWT"| CP
    CP -->|"Internal REST"| AI
    CP -->|"Metrics / Heartbeat"| NA
    CP --> PG
    CP --> RD
    PR -->|"Scrape"| CP
    GR -->|"Visualize"| PR
```

---

## Core Capabilities

1. **Multi-Tenant Control Plane**: Complete organization-level isolation with hierarchical RBAC (`SUPER_ADMIN`, `ORGANISATION_ADMIN`, `INFRASTRUCTURE_OPERATOR`, `VIEWER`).
2. **Infrastructure Node Registry**: Lifecycle state transitions (`PENDING`, `ACTIVE`, `DRAINING`, `INACTIVE`), heartbeat detection, and dynamic capacity tracking.
3. **Deterministic Workload Scheduling**: Pluggable scheduling engine with **Resource-Aware** (default), **Best-Fit**, **First-Fit**, and **Priority-Based** algorithms, protected against race conditions via transactional resource reservations.
4. **Autonomous Autoscaling Engine**: Rule-based threshold evaluation with cooldown windows, scale-up/scale-down bounds, and predictive foresight safety checks.
5. **Predictive Capacity Forecasting**: Time-series pipeline evaluating naive, rolling-average, SARIMA, and LightGBM models against held-out validation sets.
6. **Infrastructure Intelligence Assistant**: Scoped, tool-calling AI assistant (Gemini API) querying live metrics, workload states, alerts, and cost optimizations through authorized APIs.
7. **Comprehensive Observability**: OpenTelemetry instrumentation, Prometheus metrics export, and preconfigured Grafana enterprise dashboards.

---

## Quick Start (Development)

### Prerequisites
- **Java**: OpenJDK 21 or 25
- **Node.js**: 20+ (Node 26 LTS tested) & npm
- **Python**: 3.12+ (Python 3.13 tested)
- **Docker & Docker Compose**: 24+

### Setup
```bash
# 1. Clone repository
git clone https://github.com/harshalraajpatnaik/CloudScale.git
cd CloudScale

# 2. Configure environment
cp .env.example .env
# Edit .env with your preferred passwords and credentials

# 3. Start backing services (PostgreSQL, Redis, Prometheus, Grafana)
cd infra && docker compose up -d

# 4. Run Spring Boot Control Plane
cd ../control-plane && ./mvnw spring-boot:run

# 5. Run Python AI Service
cd ../ai-service && pip install -r requirements.txt && uvicorn app.main:app --reload --port 8000

# 6. Run Angular Console
cd ../console && npm install && npm start
```

---

## Documentation

Comprehensive architecture, API contracts, threat models, and operational runbooks are located in the [`docs/`](./docs) directory:

- [System Overview](./docs/system-overview.md)
- [Product Requirements](./docs/product-requirements.md)
- [Architecture Specifications](./docs/architecture/)
- [Architecture Decision Records (ADRs)](./docs/adr/)
- [API Documentation](./docs/api/)
- [Database Schema & ERDs](./docs/database/)
- [AI/ML Methodology](./docs/ai/)
- [Security Architecture](./docs/security/)
- [Testing & Quality Assurance](./docs/testing/)
- [Beginner's Learning Guide](./docs/learning-guide-for-beginners.md)

---

## License

This project is licensed under the [MIT License](./LICENSE).
