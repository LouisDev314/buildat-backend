# Architecture

This document owns engineering structure and constraints. [DOMAIN.md](DOMAIN.md) owns product invariants; [PLANS.md](../PLANS.md) owns delivery gates; [DECISIONS.md](DECISIONS.md) records unresolved architecture choices.

## Observed repository on 2026-10-16

The repository is one Maven Spring Boot application, `com.buildat:buildat-server:0.0.1-SNAPSHOT`. The POM declares Java 25 and Boot 4.1.1, matching intended versions. The wrapper selects Maven 3.9.16 (wrapper 3.3.4). Spring MVC, validation, Data JPA, Flyway/PostgreSQL, Security and Actuator dependencies are present; the imported JUnit Jupiter test has one empty `contextLoads` method under `@SpringBootTest`. Testcontainers is absent. Managed transitive versions and installed runtime versions have not been verified.

The only Java production source is the application entry point. Pre-existing unused executor imports seen at initial inspection were removed outside this documentation task during validation; no threading architecture is present. There are no domain packages, entities, repositories, controllers, explicit security configuration, API response/error types, requestId mechanism or notification implementation.

`application.yaml` reads `DATABASE_URL`, `DATABASE_USERNAME` and `DATABASE_PASSWORD`; Hibernate uses `ddl-auto: validate`, open-in-view is disabled and Flyway is enabled. The sole `V1__init.sql` is empty; it was staged at initial inspection and untracked at final inspection following changes outside this documentation task. No schema is established. Whether it has run on any database is unknown. Existing ignored compiled output is not evidence of successful current validation.

No frontend, CI workflow, container/provisioning setup or deployment configuration was found. Security and Actuator starters do not establish product authorization, an operational monitoring policy or verified endpoint behavior. No build or database connection was run for this documentation task.

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

Discovery can query PostgreSQL using explicit projections and indexes. Choose indexes from actual query needs. Do not add Redis, Kafka, Kubernetes, microservices, distributed architecture or dedicated search infrastructure without evidence and an explicit decision. Next.js/TypeScript is the intended separate frontend; location, deployment and API integration remain unresolved.

## Persistence, transactions and migrations

Flyway is the schema authority; retain Hibernate validation and disabled open-in-view. Do not switch to automatic schema creation as a setup workaround. Use database constraints to enforce identities, references and agreed uniqueness in addition to service checks. Prefer explicit projections and transaction-bound mapping over lazy loading during serialization.

Place transaction boundaries at use cases requiring atomic outcomes, especially acceptance plus membership. Resolve conflicting acceptance, withdrawal, role closure and capacity writes with a deliberate locking/version strategy and database constraints. Document conflict and retry semantics. Idempotency is required where retries could duplicate state or side effects; choose keys/deduplication scope per operation instead of adding a universal mechanism without need.

Create notification/history facts with committed state. A simple transactional in-database approach is a candidate; an outbox or background worker is not yet selected. External delivery must not report success for rolled-back operations. Any retryable delivery needs deduplication and visible failure handling.

Before writing V1, determine whether the empty file has been applied anywhere. Never edit an applied migration; use a new migration if needed. Test migrations against PostgreSQL from empty and prior supported states. Plan compatible rollout, backfills, backups and recovery before destructive changes. PostgreSQL server version, supported upgrade path and migration permissions remain decisions.

## Security and API contracts

Spring Security is present; authentication method, account recovery and session/token strategy are unresolved. Choose them before protected endpoints, alongside frontend origin, CORS and CSRF handling. Enforce object-level and action-level authorization on every private read/write using trusted actor identity. Management and moderation privileges must be explicit; membership alone is insufficient.

Validate inputs and safe user-content rendering; avoid fetching arbitrary evidence URLs server-side without a justified, protected design. Keep secrets in environment/service configuration, private fields out of public DTOs, and sensitive payloads out of logs. Select proportionate abuse controls for exposed actions. No money movement or investment handling belongs in MVP.

API envelope, pagination format, validation error representation, HTTP status mapping and request correlation are decisions to resolve, not implemented conventions. Decide whether to use a consistent envelope or ordinary resource responses with a standard error format. Define authentication, permission, missing-resource, validation and state-conflict behavior, including privacy-safe resource disclosure. If introducing `requestId`, specify ingress trust, generation, propagation, response and logging behavior; do not document it as present until verified.

## Observability, tests and deployment

Use structured operational logs, safe correlation and minimal metrics for request failures, database/migration health, application outcomes and notification failures. Define health/readiness and Actuator exposure; dependency presence alone does not prove production readiness. Avoid logging credentials, application text, reports or unnecessary personal data. Product measurement definitions live in [MVP.md](MVP.md).

Use JUnit for domain transitions and deterministic compatibility rules. Add PostgreSQL integration tests, with Testcontainers as appropriate, for migrations, constraints, transaction rollback and concurrent decisions. Add authorization coverage for other-user access and privileged actions. The existing context test depends on an external datasource configuration and is not isolated; do not infer business coverage from it. Add contract/frontend journey checks when those layers exist.

Deployment target is unresolved. Start with the smallest suitable application/database deployment; do not provision services during foundation documentation. Before alpha, define environment separation, secrets injection, TLS, migration execution, restricted operational endpoints, backups and tested restoration, rollback/recovery, and a moderation operator. External changes and deployment require user authorization. [README.md](../README.md) contains only repository-supported local commands, with execution still unverified.
