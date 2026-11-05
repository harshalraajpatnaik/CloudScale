# ADR-002: Primary Datastore Selection — PostgreSQL 16 with Redis In-Memory Acceleration

## Status
Accepted

## Context
CloudScale requires transactional guarantees (ACID) for workload placement, tenant isolation, and audit trails. At the same time, it ingests continuous time-series metrics from nodes and requires rapid token revocation lookups.

## Decision
We select **PostgreSQL 16** as the authoritative relational database, supplemented by **Redis 7** for immediate token revocation sets and rate-limiting sliding windows.
- PostgreSQL provides strict foreign key constraints, transactional concurrency controls (`SELECT ... FOR UPDATE`), JSONB support for flexible metadata, and robust indexing.
- Flyway is used for deterministic, versioned database migrations.
- Redis acts as a fast, in-memory denylist for revoked JWT refresh tokens and sliding-window rate limit counters.

## Consequences
- **Positive**: Complete relational integrity; zero risk of orphan records or inconsistent resource states; fast lookups for token revocation without hammering PostgreSQL on every API call.
- **Negative**: Requires two containerized persistence services running in Docker Compose.
