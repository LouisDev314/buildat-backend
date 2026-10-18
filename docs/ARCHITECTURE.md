# Architecture

This document owns engineering structure and constraints. [DOMAIN.md](DOMAIN.md) owns product invariants; [PLANS.md](../PLANS.md) owns delivery gates; [DECISIONS.md](DECISIONS.md) records unresolved architecture choices.

## Current backend (2026-10-18)

One Maven Boot 4.1.1/Java 25 application, with Maven 3.9.16. `config` and `http` own shared framework concerns. `auth` owns Supabase JWT/provider integration; `account` owns internal account services and package-private JPA persistence, with HTTP DTOs under `account.api`. No profiles or collaboration modules are implemented.

Security is stateless, disables Basic/form/session login, and authenticates only the two `/api/v1/me` operations plus HEAD retrieval. Exact public health probes and safe error dispatches remain. Unknown routes are denied, including authenticated requests. Local-only docs are enabled conditionally; production disables them. No CORS origin is opened. Account services derive the actor from validated JWT authentication and read authoritative standing without cross-request caching.

`RequestIdFilter` runs before Security, generates a UUID, ignores caller IDs and restores MDC after requests/error dispatches. `Problems`, MVC advice and servlet errors keep safe RFC 9457 errors with stable code/requestId and protocol headers. Bearer failures share this contract. Marked JSON controllers return DTOs wrapped by scoped success advice as requestId/data; status/headers survive. HEAD/empty, framework errors, Actuator/docs, downloads and streams are excluded. springdoc 3.1.1 describes concrete wrapped account responses and errors, with runtime-schema integration coverage.

Actuator remains status-only. Liveness excludes dependencies; readiness includes DB. JWKS/Auth failure is handled at affected requests, not added as an unconditional readiness dependency.

Surefire runs Docker-free JUnit/MVC tests (including loopback synthetic JWKS), Failsafe runs PostgreSQL/real-HTTP integration tests. PostgreSQL is pinned to 17.11; Boot service connections and unusable fallback URLs isolate tests from hosted configuration. Tests provision synthetic roles only inside disposable containers. Runtime-login HTTP coverage uses DML credentials separately from migration ownership. Actual execution evidence belongs in [PLANS.md](../PLANS.md).

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

Preserve the empty V1 byte-for-byte. Before any hosted migration, the user must inspect existing schemas and Flyway history/checksums. Do not repair, baseline, clean, move history or rewrite potentially applied files; use additive migrations. Test migrations against PostgreSQL from empty and prior supported states. Plan compatible rollout, backfills, backups and recovery before destructive changes. The user confirmed hosted PostgreSQL 17.11 on 2026-10-18; tests match it with `postgres:17.11`. Normal development uses a dedicated Supabase database, automated tests use containers, and production requires a separate project. Prefer direct JDBC or the session pooler, with verified TLS and bounded Hikari pools. Default/deployment startup disables Flyway. The explicitly approved local-development authority automatically migrates pending SQL using separate credentials; Northflank does not. Future production uses a dedicated automatic release step. Hosted verification and exact artifact authorization are prerequisites, not completed work. Runtime roles should have only necessary DML rights, no superuser/BYPASSRLS. Prefer an unexposed Buildat schema, browser roles without application-table privileges and reviewed default grants. Privileges, Data API exposure, RLS and Spring authorization are separate controls; table-level grants/schema work belong to the feature migration milestone. Application objects use private `buildat`; history remains in its user-verified existing schema. V2 requires a pre-created runtime group; it never creates hosted roles. Runtime receives SELECT/INSERT and column-only UPDATE(updated_at) for PostgreSQL row locking, with RLS policies; no standing/identity updates, DELETE or DDL. See the [Auth runbook](AUTHENTICATION.md).

## Security and API contracts

Supabase is the sole identity/session provider. Spring Security/Nimbus validates trusted asymmetric JWKS, issuer, audience, expiry/issued-at, optional not-before, canonical subject/session UUIDs, expected user-token role and Boolean anonymous state. No custom crypto, replacement login or provider call on every operation. Only the configured ES256/RS256 subset is trusted; token key URLs and editable metadata grant no authority.

Unknown-account bootstrap alone calls the provider user endpoint to verify matching permanent identity and confirmation state before opening the creation transaction. PostgreSQL targeted ON CONFLICT plus locked read ensures one issuer/subject mapping and checks current standing. Existing ACTIVE mappings need no provider lookup; SUSPENDED/DELETED mappings are retained and denied. Account GET never provisions. No reactivation, administration API or full deletion workflow exists. Provider revocation/deletion has an expiry/cache window; DB unavailability fails closed. Confirmation is provider state, not proof of historical mailbox verification when auto-confirm was used. Full behavior and failure codes are in [AUTHENTICATION.md](AUTHENTICATION.md).

Each feature needs its action matrix and transaction-safe object authorization before implementation. Derive actors from trusted authentication; account metadata never grants platform authority. Managers require explicit project authority, ordinary members have no implied management rights, and platform privileges confer no ordinary-business bypass. Deferred policies remain in [DOMAIN.md](DOMAIN.md).

Validate inputs and safe user-content rendering; avoid fetching arbitrary evidence URLs server-side without a justified, protected design. Keep secrets in environment/service configuration, private fields out of public DTOs, and sensitive payloads out of logs. Select proportionate abuse controls for exposed actions. No money movement or investment handling belongs in MVP.

Accepted application success JSON is `{ "requestId": "...", "data": ... }`; errors use RFC 9457 Problem Details. Success advice and springdoc are implemented for the first `/api/v1/me` endpoints. Advice must preserve statuses/headers and exclude errors, Actuator, docs, empty/HEAD/204/304, downloads and streams; generated OpenAPI must model the actual envelope. Define pagination and idempotency with real resources. Current Problem Details/request correlation behavior is described above.

## Observability, tests and deployment

Use structured operational logs, safe correlation and minimal metrics for request failures, database/migration health, application outcomes and notification failures. Define health/readiness and Actuator exposure; dependency presence alone does not prove production readiness. Avoid logging credentials, application text, reports or unnecessary personal data. Product measurement definitions live in [MVP.md](MVP.md).

Use JUnit for domain transitions and deterministic compatibility rules. Add PostgreSQL integration tests, with Testcontainers as appropriate, for migrations, constraints, transaction rollback and concurrent decisions. Add authorization coverage for other-user access and privileged actions. The old external-datasource context test is replaced with container integration coverage; no domain schema or business behavior is proved by an empty entity metamodel. Add contract/frontend journey checks when those layers exist.

AWS compute, TLS topology and frontend deployment remain unresolved; Northflank Sandbox is only for disposable tests, operated by the user. Start with the smallest suitable application/database deployment; do not provision services during foundation documentation. Before alpha, define environment separation, secrets injection, TLS, migration execution, restricted operational endpoints, backups and tested restoration, rollback/recovery, and a moderation operator. External changes and deployment require user authorization. [README.md](../README.md) contains only repository-supported local commands, with actual validation outcomes recorded in the roadmap.
