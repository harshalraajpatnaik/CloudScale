# Deployment Diagram — CloudScale Local Docker Environment

```mermaid
deploymentNode(dev_machine, "Developer Workstation", "macOS / Linux / Windows") {
    deploymentNode(docker_engine, "Docker Engine 24+", "Container Runtime") {
        deploymentNode(compose_net, "cloudscale-net", "Bridge Network") {
            container(c_postgres, "postgres", "postgres:16-alpine", "PostgreSQL primary database with persistent volume")
            container(c_redis, "redis", "redis:7-alpine", "Redis in-memory cache and revocation store")
            container(c_control_plane, "control-plane", "eclipse-temurin:25-jdk-alpine", "Spring Boot Control Plane (Port 8080)")
            container(c_ai_service, "ai-service", "python:3.13-slim", "FastAPI AI/ML service (Port 8000)")
            container(c_node_agent, "node-agent", "python:3.13-slim", "Simulated node telemetry agent (Port 8090)")
            container(c_prometheus, "prometheus", "prom/prometheus:v3.0.0", "Prometheus scraper (Port 9090)")
            container(c_grafana, "grafana", "grafana/grafana:11.2.0", "Grafana dashboards (Port 3000)")
        }
        
        volume(vol_pg, "pgdata", "Named Volume for PostgreSQL persistence")
        volume(vol_redis, "redisdata", "Named Volume for Redis persistence")
        volume(vol_prom, "prometheus_data", "Named Volume for Prometheus TSDB")
        volume(vol_grafana, "grafana_data", "Named Volume for Grafana state")
    }

    deploymentNode(host_browser, "Host Web Browser", "Chrome / Edge / Firefox") {
        container(c_console, "Angular Console", "node:26 / Dev Server", "Runs locally on host port 4200 (or containerized in prod)")
    }
}

Rel(c_console, c_control_plane, "HTTPS / REST", "localhost:8080")
Rel(c_control_plane, c_postgres, "JDBC", "postgres:5432")
Rel(c_control_plane, c_redis, "TCP", "redis:6379")
Rel(c_control_plane, c_ai_service, "HTTP", "ai-service:8000")
Rel(c_node_agent, c_control_plane, "HTTP", "control-plane:8080")
Rel(c_prometheus, c_control_plane, "HTTP", "control-plane:8080/actuator/prometheus")
Rel(c_grafana, c_prometheus, "HTTP", "prometheus:9090")
```
