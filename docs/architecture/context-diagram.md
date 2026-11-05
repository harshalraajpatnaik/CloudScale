# C4 Level 1: System Context Diagram — CloudScale

```mermaid
C4Context
    title System Context diagram for CloudScale

    Person(operator, "Infrastructure Operator", "Monitors node capacity, approves scaling recommendations, manages workloads.")
    Person(admin, "Organisation Admin", "Manages organization users, quotas, policies, and billing.")
    Person(superAdmin, "Super Admin", "Oversees cross-tenant platform health, system configurations, and audits.")

    System(cloudscale, "CloudScale Platform", "Allows users to register nodes, schedule workloads, monitor telemetry, predict demand, and autoscale infrastructure.")

    System_Ext(gemini, "Google Gemini API", "Provides foundation models for the infrastructure intelligence assistant and natural language explanation generation.")
    System_Ext(prometheus, "Prometheus / Grafana", "External or internal metrics aggregator and enterprise dashboard visualizer.")

    Rel(operator, cloudscale, "Manages workloads, investigates incidents, approves scaling", "HTTPS / Angular")
    Rel(admin, cloudscale, "Manages users, organizations, and resource limits", "HTTPS / Angular")
    Rel(superAdmin, cloudscale, "Monitors platform health, system policies, and audits", "HTTPS / Angular")
    Rel(cloudscale, gemini, "Executes tool calling and natural language reasoning", "HTTPS / JSON")
    Rel(prometheus, cloudscale, "Scrapes JVM and business metrics", "HTTP / Actuator")
```
