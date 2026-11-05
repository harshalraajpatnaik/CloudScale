# Database ER Diagram & Relational Schema — CloudScale

## Entity Relationship Diagram

```mermaid
erDiagram
    ORGANISATION {
        uuid id PK
        varchar(100) name
        varchar(100) slug UK
        varchar(50) plan_tier
        int max_cpu_cores_quota
        bigint max_memory_mb_quota
        timestamp created_at
        timestamp updated_at
    }

    USER {
        uuid id PK
        uuid organisation_id FK
        varchar(255) email UK
        varchar(255) password_hash
        varchar(100) full_name
        varchar(50) role
        boolean is_active
        int failed_login_attempts
        timestamp locked_until
        timestamp last_login_at
        timestamp created_at
        timestamp updated_at
    }

    REFRESH_TOKEN {
        uuid id PK
        uuid user_id FK
        varchar(255) token_hash UK
        timestamp expires_at
        boolean revoked
        varchar(45) client_ip
        varchar(255) user_agent
        timestamp created_at
    }

    NODE {
        uuid id PK
        uuid organisation_id FK
        varchar(100) hostname UK
        varchar(45) ip_address
        int total_cpu_cores
        bigint total_memory_mb
        int allocated_cpu_cores
        bigint allocated_memory_mb
        varchar(50) status
        timestamp last_heartbeat
        timestamp created_at
        timestamp updated_at
    }

    WORKLOAD {
        uuid id PK
        uuid organisation_id FK
        uuid assigned_node_id FK "nullable"
        varchar(100) name
        varchar(50) workload_type
        varchar(20) priority
        int cpu_cores_required
        bigint memory_mb_required
        varchar(50) desired_state
        varchar(50) actual_state
        text failure_reason
        timestamp scheduled_at
        timestamp created_at
        timestamp updated_at
    }

    SCALING_POLICY {
        uuid id PK
        uuid organisation_id FK
        uuid workload_id FK UK
        decimal cpu_scale_up_threshold
        decimal cpu_scale_down_threshold
        decimal memory_scale_up_threshold
        decimal memory_scale_down_threshold
        int min_replicas
        int max_replicas
        int cooldown_seconds
        int evaluation_window_seconds
        boolean is_enabled
        timestamp last_scaled_at
        timestamp created_at
    }

    SCALING_EVENT {
        uuid id PK
        uuid scaling_policy_id FK
        uuid workload_id FK
        varchar(20) action_type
        int previous_replicas
        int new_replicas
        varchar(50) trigger_metric
        decimal trigger_value
        text reasoning
        timestamp executed_at
    }

    NODE_METRIC {
        uuid id PK
        uuid node_id FK
        decimal cpu_utilization_percent
        decimal memory_utilization_percent
        bigint disk_read_bytes_sec
        bigint disk_write_bytes_sec
        bigint network_rx_bytes_sec
        bigint network_tx_bytes_sec
        int active_workload_count
        timestamp sampled_at
    }

    ALERT {
        uuid id PK
        uuid organisation_id FK
        uuid node_id FK "nullable"
        uuid workload_id FK "nullable"
        varchar(50) severity
        varchar(100) alert_type
        text message
        varchar(50) status
        timestamp triggered_at
        timestamp resolved_at
    }

    AI_RECOMMENDATION {
        uuid id PK
        uuid organisation_id FK
        varchar(50) category
        text explanation
        jsonb supporting_metrics
        decimal confidence_score
        varchar(50) expected_impact
        varchar(50) suggested_action
        jsonb action_payload
        varchar(30) approval_status
        uuid approved_by_user_id FK "nullable"
        timestamp evaluated_at
        timestamp created_at
    }

    AUDIT_LOG {
        uuid id PK
        uuid organisation_id FK
        uuid actor_user_id FK "nullable"
        varchar(100) action
        varchar(100) resource_type
        varchar(100) resource_id
        jsonb previous_state
        jsonb new_state
        varchar(45) client_ip
        timestamp recorded_at
    }

    ORGANISATION ||--o{ USER : "employs"
    ORGANISATION ||--o{ NODE : "provisions"
    ORGANISATION ||--o{ WORKLOAD : "executes"
    ORGANISATION ||--o{ SCALING_POLICY : "manages"
    ORGANISATION ||--o{ ALERT : "receives"
    ORGANISATION ||--o{ AI_RECOMMENDATION : "receives"
    ORGANISATION ||--o{ AUDIT_LOG : "audits"

    USER ||--o{ REFRESH_TOKEN : "owns"
    USER ||--o{ AUDIT_LOG : "initiates"

    NODE ||--o{ WORKLOAD : "hosts"
    NODE ||--o{ NODE_METRIC : "records"
    NODE ||--o{ ALERT : "triggers"

    WORKLOAD ||--o{ SCALING_POLICY : "governed_by"
    WORKLOAD ||--o{ SCALING_EVENT : "scales"
    WORKLOAD ||--o{ ALERT : "triggers"

    SCALING_POLICY ||--o{ SCALING_EVENT : "triggers"
```
