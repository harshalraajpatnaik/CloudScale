# CloudScale Documentation Index

Welcome to the technical documentation for **CloudScale** — an enterprise-grade cloud infrastructure management and predictive resource orchestration platform.

## Documentation Structure

```
docs/
├── README.md                                  # Documentation Index (this file)
├── product-requirements.md                    # Core vision, personas, and success metrics
├── functional-requirements.md                 # Detailed module-by-module feature specifications
├── non-functional-requirements.md             # Performance, security, scalability, and reliability SLAs
├── system-overview.md                         # High-level architecture and platform topology
│
├── architecture/                              # Deep technical architecture specifications
│   ├── architecture-overview.md               # End-to-end design & design principles
│   ├── context-diagram.md                     # C4 Level 1: System Context Diagram
│   ├── container-diagram.md                   # C4 Level 2: Container Architecture
│   ├── component-diagram.md                   # C4 Level 3: Component Interactions
│   ├── deployment-diagram.md                  # Infrastructure & container topology
│   ├── data-flow-diagrams.md                  # Ingestion, scheduling, and scaling flows
│   ├── sequence-diagrams.md                   # Critical path sequence diagrams
│   ├── database-er-diagram.md                 # Entity relationship specifications
│   ├── security-architecture.md               # Zero-trust, RBAC, and crypto specifications
│   ├── ai-architecture.md                     # AI/ML pipelines, tool calling & evaluation
│   └── infrastructure-architecture.md         # Simulated agent & future K8s/KVM roadmap
│
├── adr/                                       # Architecture Decision Records
│   ├── ADR-001-architecture-style.md          # Modular monolith vs microservices
│   ├── ADR-002-database-selection.md          # PostgreSQL as primary datastore
│   ├── ADR-003-scheduling-strategy.md         # Resource-Aware algorithm selection
│   ├── ADR-004-ai-service-boundary.md         # Python AI service separation
│   ├── ADR-005-authentication-strategy.md     # RS256 JWT with Redis revocation
│   └── ADR-006-predictive-autoscaling.md      # Rule-based foundation with safety guardrails
│
├── api/                                       # REST API contracts
│   ├── api-overview.md                        # Envelope format, status codes, pagination
│   ├── authentication.md                      # Auth & user management endpoints
│   ├── infrastructure-apis.md                 # Nodes & resource management
│   ├── workload-apis.md                       # Workload lifecycle & scheduling
│   └── forecasting-apis.md                    # AI predictions & recommendations
│
├── database/                                  # Database architecture
│   ├── schema.md                              # Tables, constraints, and data dictionary
│   ├── indexing-strategy.md                   # B-Tree, composite, and time-series indexing
│   └── transaction-design.md                  # Concurrency, isolation levels, and locking
│
├── ai/                                        # Machine Learning & AI
│   ├── model-selection.md                     # Model taxonomy & criteria
│   ├── forecasting-methodology.md             # Time-series training and validation
│   ├── anomaly-detection.md                   # Statistical and ML anomaly detection
│   ├── agent-tools.md                         # Scoped function calling specifications
│   ├── evaluation-methodology.md              # Benchmarking metrics (MAE, RMSE, MAPE)
│   └── model-limitations.md                   # Known boundaries and fallback policies
│
├── security/                                  # Security & Threat Modeling
│   ├── threat-model.md                        # STRIDE threat model & mitigations
│   ├── authentication.md                      # Token handling & rotation specs
│   ├── authorisation.md                       # Role hierarchies and tenant isolation
│   └── security-testing.md                    # Penetration testing & fuzzing procedures
│
├── testing/                                   # QA & Quality Engineering
│   ├── testing-strategy.md                    # Test pyramid & coverage targets
│   ├── test-cases.md                          # Comprehensive test suite specifications
│   ├── failure-scenarios.md                   # Chaos engineering & fault injection
│   └── performance-testing.md                 # k6 load testing & latency SLAs
│
├── benchmarks/                                # Verified Benchmarks
│   ├── benchmark-methodology.md               # Hardware, workload, and profiling setup
│   └── benchmark-results.md                   # Throughput & latency measurements
│
├── deployment/                                # DevOps & Operations
│   ├── local-setup.md                         # Local environment bootstrapping
│   ├── docker-setup.md                        # Docker Compose configuration
│   ├── production-deployment.md               # Production architecture & hardening
│   └── environment-variables.md               # Exhaustive environment variable reference
│
├── operations/                                # Runbooks & SRE
│   ├── monitoring.md                          # Metrics, dashboards, and alerting rules
│   ├── troubleshooting.md                     # Incident resolution runbooks
│   ├── disaster-recovery.md                   # Backup and recovery strategies
│   └── known-limitations.md                   # Documented design boundaries
│
├── user-guide/                                # Persona-specific manuals
│   ├── getting-started.md                     # Quickstart tutorial
│   ├── administrator-guide.md                 # Organisation & policy management
│   ├── operator-guide.md                      # Daily operations & incident response
│   └── viewer-guide.md                        # Metrics inspection & auditing
│
├── glossary.md                                # Domain terms and definitions
└── learning-guide-for-beginners.md            # Beginner-friendly concepts walkthrough
```
