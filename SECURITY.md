# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

## Reporting a Vulnerability

We take the security of CloudScale seriously. If you believe you have found a security vulnerability in any part of this repository, please report it immediately.

### Process
1. **Do not disclose publicly**: Please refrain from opening public GitHub issues, discussions, or pull requests regarding suspected security vulnerabilities.
2. **Email the maintainer**: Send an email directly to `68125965+harshalraajpatnaik@users.noreply.github.com` with:
   - Type of issue (e.g., buffer overflow, SQL injection, privilege escalation, cross-tenant leak).
   - Full steps to reproduce the vulnerability.
   - Proof-of-concept exploit code or HTTP request samples if available.
   - Any suggested mitigations.
3. **Response timeline**: You will receive an acknowledgment within 48 hours and an initial triage assessment within 5 business days.

## Security Architecture Principles in CloudScale
- **Zero Trust at Service Boundaries**: All tenant access is validated at the service layer and database query level.
- **Stateless Tokens with Revocation**: RS256 JWT access tokens with Redis-backed revocation sets for immediate token blacklisting.
- **Strict Role-Based Access Control**: `SUPER_ADMIN`, `ORGANISATION_ADMIN`, `INFRASTRUCTURE_OPERATOR`, and `VIEWER` roles enforced via Spring Security `@PreAuthorize` annotations.
- **Cryptographic Standards**: BCrypt (cost factor 12) for credentials, SHA-256 for token hashing.
- **No Direct Agent DB Access**: The Python AI intelligence agent interacts strictly through authenticated, scoped REST endpoints and cannot execute raw queries or shell commands.
