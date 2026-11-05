# Security Architecture — CloudScale

## 1. Principles
- **Zero Trust at Trust Boundaries**: Every internal API request requires validated credentials. Services never trust client input without schema and authorization verification.
- **Tenant Isolation by Construction**: All domain repositories and queries enforce `WHERE organisation_id = :tenantId`. Entity models enforce organization binding.
- **Principle of Least Privilege**: Users possess only the permissions required for their designated role (`SUPER_ADMIN`, `ORGANISATION_ADMIN`, `INFRASTRUCTURE_OPERATOR`, `VIEWER`).
- **Cryptographic Rigor**: Asymmetric RS256 token signing, BCrypt with 12 rounds for password hashing, and SHA-256 for token storage digests.

## 2. Authentication Flow & Token Lifecycle

### Token Specifications
| Token Type | Algorithm | Lifetime | Storage Location | Invalidation Mechanism |
|------------|-----------|----------|------------------|------------------------|
| Access Token | RS256 | 15 minutes | In-memory / Bearer Header | Expiration & Key Rotation |
| Refresh Token | Opaque / SHA-256 | 30 days | HttpOnly Cookie / DB | DB Revocation & Redis Blacklist |

### Refresh Token Single-Use Rotation
1. Client presents refresh token `R1`.
2. Control plane validates `R1` against database hash and Redis revocation set.
3. If `R1` was already revoked, a token reuse attack is detected: all refresh tokens for that user are immediately invalidated, and an alert is recorded in `audit_logs`.
4. If `R1` is valid: `R1` is marked as revoked in PostgreSQL and added to Redis revocation set. A new access token `A2` and fresh refresh token `R2` are issued atomically.

## 3. Role-Based Access Control (RBAC) Matrix

| Resource / Action | SUPER_ADMIN | ORGANISATION_ADMIN | INFRASTRUCTURE_OPERATOR | VIEWER |
|-------------------|:-----------:|:------------------:|:-----------------------:|:------:|
| Manage Organizations | ✅ | ❌ | ❌ | ❌ |
| Manage Platform Settings | ✅ | ❌ | ❌ | ❌ |
| Manage Org Users | ✅ | ✅ | ❌ | ❌ |
| Register / Decommission Nodes | ✅ | ✅ | ❌ | ❌ |
| Create / Terminate Workloads | ✅ | ✅ | ✅ | ❌ |
| Change Autoscaling Policies | ✅ | ✅ | ❌ | ❌ |
| Approve AI Recommendations | ✅ | ✅ | ✅ | ❌ |
| View Telemetry & Dashboards | ✅ | ✅ | ✅ | ✅ |
| View Audit Logs | ✅ | ✅ | ✅ | ✅ |

## 4. Threat Model & Mitigations
- **Cross-Tenant Data Tampering**: Prevented by strict JPA entity listener validation, Spring Security method-level security (`@PreAuthorize("@tenantService.hasAccess(#orgId)")`), and integration test assertions.
- **Brute Force Login**: Monitored per email and client IP; accounts locked for 15 minutes after 5 consecutive failures.
- **SQL Injection**: Prevented through Hibernate parameterized HQL/Criteria queries; zero dynamic raw SQL concatenation.
- **Cross-Site Scripting (XSS)**: Angular template context-aware sanitization by default; Content Security Policy (CSP) headers applied via Spring Security.
- **AI Agent Exploitation**: AI agent lacks direct DB connection strings or bash execution capabilities; restricted strictly to controlled read-only internal REST endpoints.
