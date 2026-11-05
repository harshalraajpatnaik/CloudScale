# C4 Level 3: Component Diagram — Control Plane

```mermaid
C4Component
    title Component Diagram for Spring Boot Control Plane

    Container_Boundary(cp, "Spring Boot Control Plane")
        Component(securityFilter, "Security Filter Chain", "Spring Security", "Authenticates RS256 JWT, enforces tenant context, checks role permissions.")
        Component(authCtrl, "Auth Controller & Service", "Java / Spring Web", "Handles registration, login, token refresh, and logout.")
        Component(orgService, "Tenancy Service", "Spring Service", "Enforces organization boundaries and resource quotas.")
        Component(nodeRegistry, "Node Registry Service", "Spring Service", "Tracks host capacity, heartbeats, and node state transitions.")
        Component(workloadService, "Workload Service", "Spring Service", "Enforces workload state machine and lifecycle operations.")
        Component(schedulerEngine, "Scheduling Engine", "Domain Component", "Executes Resource-Aware, Best-Fit, First-Fit, and Priority algorithms with pessimistic locks.")
        Component(autoscaler, "Autoscaling Controller", "Spring Scheduled Task", "Evaluates scaling rules against metric windows and queues replica changes.")
        Component(metricsIngest, "Metrics Ingestion Service", "Spring Service", "Ingests node agent telemetry and writes to time-series storage.")
        Component(costEngine, "Cost Estimation Engine", "Domain Component", "Calculates hourly/monthly burn rate and underutilized node savings.")
        Component(aiClient, "AI Service Client", "WebClient / RestTemplate", "Communicates with Python FastAPI service for forecasts and advice.")
        Component(auditService, "Audit Logging Service", "Spring Service / AOP", "Records immutable change events.")
    end

    ContainerDb(postgres, "PostgreSQL 16", "Database", "Stores entities and state.")
    ContainerDb(redis, "Redis 7", "Cache", "Stores token revocation list.")
    Container(aiService, "Python AI Service", "FastAPI", "Generates forecasts.")

    Rel(securityFilter, authCtrl, "Routes auth endpoints")
    Rel(securityFilter, redis, "Verifies token not revoked")
    Rel(nodeRegistry, postgres, "Queries and updates node state")
    Rel(workloadService, schedulerEngine, "Requests placement for pending workloads")
    Rel(schedulerEngine, nodeRegistry, "Reserves host capacity with SELECT FOR UPDATE")
    Rel(autoscaler, metricsIngest, "Reads rolling metrics")
    Rel(autoscaler, workloadService, "Requests scale-up / scale-down")
    Rel(autoscaler, aiClient, "Requests forecast signals")
    Rel(costEngine, nodeRegistry, "Aggregates resource allocations")
    Rel(aiClient, aiService, "HTTP POST /forecast")
    Rel(workloadService, auditService, "Publishes state change events")
```
