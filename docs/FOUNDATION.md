# Backend foundation runbook

Phase 1 implemented 2026-10-18. **Current Phase 2 startup/Auth/migration instructions are in [AUTHENTICATION.md](AUTHENTICATION.md)**; the Phase 1 setup/policy sections below are retained as historical evidence and are not current activation instructions. [PLANS.md](../PLANS.md) owns gates and one next task; [DECISIONS.md](DECISIONS.md) distinguishes accepted direction from implementation.

## Prerequisites and commands

Java 25 and the declared Maven wrapper are required. The wrapper is not executable in this checkout: use `sh mvnw` on POSIX, or `.\mvnw.cmd` on Windows. Maven uses its ordinary user cache and may download dependencies. Do not change dependency/wrapper versions to bypass environment problems.

```sh
java -version
sh mvnw --version
docker version
docker info
sh mvnw --batch-mode test
sh mvnw --batch-mode verify
```

`test` runs JUnit/Surefire `*Test` (unit/MVC), without Docker or a datasource. `verify` additionally runs Failsafe `*IT`, starts the application on a random HTTP port and creates/removes a disposable Testcontainers PostgreSQL database. Tests pin `postgres:17.11`, matching the hosted version confirmed by the user. Boot-managed Testcontainers version is 2.0.5. Review this pin deliberately if hosted compatibility changes; no arbitrary major, latest tag or automatic upgrade.

Start a compatible Docker engine before `verify`. Missing Docker is a visible failure, never a skipped test or fallback to Supabase. Initial image downloads need network access. Test service connections supply separate container credentials to both JPA and Flyway. The `test` profile enables Flyway and sets unusable datasource fallback values; personal `DATABASE_*` values are not needed. Never use this profile for normal hosted startup.

The final outage test stops its own disposable database after the checks that need it. Expect framework database-health warnings in this test. Readiness must fail while liveness stays healthy. No container reuse or shared database is configured.

## Historical Phase 1 hosted setup — revised by Phase 2

Use the designated Supabase development project, with synthetic data and no concurrent migration experiments. Production will need a separate project. No local PostgreSQL server or Compose stack is required.

Set process environment or unshared IDE configuration:

| Name | Purpose |
| --- | --- |
| `DATABASE_URL` | JDBC URL, without embedded credentials; direct endpoint or verified session pooler |
| `DATABASE_USERNAME` | Runtime role, restricted to required application privileges |
| `DATABASE_PASSWORD` | Runtime password; never commit or paste into chat |
| `DATABASE_POOL_MAX_SIZE` | Optional maximum pool size, default 5; budget across all processes |

Spring does not load `.env` automatically. For hosted JDBC, configure `sslmode=verify-full` and the provider CA with `sslrootcert` in the JDBC URL; the CA path is local configuration, not a committed secret. There is no separate `DATABASE_SSL_ROOT_CERT` wiring yet. Do not use transaction pooling for this persistent MVC/JPA runtime without a separate compatibility review. Minimum idle is 1 and acquisition timeout is 5 seconds.

Ordinary startup:

```sh
sh mvnw spring-boot:run
```

Run this only after the user has verified the hosted environment; it was not used as validation in this task. Current implementation has no Auth or domain API. All ordinary routes are denied except the two probes.

## Historical Phase 1 migration policy — superseded in part

Normal startup sets `spring.flyway.enabled=false`. Hibernate stays `validate`; open-in-view stays disabled. Testcontainers enables Flyway only for disposable databases. V1 remains empty and unchanged.

Before the first hosted migration, the user must inspect actual schemas/history/checksums and compare with the repository. Preserve the established history location. Stop on conflicts; never repair, baseline, clean, relocate history or rewrite potentially applied migrations. Prepare additive migrations as features are approved. Separate migration DDL credentials from runtime DML credentials. Flyway remains the only migration authority; do not introduce Supabase CLI migrations.

No hosted migration runner/command or grants are implemented in this milestone. Proposed `FLYWAY_URL`, `FLYWAY_USER`, `FLYWAY_PASSWORD` are **not wired** to runtime Spring configuration. The user executes hosted changes after artifact review. Future schema work should isolate application objects from Data API/browser access, restrict default privileges and evaluate RLS separately from SQL grants and Spring authorization.

## Historical Phase 1 HTTP contract

