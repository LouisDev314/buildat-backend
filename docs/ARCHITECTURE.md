# Architecture

This document owns engineering structure and constraints. [DOMAIN.md](DOMAIN.md) owns product invariants; [PLANS.md](../PLANS.md) owns delivery gates; [DECISIONS.md](DECISIONS.md) records unresolved architecture choices.

## Current foundation (2026-10-18)

This is one Maven Spring Boot application, `com.buildat:buildat-server:0.0.1-SNAPSHOT`, targeting Java 25 and Boot 4.1.1. Maven wrapper remains 3.9.16. Foundation implementation adds `config` and `http` support packages only; no domain modules, entities or ordinary application controllers exist. `ProblemErrorController` handles servlet error dispatches.

Security permits only GET/HEAD of the two exact health probe paths; every other ordinary route is denied even for an authenticated principal. Error dispatches may render safe errors. Form login, Basic, logout, request caching and sessions are disabled; generated local-user auto-configuration is excluded. CSRF is disabled for this stateless foundation with no cookie-authenticated operations. Supabase bearer authentication and CORS are deferred to the Auth milestone; do not add cookie authentication without revisiting CSRF.

Actuator exposes health only, with discovery disabled, JMX exposure disabled and details/components hidden regardless of authentication. Liveness includes process liveness only; readiness includes readiness state and database health. Probe bodies retain Actuator's status format, including 503 on failure, rather than application response envelopes.

`RequestIdFilter` runs before Security, generates an authoritative UUID, ignores caller IDs, stores it in a request attribute/MDC and returns `X-Request-ID`. Error dispatches reuse that attribute and MDC is restored/removed in `finally`. Async/streaming requires a separate context-propagation design. CORS exposure will be configured with exact frontend origins when an Auth client exists.

`ApiExceptionHandler`, `Problems` and the error controller use Spring Problem Details, stable codes and generic descriptions without rejected values or exception text. MVC protocol headers are preserved. Instances exclude query strings. Filter-level access failures and firewall rejections use the same contract. Unexpected MVC failures are logged once with request ID only. No general request/payload logging or custom interception framework is added.

Surefire runs Docker-free JUnit/MVC `*Test`; Failsafe runs `*IT` during `verify`. Testcontainers service connections supply JDBC/Flyway credentials independently of hosted configuration. The test profile enables Flyway against its container; its fallback datasource is intentionally unusable. Tests pin `postgres:17.11`, matching the user-confirmed hosted version on 2026-10-18. There is no skip-on-missing-Docker path. See [FOUNDATION.md](FOUNDATION.md) and [PLANS.md](../PLANS.md) for actual execution evidence and incomplete gates.

Historical baseline (2026-10-16): only an application entry point and external-datasource context test existed, with no explicit Security or HTTP contract. The revised plan supersedes those setup observations. The implementation began with a clean working tree; the plan's staged empty Dockerfile was absent. No Dockerfile is created or modified.

## Intended modular monolith

Use one backend application and PostgreSQL database, organized by domain responsibility. Boundaries below are intended logical modules, not existing packages or a requirement for separate Maven artifacts:

| Boundary | Owns | Permitted coordination |
| --- | --- | --- |
| Identity and profiles | Accounts, authentication integration, profile data and skills vocabulary | Exposes identity/profile views and authorization context |
| Projects and roles | Project ownership, publication, openings and disclosures | Supplies eligibility and project management checks |
| Collaboration | Applications, decisions, membership and participation authority | Coordinates transactional acceptance with project/role policy |
| Delivery and evidence | Milestones, contributions, provenance and delivery history | Validates project membership/authority through explicit interfaces |
| Discovery | Read queries and deterministic compatibility | Reads explicit views; never owns or changes source domain state |
| Notifications | Recipient notices and delivery state | Consumes committed domain facts; does not decide acceptance or membership |
| Trust and safety | Reports, moderation actions and restricted audit context | Invokes explicit restriction interfaces; no unrestricted cross-module writes |

Keep controller concerns, use-case transactions and persistence responsibilities distinct. Domain rules must not depend on HTTP DTOs or frontend state. Avoid exposing JPA entities as API contracts. Modules own writes to their state; cross-module workflows use explicit application services/interfaces. Prevent circular dependencies and a catch-all shared module. Select package layout and interfaces when implementing the first vertical slice.

Discovery can query PostgreSQL using explicit projections and indexes. Choose indexes from actual query needs. Do not add Redis, Kafka, Kubernetes, microservices, distributed architecture or dedicated search infrastructure without evidence and an explicit decision. Next.js/TypeScript with Axios and TanStack Query is the accepted frontend direction; repository location and package versions remain deferred. Supabase supplies Auth/hosted PostgreSQL, Resend is accepted for later transactional email. Northflank Sandbox is for disposable testing; AWS is the future alpha/production direction with compute/TLS undecided. The user owns provider setup, hosted changes and deployment.

## Persistence, transactions and migrations

