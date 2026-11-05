# Non-Functional Requirements Document (NFR) — CloudScale

## 1. Performance & Latency SLAs
- **API Response Time**:
  - Read endpoints (`GET /api/v1/...`): 95th percentile latency < 100ms.
  - Write endpoints (`POST`, `PUT`, `DELETE`): 95th percentile latency < 250ms.
- **Scheduling Latency**: The scheduling engine must resolve a node placement decision within < 50ms for clusters of up to 100 nodes.
- **Telemetry Ingestion Throughput**: Capable of ingesting 1,000 metric samples/minute on single-node development hardware.
- **AI Inference Latency**:
  - Time-series model prediction: < 200ms per forecast batch.
  - Anomaly detection scoring: < 50ms per batch.
  - LLM tool-calling round-trip: < 3.0s total user latency.

## 2. Concurrency & Data Integrity
- **ACID Transaction Guarantees**: Resource allocations and reservations must be atomic.
- **Race Condition Prevention**: Concurrent scheduling requests competing for the same host capacity must never violate node resource caps (`allocated_cpu <= total_cpu` and `allocated_mem <= total_mem`).
- **Isolation Level**: Database transactions performing resource reservation execute with appropriate locking (`SELECT ... FOR UPDATE` with `READ_COMMITTED` or `SERIALIZABLE` isolation).

## 3. Security & Compliance
- **Authentication**: Stateless RS256 JWT with cryptographic verification.
- **Credential Storage**: BCrypt hashing with salt rounds set to 12.
- **Tenant Isolation**: Strict logical separation; queries must be bounded by `organisation_id`.
- **Secret Management**: Zero plain-text credentials or API keys in source code; configuration through environment variables only.
- **OWASP Compliance**: Robust mitigation against OWASP Top 10 vulnerabilities (SQLi via ORM parameter binding, XSS via Angular output sanitization, CSRF mitigation via Bearer token authorization, CORS strict origin binding).

## 4. Reliability & Fault Tolerance
- **Graceful Degradation**: If the Python AI service is unavailable, core scheduling and rule-based autoscaling must continue functioning without interruption.
- **Node Heartbeat Resilience**: Transient network hiccups (<90s) must not trigger premature node draining; explicit thresholds prevent flapping.
- **Self-Healing Simulation**: Node agent simulator must auto-recover and reconnect upon control plane restarts.

## 5. Observability & Maintainability
- **Structured Logging**: JSON-formatted logs with correlation IDs (`traceId`, `spanId`) across Java and Python services.
- **Metrics Exposure**: Standard Prometheus scrape targets providing JVM, HTTP, database connection pool, and custom scheduling metrics.
- **Code Maintainability**: Clean Architecture with modular boundaries, explicit DTOs, domain service isolation, and zero circular dependencies.

## 6. Accessibility & Frontend Standards
- **WCAG 2.1 AA Compliance**: High-contrast ratios, keyboard focus trapping, ARIA roles, and readable typography.
- **Modern Web Standards**: Native CSS layout, clean responsive breakpoints, container queries, and standard popover/dialog behavior per modern web guidelines.
