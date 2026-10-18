# Buildat backend

Buildat is a project-first collaboration network, initially for indie-game development. Creators recruit for defined roles; builders join teams and build a history of meaningful contributions. Product features remain planned.

This Maven backend targets Java 25 and Spring Boot 4.1.1 with MVC, validation, Security, JPA, PostgreSQL, Flyway and Actuator. It now has explicit route denial, safe health probes, request correlation and Problem Details. No Auth integration, domain entities, application API, frontend or deployment configuration is implemented.

## Development and verification

Normal development uses a dedicated Supabase PostgreSQL project. No local PostgreSQL installation or Compose stack is required. Automated database tests use disposable PostgreSQL Testcontainers and never require Supabase credentials.

Prerequisites: Java 25; wrapper-selected Maven 3.9.16 (downloads when uncached); a working Docker-compatible engine for integration tests; and PostgreSQL test image downloads when uncached. The hosted version was confirmed as 17.11. On POSIX, invoke the non-executable checked-in wrapper through `sh`.

| Purpose | POSIX shell | PowerShell / Windows |
| --- | --- | --- |
| Maven/JDK | `sh mvnw --version` | `.\mvnw.cmd --version` |
| Unit and MVC tests, no database/container | `sh mvnw --batch-mode test` | `.\mvnw.cmd --batch-mode test` |
| Unit + PostgreSQL/HTTP integration tests | `sh mvnw --batch-mode verify` | `.\mvnw.cmd --batch-mode verify` |
| Normal development startup | `sh mvnw spring-boot:run` | `.\mvnw.cmd spring-boot:run` |

Tests pin `postgres:17.11`, matching the user-confirmed development database version. Review the pin before changing the hosted major. Missing Docker fails integration tests; tests do not skip or use a hosted database. See the [runbook](docs/FOUNDATION.md).

For normal development startup, supply `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` in the process environment or an unshared IDE configuration. Do not paste secrets into chat or commit them. `.env` is ignored but is not automatically loaded by Spring. Hosted JDBC must use certificate/hostname verification (`sslmode=verify-full` and the provider CA through `sslrootcert`); use a direct connection or verified session pooler. Default Hikari maximum is 5 (`DATABASE_POOL_MAX_SIZE`), minimum idle 1, acquisition timeout 5 seconds.

Ordinary startup **does not run Flyway**. The user operates hosted migrations separately with dedicated migration credentials after inspecting existing history. Hibernate stays `validate`, open-in-view stays disabled. V1 is unchanged and empty. No hosted migration command/tool is provided yet; migration variables are not implicitly wired to Spring. Do not start against Supabase to substitute for isolated verification.

Only GET/HEAD `/actuator/health/liveness` and `/actuator/health/readiness` are public. They expose status only; readiness includes the database. Other routes are denied, including aggregate health, operational endpoints and login. Errors use `application/problem+json` with a stable `code` and `requestId`; all requests receive a server-generated `X-Request-ID`. No form login, Basic authentication or API sessions are enabled.

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
