# Functional Requirements Document — CloudScale

## Module 1: Authentication & Authorization (M1)
- **User Registration**: Sign up with email, password (minimum 8 characters, mixed case, numbers, symbols), and full name.
- **Organization Association**: First user of an organization assumes `ORGANISATION_ADMIN`; subsequent users are invited or assigned roles by admins.
- **Login & Token Generation**: Authenticate credentials; return RS256-signed JWT Access Token (15-min TTL) and cryptographically random Refresh Token (30-day TTL).
- **Token Rotation & Revocation**: Refreshing an access token invalidates the used refresh token and issues a new pair. Immediate revocation supported via Redis denylist.
- **Role-Based Access Control (RBAC)**: Support roles:
  - `SUPER_ADMIN`: System-wide administration, tenant management.
  - `ORGANISATION_ADMIN`: Organization resources, workloads, nodes, user invites.
  - `INFRASTRUCTURE_OPERATOR`: Workload lifecycle, alert triage, scaling approvals.
  - `VIEWER`: Read-only analytics and audit logs.
- **Account Protection**: Progressive lockout after 5 consecutive failed attempts (15-minute lock duration).
- **Audit Trails**: Capture every login, logout, failed attempt, and privilege change.

## Module 2: Organization & Tenancy Management (M2)
- **Tenant Isolation**: Every database interaction with tenant data must filter by `organisation_id`.
- **Organization Hierarchy**: Manage team members, allocate organizational resource quotas, and configure organization-wide defaults.
- **Resource Limits**: Define maximum CPU and memory caps per organization.

## Module 3: Infrastructure Node Registry (M3)
- **Node Registration**: Register physical/simulated nodes with hostname, IP, total CPU cores, and total memory (MB).
- **Node State Machine**: Enforce transitions between `PENDING`, `ACTIVE`, `DRAINING`, `MAINTENANCE`, `INACTIVE`.
- **Heartbeat & Liveness**: Process heartbeat signals from node agents (30s interval). Mark nodes as `UNREACHABLE` if no heartbeat received for > 90s.
- **Capacity Accounting**: Dynamically track `allocated_cpu_cores` and `allocated_memory_mb` based on active workloads.

## Module 4: Workload Management (M4)
- **Workload Lifecycle**: Support states: `PENDING`, `SCHEDULING`, `RUNNING`, `STOPPING`, `STOPPED`, `SUSPENDED`, `FAILED`, `COMPLETED`.
- **Resource Specifications**: Require explicit CPU core and memory MB requests.
- **Workload Priorities**: Support `CRITICAL`, `HIGH`, `NORMAL`, `LOW`, `BATCH`.
- **State Validation**: Block illegal state jumps (e.g., cannot transition directly from `STOPPED` to `RUNNING` without scheduling).

## Module 5: Scheduling Engine (M5)
- **Pluggable Algorithms**:
  - `ResourceAwareScheduler` (Default): Evaluates balanced CPU/memory load vector to minimize fragmentation.
  - `BestFitScheduler`: Minimizes leftover capacity on host to pack tightly.
  - `FirstFitScheduler`: Fast first available node meeting constraints.
  - `PriorityScheduler`: Preempts lower-priority workloads when high-priority demands capacity.
- **Concurrency Guarantees**: Pessimistic database locking (`SELECT ... FOR UPDATE`) prevents concurrent overscheduling.

## Module 6: Autoscaling Controller (M6)
- **Scaling Policies**: Bind policies to workloads with metric type (CPU%, Memory%), scale-up threshold (e.g., >80%), scale-down threshold (e.g., <30%), min/max replica limits, and cooldown timers (default 300s).
- **Rule-Based Evaluation**: Periodic evaluation loop (60s) compares sliding window metrics against thresholds and triggers scaling actions.
- **Predictive Foresight Integration**: Incorporate AI service capacity forecasts to proactively queue scaling actions before traffic peaks occur.

## Module 7: Monitoring & Metrics Ingestion (M7)
- **Telemetry Ingestion**: Ingest CPU utilization, memory consumption, disk I/O, network I/O, and workload counts from node agents every 30s.
- **Prometheus Export**: Expose Micrometer metrics at `/actuator/prometheus`.
- **Alert Generation**: Generate alerts for high CPU (>90% for 3 mins), high memory (>95%), and unreachable nodes.

## Module 8: Cost Analytics (M8)
- **Cost Estimation**: Compute hourly, daily, and monthly projected spend based on allocated resources and sample pricing parameters.
- **Underutilization Detection**: Flag nodes with average utilization <20% as optimization candidates.
- **Allocation Simulation**: Model cost impacts of resizing workloads or consolidating nodes.

## Module 9: AI/ML Forecasting & Diagnostics (M9)
- **Time-Series Forecasting**: Generate multi-step capacity forecasts (5m, 15m, 30m) using statistical (Moving Average, SARIMA) and ML (LightGBM) models.
- **Anomaly Detection**: Flag unusual resource consumption using rolling Z-Score, IQR, and Isolation Forests.
- **Infrastructure Intelligence Assistant**: Scoped LLM agent executing structured tools (`getNodeMetrics`, `getWorkloadDetails`, `getScalingHistory`, `getCapacityForecast`, `getCostAnalysis`) over authenticated internal REST APIs.

## Module 10: Audit Logging (M10)
- Immutable audit ledger recording actor, timestamp, action, resource URI, previous state, new state, and client IP address.

## Module 11: Notification Management (M11)
- Dispatch in-app notifications and event streams for alerts, autoscaling executions, and system state changes.

## Module 12: Angular Management Console (M12)
- Modern, responsive SPA with 20 enterprise pages, dark/light themes, reactive forms, real-time ECharts data visualizations, and accessible navigation.
