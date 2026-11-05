-- =============================================================================
-- CloudScale Database Migration V1: Initial Schema
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Organisations (Tenant Root)
CREATE TABLE organisations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    plan_tier VARCHAR(50) NOT NULL DEFAULT 'FREE',
    max_cpu_quota INT NOT NULL DEFAULT 64,
    max_memory_mb_quota BIGINT NOT NULL DEFAULT 131072,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_organisations_slug ON organisations(slug);

-- 2. Users & RBAC
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_org_id ON users(organisation_id);

-- 3. Refresh Tokens (Single-Use Token Rotation)
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    client_ip VARCHAR(45),
    user_agent VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- 4. Infrastructure Nodes
CREATE TABLE nodes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    hostname VARCHAR(100) NOT NULL UNIQUE,
    ip_address VARCHAR(45) NOT NULL,
    total_cpu_cores INT NOT NULL,
    total_memory_mb BIGINT NOT NULL,
    allocated_cpu_cores INT NOT NULL DEFAULT 0,
    allocated_memory_mb BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    is_simulated BOOLEAN NOT NULL DEFAULT TRUE,
    last_heartbeat TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_node_cpu_allocation CHECK (allocated_cpu_cores <= total_cpu_cores),
    CONSTRAINT chk_node_mem_allocation CHECK (allocated_memory_mb <= total_memory_mb)
);

CREATE INDEX idx_nodes_org_status ON nodes(organisation_id, status);
CREATE INDEX idx_nodes_heartbeat ON nodes(last_heartbeat);

-- 5. Workloads
CREATE TABLE workloads (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    assigned_node_id UUID REFERENCES nodes(id) ON DELETE SET NULL,
    name VARCHAR(100) NOT NULL,
    workload_type VARCHAR(50) NOT NULL DEFAULT 'SERVICE',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    cpu_cores_required INT NOT NULL,
    memory_mb_required BIGINT NOT NULL,
    desired_state VARCHAR(50) NOT NULL DEFAULT 'RUNNING',
    actual_state VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    failure_reason TEXT,
    scheduled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workloads_org_state ON workloads(organisation_id, actual_state);
CREATE INDEX idx_workloads_assigned_node ON workloads(assigned_node_id);

-- 6. Scaling Policies
CREATE TABLE scaling_policies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    workload_id UUID NOT NULL UNIQUE REFERENCES workloads(id) ON DELETE CASCADE,
    cpu_scale_up_threshold NUMERIC(5,2) NOT NULL DEFAULT 80.00,
    cpu_scale_down_threshold NUMERIC(5,2) NOT NULL DEFAULT 30.00,
    memory_scale_up_threshold NUMERIC(5,2) NOT NULL DEFAULT 85.00,
    memory_scale_down_threshold NUMERIC(5,2) NOT NULL DEFAULT 35.00,
    min_replicas INT NOT NULL DEFAULT 1,
    max_replicas INT NOT NULL DEFAULT 10,
    cooldown_seconds INT NOT NULL DEFAULT 300,
    evaluation_window_seconds INT NOT NULL DEFAULT 300,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_scaled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 7. Scaling Events
CREATE TABLE scaling_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scaling_policy_id UUID NOT NULL REFERENCES scaling_policies(id) ON DELETE CASCADE,
    workload_id UUID NOT NULL REFERENCES workloads(id) ON DELETE CASCADE,
    action_type VARCHAR(20) NOT NULL,
    previous_replicas INT NOT NULL,
    new_replicas INT NOT NULL,
    trigger_metric VARCHAR(50) NOT NULL,
    trigger_value NUMERIC(5,2) NOT NULL,
    reasoning TEXT,
    executed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_scaling_events_workload ON scaling_events(workload_id, executed_at DESC);

-- 8. Node Metrics (Time-Series)
CREATE TABLE node_metrics (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    node_id UUID NOT NULL REFERENCES nodes(id) ON DELETE CASCADE,
    cpu_utilization_percent NUMERIC(5,2) NOT NULL,
    memory_utilization_percent NUMERIC(5,2) NOT NULL,
    disk_read_bytes_sec BIGINT NOT NULL DEFAULT 0,
    disk_write_bytes_sec BIGINT NOT NULL DEFAULT 0,
    network_rx_bytes_sec BIGINT NOT NULL DEFAULT 0,
    network_tx_bytes_sec BIGINT NOT NULL DEFAULT 0,
    active_workload_count INT NOT NULL DEFAULT 0,
    sampled_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_node_metrics_node_time ON node_metrics(node_id, sampled_at DESC);
CREATE INDEX idx_node_metrics_sampled_at ON node_metrics(sampled_at DESC);

-- 9. Alerts
CREATE TABLE alerts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    node_id UUID REFERENCES nodes(id) ON DELETE CASCADE,
    workload_id UUID REFERENCES workloads(id) ON DELETE CASCADE,
    severity VARCHAR(50) NOT NULL,
    alert_type VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    triggered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);

CREATE INDEX idx_alerts_org_status ON alerts(organisation_id, status);

-- 10. AI Recommendations
CREATE TABLE ai_recommendations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL,
    explanation TEXT NOT NULL,
    supporting_metrics JSONB NOT NULL DEFAULT '{}'::jsonb,
    confidence_score NUMERIC(4,3) NOT NULL,
    expected_impact VARCHAR(50) NOT NULL,
    suggested_action VARCHAR(50) NOT NULL,
    action_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    approval_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    approved_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    evaluated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_recommendations_org_status ON ai_recommendations(organisation_id, approval_status);

-- 11. Audit Logs (Immutable)
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    organisation_id UUID NOT NULL REFERENCES organisations(id) ON DELETE CASCADE,
    actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100) NOT NULL,
    previous_state JSONB,
    new_state JSONB,
    client_ip VARCHAR(45),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_org_time ON audit_logs(organisation_id, recorded_at DESC);
