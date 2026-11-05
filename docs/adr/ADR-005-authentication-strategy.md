# ADR-005: Authentication & Token Management Strategy

## Status
Accepted

## Context
A cloud control plane requires secure authentication across client-to-backend and service-to-service communication, support for role-based permissions, multi-tenancy, and protection against token theft.

## Decision
We adopt **asymmetric RS256 JWT access tokens** paired with **cryptographically random single-use refresh tokens**:
1. **Access Tokens**:
   - Signed using RS256 (private key held by Spring Boot, public key verifiable).
   - Short lifespan (15 minutes).
   - Contains user ID, email, role, and `organisation_id` claims.
2. **Refresh Tokens**:
   - Single-use token rotation: refreshing an access token invalidates the old refresh token and issues a new one.
   - Stored hashed in PostgreSQL with expiry (30 days).
   - Instant revocation supported via a Redis denylist set (`SISMEMBER revoked_tokens <hash>`).
   - If an already-revoked refresh token is presented, all refresh tokens for that user are revoked immediately (reuse attack detection).
3. **Password Security**:
   - BCrypt hashing with strength 12.
   - Account lockout after 5 consecutive failed attempts.

## Consequences
- **Positive**: Stateless access verification on most calls; immediate revocation capability for security emergencies; defense against token replay attacks.
- **Negative**: Requires maintaining token rotation logic and Redis denylist synchronization.
