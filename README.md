# Buildat backend

Buildat is a project-first collaboration network, initially for indie-game development. Creators recruit for defined roles; builders join teams and build a history of meaningful contributions. Product features remain planned.

This Maven backend targets Java 25 and Spring Boot 4.1.1 with MVC, validation, Security, JPA, PostgreSQL, Flyway and Actuator. It includes Supabase JWT authentication, minimal persistent internal accounts, safe health probes, request correlation, application success envelopes and Problem Details. Profiles, frontend and deployment remain deferred. Hosted activation has not been performed.

## Development and verification

Normal development uses a dedicated Supabase PostgreSQL project. No local PostgreSQL installation or Compose stack is required. Automated database tests use disposable PostgreSQL Testcontainers and never require Supabase credentials.

Prerequisites: Java 25; wrapper-selected Maven 3.9.16 (downloads when uncached); a working Docker-compatible engine for integration tests; and PostgreSQL test image downloads when uncached. The hosted version was confirmed as 17.11. On POSIX, invoke the non-executable checked-in wrapper through `sh`.

| Purpose | POSIX shell | PowerShell / Windows |
| --- | --- | --- |
| Maven/JDK | `sh mvnw --version` | `.\mvnw.cmd --version` |
| Unit and MVC tests, no database/container | `sh mvnw --batch-mode test` | `.\mvnw.cmd --batch-mode test` |
| Unit + PostgreSQL/HTTP integration tests | `sh mvnw --batch-mode verify` | `.\mvnw.cmd --batch-mode verify` |
| Approved local development startup | `sh mvnw spring-boot:run -Dspring-boot.run.profiles=local-development` | `.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local-development` |

Tests pin `postgres:17.11`, matching the user-confirmed development database version. Review the pin before changing the hosted major. Missing Docker fails integration tests; tests do not skip or use a hosted database. See the [runbook](docs/FOUNDATION.md).

For normal development startup, supply `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` in the process environment or an unshared IDE configuration. Do not paste secrets into chat or commit them. `.env` is ignored but is not automatically loaded by Spring. Hosted JDBC must use certificate/hostname verification (`sslmode=verify-full` and the provider CA through `sslrootcert`); use a direct connection or verified session pooler. Default Hikari maximum is 5 (`DATABASE_POOL_MAX_SIZE`), minimum idle 1, acquisition timeout 5 seconds.

Default/deployment startup **does not run Flyway**. The explicit `local-development` profile binds to loopback and enables local Swagger/OpenAPI. It runs pending SQL automatically only after hosted project/history/privilege verification, exact SQL review and authorization, with `BUILDAT_LOCAL_MIGRATIONS_APPROVED=true`. It uses separate migration credentials and the verified history schema. Northflank does not independently migrate; future production uses one dedicated automatic release step, never every replica. V1 remains unchanged. V2 is prepared and tested only against disposable databases; hosted conflicts stop activation.

Configure Auth through the public project URL, exact issuer/JWKS/audience, verified ES256 or RS256 allowlist and publishable key, alongside private JDBC credentials. Missing configuration fails startup. No Auth admin key or shared JWT secret is needed. See [.env.example](.env.example) and the [authentication runbook](docs/AUTHENTICATION.md) for the complete ordered user-only setup, privilege requirements and separate hosted authorization gate. Do not run hosted startup as test verification.

POST `/api/v1/me/bootstrap` provisions once after provider confirmation verification (`201`, then `200`); GET `/api/v1/me` retrieves only (`403 ACCOUNT_BOOTSTRAP_REQUIRED` before provisioning). Success is `{ "requestId": "...", "data": { "id": "..." } }`. JWT validation is local with cached trusted keys; every account operation reads authoritative standing. Suspended/deleted accounts cannot recreate themselves. Provider deletion/sign-out can leave access JWTs usable until expiry; this is not immediate provider revocation.

Only GET/HEAD `/actuator/health/liveness` and `/actuator/health/readiness` are unconditionally public. They expose status only; readiness includes the database. Unlisted routes remain denied. Errors use `application/problem+json` with stable `code`/`requestId`; all requests receive a server-generated `X-Request-ID`. No form login, Basic authentication, API sessions or open CORS policy exists. Swagger and `/v3/api-docs` are disabled outside explicitly local development by default.

## Documentation

| Document | Owns |
| --- | --- |
| [AGENTS.md](AGENTS.md) | Standing instructions for future Codex work |
| [PLANS.md](PLANS.md) | Current evidence, implementation order and acceptance gates |
| [PRODUCT.md](docs/PRODUCT.md) | Thesis, audience, positioning and long-term vision |
| [MVP.md](docs/MVP.md) | MVP scope, behavior and private-alpha measures |
| [DOMAIN.md](docs/DOMAIN.md) | Concepts, ownership, permissions and consistency |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Observed architecture and intended engineering direction |
| [BUSINESS.md](docs/BUSINESS.md) | Commercial hypotheses and future funding constraints |
| [DECISIONS.md](docs/DECISIONS.md) | Accepted, proposed and deferred decisions |

Read the roadmap and relevant documents before implementation. The [foundation runbook](docs/FOUNDATION.md) records setup, validation and current blockers. Existing ignored `HELP.md`, when present, contains generated Spring/Maven references and remains unchanged.

Phase 2 operational details: [Authentication and accounts](docs/AUTHENTICATION.md).