Flyway is the schema authority; retain Hibernate validation and disabled open-in-view. Do not switch to automatic schema creation as a setup workaround. Use database constraints to enforce identities, references and agreed uniqueness in addition to service checks. Prefer explicit projections and transaction-bound mapping over lazy loading during serialization.

Place transaction boundaries at use cases requiring atomic outcomes, especially acceptance plus membership. Resolve conflicting acceptance, withdrawal, role closure and capacity writes with a deliberate locking/version strategy and database constraints. Document conflict and retry semantics. Idempotency is required where retries could duplicate state or side effects; choose keys/deduplication scope per operation instead of adding a universal mechanism without need.

Create notification/history facts with committed state. A simple transactional in-database approach is a candidate; an outbox or background worker is not yet selected. External delivery must not report success for rolled-back operations. Any retryable delivery needs deduplication and visible failure handling.

Preserve the empty V1 byte-for-byte. Before any hosted migration, the user must inspect existing schemas and Flyway history/checksums. Do not repair, baseline, clean, move history or rewrite potentially applied files; use additive migrations. Test migrations against PostgreSQL from empty and prior supported states. Plan compatible rollout, backfills, backups and recovery before destructive changes. The user confirmed hosted PostgreSQL 17.11 on 2026-10-18; tests match it with `postgres:17.11`. Normal development uses a dedicated Supabase database, automated tests use containers, and production requires a separate project. Prefer direct JDBC or the session pooler, with verified TLS and bounded Hikari pools. Hosted Flyway execution is disabled at application startup; the user runs migrations separately with DDL credentials. Runtime roles should have only necessary DML rights, no superuser/BYPASSRLS. Prefer an unexposed Buildat schema, browser roles without application-table privileges and reviewed default grants. Privileges, Data API exposure, RLS and Spring authorization are separate controls; table-level grants/schema work belong to the feature migration milestone. No schema or history location is selected here.

## Security and API contracts

Supabase is the sole accepted identity/session provider; Spring will validate JWTs locally with trusted issuer, audience, expiry/not-before, subject, asymmetric algorithm and configured JWKS. No replacement login/password/token/refresh system, per-request provider lookup or domain coupling to `auth.users` is planned. Auth implementation must verify actual token shape and signing mode, reject disallowed anonymous identities, test caching/rotation/unknown keys/provider failures, and never trust token-supplied key URLs.

The Auth milestone will resolve internal accounts by unique issuer/subject, enforce authoritative Buildat standing on every protected operation and preserve restricted/deleted mappings. Unknown-account bootstrap is idempotent with transactional race handling and a one-time provider existence/confirmation check; unavailable lookup creates nothing and returns 503. Existing valid JWTs may survive provider sign-out/deletion until expiry. Short lifetime review (provisionally 15 minutes), sign-out scope and privileged session verification remain Auth/operator decisions, not current behavior.

Each feature needs its action matrix and transaction-safe object authorization before implementation. Derive actors from trusted authentication; account metadata never grants platform authority. Managers require explicit project authority, ordinary members have no implied management rights, and platform privileges confer no ordinary-business bypass. Deferred policies remain in [DOMAIN.md](DOMAIN.md).

Validate inputs and safe user-content rendering; avoid fetching arbitrary evidence URLs server-side without a justified, protected design. Keep secrets in environment/service configuration, private fields out of public DTOs, and sensitive payloads out of logs. Select proportionate abuse controls for exposed actions. No money movement or investment handling belongs in MVP.

Accepted application success JSON is `{ "requestId": "...", "data": ... }`; errors use RFC 9457 Problem Details. Success advice and springdoc are deliberately deferred until the first real `/api/v1` application endpoint. Advice must preserve statuses/headers and exclude errors, Actuator, docs, empty/HEAD/204/304, downloads and streams; generated OpenAPI must model the actual envelope. Define pagination and idempotency with real resources. Current Problem Details/request correlation behavior is described above.

## Observability, tests and deployment

Use structured operational logs, safe correlation and minimal metrics for request failures, database/migration health, application outcomes and notification failures. Define health/readiness and Actuator exposure; dependency presence alone does not prove production readiness. Avoid logging credentials, application text, reports or unnecessary personal data. Product measurement definitions live in [MVP.md](MVP.md).

Use JUnit for domain transitions and deterministic compatibility rules. Add PostgreSQL integration tests, with Testcontainers as appropriate, for migrations, constraints, transaction rollback and concurrent decisions. Add authorization coverage for other-user access and privileged actions. The old external-datasource context test is replaced with container integration coverage; no domain schema or business behavior is proved by an empty entity metamodel. Add contract/frontend journey checks when those layers exist.

AWS compute, TLS topology and frontend deployment remain unresolved; Northflank Sandbox is only for disposable tests, operated by the user. Start with the smallest suitable application/database deployment; do not provision services during foundation documentation. Before alpha, define environment separation, secrets injection, TLS, migration execution, restricted operational endpoints, backups and tested restoration, rollback/recovery, and a moderation operator. External changes and deployment require user authorization. [README.md](../README.md) contains only repository-supported local commands, with actual validation outcomes recorded in the roadmap.
