# Contributing to CloudScale

Thank you for your interest in contributing to **CloudScale** — an intelligent cloud infrastructure management and predictive resource orchestration platform.

## Code of Conduct
Please be respectful, collaborative, and professional in all interactions within issues, pull requests, and discussions.

## Development Principles
1. **Modular Architecture**: Maintain clean package and service boundaries. Avoid circular dependencies.
2. **Quality & Evidence Before Claims**: Never claim a feature works without passing automated tests. Simulated components must be explicitly labelled as simulated.
3. **No Hardcoded Secrets**: Use environment variables and `.env.example`.
4. **Tenant Isolation**: Every database interaction with tenant-scoped entities must verify organisation boundaries.
5. **Security First**: All state-changing endpoints must enforce authentication and authorization.

## Workflow
1. Fork or branch from `main`.
2. Ensure your changes compile, pass linter checks, and satisfy unit/integration tests:
   - Backend: `mvn test`
   - Frontend: `npm test`
   - AI Service: `pytest`
3. Commit with semantic commit messages: `feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`.
4. Submit a Pull Request with a clear description and testing evidence.