| Route | Public behavior |
| --- | --- |
| GET/HEAD `/actuator/health/liveness` | Process liveness, status only |
| GET/HEAD `/actuator/health/readiness` | Initialization/readiness state and DB, status only; 503 when unavailable |
| Any other ordinary route | Denied, including aggregate health, operational endpoints and login |

Successful probe JSON is Actuator status output, for example `{ "status": "UP" }`. Failed probes retain the same status-only contract. They are excluded from future application response envelopes. Actuator discovery and JMX exposure are disabled; components/details are hidden.

Every request gets server-controlled `X-Request-ID`, independent of incoming IDs. It is stored in a request attribute and MDC, reused on servlet error dispatches and cleaned/restored in `finally`. Async/streaming propagation is deferred. No CORS origin is opened in this milestone; Auth/frontend work must add an exact allowlist and expose this header.

Security, firewall, MVC and servlet-dispatch errors use `application/problem+json` with `type`, `title`, numeric `status`, generic `detail`, query-free `instance`, stable `code` and `requestId`. Validation failures add field/code/generic-message entries without rejected values. Framework protocol headers such as `Allow` survive. No exception text or stack trace is returned. Unexpected MVC exceptions log one generic request-ID message; no payload, query string, token or full exception is logged by the handler.

There is no Auth mechanism: route denial is a safe baseline, not proof of product authorization. Form login, Basic and API sessions are disabled; generated local users are excluded. Supabase Auth, account tables/standing, bearer challenges and object authorization come next. Success response advice and springdoc wait for the first real application JSON API, whose accepted format is `{ "requestId": "...", "data": ... }`.

## Historical Phase 1 validation evidence — 2026-10-18

| Exact command/check | Result |
| --- | --- |
| `java -version` | Microsoft OpenJDK 25.0.4.1 |
| `./mvnw --version` | Non-executable wrapper; used `sh` without changing mode |
| `sh mvnw --version` | Maven 3.9.16, Java 25.0.4.1; wrapper download/cache succeeded with required sandbox access |
| `docker version` / `docker info` | Initially blocked by sandbox, then engine stopped; user started Docker |
| `docker version --format '{{.Server.Version}}'` | 27.3.1 |
| `docker info --format '{{.OSType}} {{.Architecture}}'` | `linux aarch64` |
| `sh mvnw --batch-mode test` | PASS: 7 tests, 0 failures/errors/skips |
| `sh mvnw --batch-mode verify` | PASS: 7 unit/MVC + 4 PostgreSQL/HTTP integration tests, 0 failures/errors/skips |
| `git diff --check` | PASS |
| `git diff --cached --check` | PASS; no staged changes |
| V1 comparison to HEAD | Identical, zero bytes |
| Documentation local-link check | PASS; local Markdown targets exist |
| `git status --short --untracked-files=all` / final diff review | Only intended foundation/docs/test changes; no Dockerfile or unrelated pre-existing changes |

Initial compilation/fixture and integration ordering failures were corrected before the passing runs. Maven's JDK agent warnings come from the existing test framework/Mockito and are not skipped tests. Full verification proves clean isolated startup and migration behavior against PostgreSQL 17.11; it does not prove domain schema correctness (there are no production entities), Supabase Auth, hosted privileges/TLS/history, packaging startup, deployment or production readiness.

No hosted database/service was contacted, no provider configuration changed, and no deployment, commit or push occurred. These checks remain outside this milestone. The [architecture](ARCHITECTURE.md) records accepted future boundaries; [decision register](DECISIONS.md) retains superseded rationale.

## Phase 2 current operational policy

Default/deployment startup retains Flyway disabled, Hibernate validation and disabled open-in-view. Local automatic migration is now supported only through the explicit local-development profile, separate credentials, verified history schema and `BUILDAT_LOCAL_MIGRATIONS_APPROVED=true` after hosted verification and exact SQL authorization. Northflank is not a competing runner. Production's future release job will migrate automatically once; replicas will not.

V1 is unchanged; V2 creates minimal private accounts and RLS/grants. Tests establish V1→V2 migration, nonempty entity validation, runtime-role bootstrap, concurrency, rollback and HTTP contracts on disposable PostgreSQL. They do not establish hosted compatibility or settings. Follow the ordered [user-only Auth setup](AUTHENTICATION.md#ordered-user-only-setup) before any hosted activation. Final Phase 2 commands/results are recorded in [PLANS.md](../PLANS.md).
