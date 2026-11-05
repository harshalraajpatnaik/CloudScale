# ADR-004: AI Service Boundary & Authority Model

## Status
Accepted

## Context
Integrating artificial intelligence into infrastructure orchestration introduces reliability and security risks if LLM or ML models are permitted direct write access to cluster state or database records.

## Decision
We enforce a strict boundary:
- The **Java Spring Boot Control Plane** remains the exclusive authority for infrastructure state changes, policy enforcement, and authentication.
- The **Python AI Service** functions strictly as an advisory, prediction, and diagnostic service.
- The AI Service has read-only access to historical metrics for model training.
- In tool-calling workflows, the AI Assistant interacts with the control plane through authenticated internal REST APIs carrying the user's JWT. The AI service possesses no database credentials and no shell execution privileges.
- All AI recommendations that mutate infrastructure require human operator approval.

## Consequences
- **Positive**: Eliminates hallucination-driven infrastructure destruction; conforms to enterprise security compliance; enables graceful degradation if the AI service experiences downtime.
- **Negative**: Adds an HTTP hop between Python and Java when fulfilling assistant tool calls.
