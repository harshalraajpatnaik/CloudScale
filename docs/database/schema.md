# Database Schema & Indexing Design — CloudScale

## 1. Migration Strategy
All database schema changes are strictly versioned using **Flyway**.
- Migration scripts reside in `control-plane/src/main/resources/db/migration/`.
- File naming convention: `V{version}__{description}.sql` (e.g., `V1__init_schema.sql`, `V2__seed_roles.sql`).
- Schema rollback scripts are tested alongside forward migrations.

## 2. Table Specifications & Indexing Strategy

### `organisations`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY | Unique tenant identifier |
| `name` | VARCHAR(100) | NOT NULL | Display name |
| `slug` | VARCHAR(100) | NOT NULL UNIQUE | URL-friendly slug |
| `plan_tier` | VARCHAR(50) | NOT NULL DEFAULT 'FREE' | Tier (FREE, PRO, ENTERPRISE) |
| `max_cpu_quota` | INT | NOT NULL DEFAULT 64 | Maximum permitted CPU cores |
| `max_memory_mb_quota` | BIGINT | NOT NULL DEFAULT 131072 | Maximum memory quota in MB |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT NOW() | Creation timestamp |
| `updated_at` | TIMESTAMPTZ | NOT NULL DEFAULT NOW() | Last update timestamp |

**Indexes**:
- `idx_organisations_slug` ON `organisations(slug)`

---

### `users`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY | User identifier |
| `organisation_id` | UUID | NOT NULL REFERENCES organisations(id) | Tenant owner |
| `email` | VARCHAR(255) | NOT NULL UNIQUE | User email address |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt hash |
| `full_name` | VARCHAR(100) | NOT NULL | Display name |
| `role` | VARCHAR(50) | NOT NULL | RBAC role |
| `is_active` | BOOLEAN | NOT NULL DEFAULT TRUE | Active flag |
| `failed_login_attempts` | INT | NOT NULL DEFAULT 0 | Failed attempts counter |
| `locked_until` | TIMESTAMPTZ | NULL | Account lockout expiry |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT NOW() | Creation timestamp |

**Indexes**:
- `idx_users_email` ON `users(email)`
- `idx_users_org_id` ON `users(organisation_id)`

---

### `nodes`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY | Node identifier |
| `organisation_id` | UUID | NOT NULL REFERENCES organisations(id) | Tenant owner |
| `hostname` | VARCHAR(100) | NOT NULL UNIQUE | Hostname |
| `ip_address` | VARCHAR(45) | NOT NULL | IP or endpoint |
| `total_cpu_cores` | INT | NOT NULL | Total CPU cores |
| `total_memory_mb` | BIGINT | NOT NULL | Total memory in MB |
| `allocated_cpu_cores` | INT | NOT NULL DEFAULT 0 | Allocated CPU cores |
| `allocated_memory_mb` | BIGINT | NOT NULL DEFAULT 0 | Allocated memory in MB |
| `status` | VARCHAR(50) | NOT NULL DEFAULT 'ACTIVE' | State machine status |
| `is_simulated` | BOOLEAN | NOT NULL DEFAULT TRUE | Simulated flag |
| `last_heartbeat` | TIMESTAMPTZ | NOT NULL DEFAULT NOW() | Heartbeat timestamp |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT NOW() | Creation timestamp |

**Indexes**:
- `idx_nodes_org_status` ON `nodes(organisation_id, status)`
- `idx_nodes_heartbeat` ON `nodes(last_heartbeat)`

---

### `workloads`
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY | Workload identifier |
| `organisation_id` | UUID | NOT NULL REFERENCES organisations(id) | Tenant owner |
| `assigned_node_id` | UUID | NULL REFERENCES nodes(id) | Host node |
| `name` | VARCHAR(100) | NOT NULL | Name |
| `workload_type` | VARCHAR(50) | NOT NULL DEFAULT 'SERVICE' | Type (SERVICE, JOB, BATCH) |
| `priority` | VARCHAR(20) | NOT NULL DEFAULT 'NORMAL' | Priority tier |
| `cpu_cores_required` | INT | NOT NULL | CPU requirements |
| `memory_mb_required` | BIGINT | NOT NULL | Memory requirements |
| `desired_state` | VARCHAR(50) | NOT NULL DEFAULT 'RUNNING' | Desired state |
| `actual_state` | VARCHAR(50) | NOT NULL DEFAULT 'PENDING' | Current state |
| `failure_reason` | TEXT | NULL | Failure diagnostic details |
| `created_at` | TIMESTAMPTZ | NOT NULL DEFAULT NOW() | Creation timestamp |

**Indexes**:
- `idx_workloads_org_state` ON `workloads(organisation_id, actual_state)`
- `idx_workloads_assigned_node` ON `workloads(assigned_node_id)`

---

### `node_metrics` (Time-Series)
| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | UUID | PRIMARY KEY | Metric sample ID |
| `node_id` | UUID | NOT NULL REFERENCES nodes(id) | Associated node |
| `cpu_utilization_percent` | NUMERIC(5,2) | NOT NULL | CPU utilization % |
| `memory_utilization_percent` | NUMERIC(5,2) | NOT NULL | Memory utilization % |
| `disk_read_bytes_sec` | BIGINT | NOT NULL DEFAULT 0 | Disk read rate |
| `disk_write_bytes_sec` | BIGINT | NOT NULL DEFAULT 0 | Disk write rate |
| `network_rx_bytes_sec` | BIGINT | NOT NULL DEFAULT 0 | Network receive rate |
| `network_tx_bytes_sec` | BIGINT | NOT NULL DEFAULT 0 | Network transmit rate |
| `active_workload_count` | INT | NOT NULL DEFAULT 0 | Number of running workloads |
| `sampled_at` | TIMESTAMPTZ | NOT NULL | Timestamp of metric |

**Indexes**:
- `idx_node_metrics_node_time` ON `node_metrics(node_id, sampled_at DESC)`
- `idx_node_metrics_sampled_at` ON `node_metrics(sampled_at DESC)`
