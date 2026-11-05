# Architecture Overview — CloudScale

## 1. Architectural Philosophy
CloudScale is architected according to the following foundational principles:

1. **Separation of Control & Execution**: The control plane (Java) is the single source of truth for desired and actual state. The intelligence tier (Python) offers predictions and recommendations, but possesses no direct authority to alter infrastructure state without control-plane validation.
2. **Deterministic Foundations Before AI Enhancements**: Core scheduling, rule-based autoscaling, and state transitions must be mathematically verifiable and deterministic before applying probabilistic machine learning layers.
3. **Defense-in-Depth Multi-Tenancy**: Organization boundaries are enforced at the API route, service layer, database query, and entity validation levels.
4. **Resilience to Component Outages**: The loss or slow response of the Python AI service or Prometheus never impairs the ability of the control plane to manage running workloads or execute essential rule-based scheduling.
5. **Traceable & Auditable**: Every operational change is recorded in an immutable audit ledger with before/after state diffs.

## 2. Layered Architecture

```
+-------------------------------------------------------------------------+
|                         Presentation Layer                              |
|   Angular 19 SPA | Material 3 Components | ECharts | Reactive Forms     |
+-------------------------------------------------------------------------+
                                    | REST (JSON / Bearer JWT)
+-------------------------------------------------------------------------+
|                         API Gateway & Security Layer                     |
|   Spring Security Filter Chain | JWT RS256 Auth | Rate Limiter | CORS   |
+-------------------------------------------------------------------------+
                                    |
+-------------------------------------------------------------------------+
|                         Domain Application Services                     |
|   Node Service | Workload Service | Scheduler Service | Autoscaler      |
|   Cost Service | Tenancy Service  | Audit Service     | Telemetry Svc   |
+-------------------------------------------------------------------------+
                    |                                 |
+-------------------+----------------+   +------------+------------+
|        Data Access Layer           |   |   Integration Clients    |
|   Spring Data JPA | Hibernate 6.x  |   |   FastAPI REST Client    |
|   Flyway Database Migrations       |   |   Gemini AI Proxy        |
+------------------------------------+   +-------------------------+
                    |                                 |
+-------------------+----------------+   +------------+------------+
|            Persistence             |   |    Intelligence Tier    |
|   PostgreSQL 16 Relational Engine  |   |   FastAPI | Scikit-learn|
|   Redis 7 Token Revocation Store   |   |   LightGBM | Pandas     |
+------------------------------------+   +-------------------------+
```

## 3. Communication Protocols
- **Client to Control Plane**: HTTPS REST with JSON payloads. Authenticated using Bearer JWT.
- **Node Agent to Control Plane**: Internal REST with shared HMAC authentication token or API key.
- **Control Plane to AI Service**: Internal REST over Docker bridge network with mTLS/pre-shared service credentials.
- **AI Service to Gemini**: Google GenAI HTTPS/gRPC with API key authentication.
- **Prometheus to Control Plane**: HTTP pull over `/actuator/prometheus`.
