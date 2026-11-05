# ADR-001: Architecture Style — Modular Monolith with Distinct Intelligence Service

## Status
Accepted

## Context
We need to design a robust cloud control plane that balances maintainability, portfolio clarity, deployment simplicity on developer hardware, and scalability. Choosing an overly fragmented microservices architecture prematurely introduces distributed transaction complexity, network latency, and operational fragility. Conversely, a monolithic design bundling Python ML libraries inside a JVM process is inefficient and non-idiomatic.

## Decision
We adopt a **modular monolith** for the core control plane implemented in Java / Spring Boot, complemented by a dedicated, loosely coupled **Python AI service** for machine learning and LLM interactions, and an independent **Angular SPA** frontend.
- Logical modules within Spring Boot maintain explicit package boundaries (e.g., `com.cloudscale.auth`, `com.cloudscale.workload`, `com.cloudscale.scheduler`).
- Cross-module interactions occur via strongly typed Java interfaces and domain events rather than RPCs or message queues initially.
- The Python AI service operates as a separate process communicating via versioned REST APIs.

## Consequences
- **Positive**: Extremely fast compile-test-debug loops; zero distributed transaction nightmares; straightforward local Docker deployment; clear module boundaries permit future microservice extraction if required.
- **Negative**: The control plane components scale vertically together in the initial version.
