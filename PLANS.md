# Buildat implementation roadmap

Authoritative living roadmap, inspected and updated 2026-10-18. Read [AGENTS.md](AGENTS.md) before changes. [MVP.md](docs/MVP.md) owns behavior, [DOMAIN.md](docs/DOMAIN.md) owns invariants, [ARCHITECTURE.md](docs/ARCHITECTURE.md) owns engineering constraints and [DECISIONS.md](docs/DECISIONS.md) owns decision status. Update this file with evidence whenever a gate changes; file presence alone is not completion.

## Current Phase 2 state — 2026-10-18

User authorized repository implementation of the Phase 2 authentication/internal-account plan. Implementation began with clean Git status. No hosted database/Auth request, provider setting change, deployment, commit or push is authorized or performed.

- Added Boot-managed OAuth2 Resource Server with explicit trusted JWKS/asymmetric algorithms, issuer/audience/time/identity validation, permanent-user policy, no metadata authorities and compatible security Problem Details.
- Added minimal account ownership under `com.buildat.account`, unique issuer/subject mapping, standing checks, one-time provider confirmation verification for initial creation and transaction-safe concurrent bootstrap. Existing mappings never resurrect or change timestamps on retries. GET never provisions.
- Added POST bootstrap and GET/HEAD me, scoped requestId/data success advice and springdoc 3.1.1 with runtime/generated-schema coverage. Default/remote/production docs stay disabled; local docs bind to loopback.
- V1 remains unchanged and empty. V2 is an offline-prepared candidate applied only to disposable PostgreSQL 17.11. Hosted history/version/roles/exposure/TLS and exact hosted SQL authorization remain prerequisites. No hosted activation is claimed.
- Default Flyway remains disabled. The explicit local-development profile wires separate migration credentials and verified history schema; pending SQL runs automatically only after `BUILDAT_LOCAL_MIGRATIONS_APPROVED=true`. Local is the single shared-development authority, Northflank never independently migrates, and production's future dedicated release step is documentation only.
- PostgreSQL row locking requires UPDATE on at least one column. The runtime group therefore receives only UPDATE(updated_at) in addition to SELECT/INSERT and RLS policies. Real HTTP tests use the restricted runtime login; identity/standing updates, DELETE and DDL are denied. See D26.
- Initial compilation/test-fixture and contract failures were corrected during development. Final validation evidence is recorded below; test-only role creation and failing migration fixtures never enter production locations.

### Phase 2 validation evidence

| Exact command/check | Final outcome |
| --- | --- |
| `sh mvnw --batch-mode dependency:tree` | PASS; Security 7.1.1, springdoc 3.1.1, Boot-managed Jackson 2.21.5 and 3.1.5; no AI/MCP modules added |
| `sh mvnw --batch-mode -Dtest=JwtSecurityTest,AccountServiceTest,ApiContractTest test` | PASS: 12 tests, zero failures/errors/skips |
| `sh mvnw --batch-mode -Dit.test=AccountPersistenceIT,AuthenticationApiIT,BuildatServerApplicationIT verify` | PASS: 25 unit/MVC + 13 integration tests, zero failures/errors/skips |
| `sh mvnw --batch-mode verify` | PASS: 25 unit/MVC + 14 integration tests, zero failures/errors/skips; includes generated OpenAPI/Swagger checks |
| `git diff --exit-code HEAD -- src/main/resources/db/migration/V1__init.sql` | PASS; zero-byte V1 identical to HEAD |
| `git diff --check` and `git diff --cached --check` | PASS; no staged changes |
| Python local Markdown file-target check | PASS across README, PLANS, AGENTS and docs |
| Final status/diff review | Intended Phase 2 source/configuration/migration/test/docs only; no unrelated pre-existing work |

The first sandboxed Maven dependency attempt could not write the normal cache; approved retry succeeded. Docker access for disposable tests succeeded with required sandbox access. Early test failures (fixture imports, challenge/HEAD expectations and outage-test ordering) were corrected. Real DB outage exposed a Hibernate rollback wrapper with a pool-generated SQLException lacking SQLSTATE; the handler now distinguishes JDBC transaction/connection failure from unrelated persistence errors and returns 503. Deliberate migration failure, uniqueness violation and database shutdown tests produce expected framework diagnostics. No live Supabase verification was run.

The isolated Phase 2 repository gate is complete. Hosted activation remains incomplete and separately authorized; the next task below is intentionally user-operated.

## Historical Phase 1 completion evidence — 2026-10-18

- User authorized the revised plan's **first backend-foundation milestone**. Auth, account/domain features, frontend, success wrapping/springdoc and deployment remain later work. Accepted direction and superseded proposals are recorded in [DECISIONS.md](docs/DECISIONS.md), D14–D21.
- Implementation started with a clean Git working tree. The attached plan's historical staged Dockerfile was absent; no Dockerfile was created or changed. No unrelated work was overwritten and no commit/push was made.
- Java 25.0.4.1 and wrapper-selected Maven 3.9.16 run successfully. Docker was initially stopped; after the user started it, Docker Engine 27.3.1 (`linux aarch64`) and Testcontainers worked. No global settings were changed.
- User confirmed hosted PostgreSQL **17.11**. Tests pin `postgres:17.11`, use Boot service connections and deliberately unusable fallback datasource values, and never connect to Supabase. The container actually reported PostgreSQL 17.11.
- Explicit Security permits only GET/HEAD of exact liveness/readiness probe paths and denies all other ordinary routes; form/Basic/session login is disabled. Actuator exposes status only. Readiness includes DB, liveness excludes it.
- Added server-controlled request IDs before Security, error-dispatch correlation/MDC cleanup and generic RFC 9457 errors for MVC/security/firewall/container failures. No application success API exists yet; response advice and springdoc remain deferred.
- Normal startup now disables Flyway; hosted changes remain separately user-operated. Hibernate `validate` and disabled open-in-view remain. V1 is byte-for-byte unchanged and empty. No production entities or domain schema are implemented.
- Replaced the external-datasource context test with Failsafe container/real-HTTP tests and added Docker-free JUnit/MVC tests. `sh mvnw --batch-mode test` passed **7 tests**; `sh mvnw --batch-mode verify` passed **7 unit/MVC + 4 integration tests**, **0 skips**. Migration application/revalidation, status-only probes, process readiness refusal, DB outage, route denial, HTTP errors, HEAD and error-dispatch request IDs are covered. Empty entity validation is not domain-schema validation evidence.
- Initial validation failures (ProblemDetail argument type, MVC fixture API and database-outage ordering) were corrected before the passing runs. Git whitespace checks, unchanged-V1 comparison, documentation local links and final status review are recorded in [FOUNDATION.md](docs/FOUNDATION.md).

## Historical documentation baseline — 2026-10-16

- One Maven backend with application entry point under `com.buildat`; no controllers, entities, repositories or domain services were found.
- POM declares Java 25, Spring Boot 4.1.1 and web MVC, validation, JPA, Flyway/PostgreSQL, Security and Actuator dependencies. Wrapper selects Maven 3.9.16. Intended backend versions match declarations; resolved dependency/runtime versions are unverified.
- Datasource uses three environment variables; Hibernate validates schemas, open-in-view is disabled and Flyway is enabled. `V1__init.sql` is empty. No domain schema exists in repository migration files; external database state is unknown.
- One JUnit context-load test exists, with no isolated database/test profile. No Testcontainers, business tests or proof of a passing current build.
- No frontend, CI workflow, provisioning script or deployment configuration found. Ignored `target/classes` output does not establish current correctness.
- Before this task: staged `.idea/misc.xml` modification and empty V1 addition; unstaged application-class imports. During validation, changes outside this documentation task removed the imports and unstaged the IDE/migration changes. Final observation: `.idea/misc.xml` is modified, V1 is untracked and the application class matches HEAD. Do not overwrite this work. The prior README was a title only; no existing AGENTS.md, PLANS.md or domain/planning documents were found. Generated ignored HELP.md was read and preserved.
- Documentation foundation: the nine requested documents are authored in this working tree. They do not constitute a committed release or implemented product. Validation is recorded in the documentation task report; no executable code, dependency or configuration changes are part of this task.

## Delivery order and acceptance gates

Phases 0–2 have repository implementation evidence; later feature gates remain planned. Hosted activation is a separate incomplete gate. Ordering is a dependency guide, not a calendar estimate. Trust, authorization and integrity apply from the first slice; phase 8 consolidates safety operations rather than postponing protection.

| Phase | Prioritized work and dependencies | Acceptance gate |
| --- | --- | --- |
| 0. Documentation foundation | Establish all requested docs; distinguish evidence, direction and hypotheses; preserve user work. | Nine docs exist, local links resolve, consistency review and diff checks pass, changed files reported; no implementation changes. |
| 1. Backend foundation | **First milestone implemented and isolated gate passed.** Explicit denial, safe probes, correlation/errors, Testcontainers/Failsafe, manual hosted migrations and updated decisions. No Auth/domain tables. Depends on 0. | Java/wrapper and Docker work; 7 unit/MVC and 4 container/HTTP tests pass via `verify`. V1 applies once/revalidates unchanged. No domain schema or hosted setup is claimed. |
| 2. Authentication and internal accounts | Supabase local-JWT validation, provider-verified idempotent account bootstrap, authoritative standing, account HTTP contract and local OpenAPI. Backend only. | Isolated JWT/API/persistence/concurrency/runtime-role and migration checks pass; hosted activation remains separately user-operated. See current evidence above. |
| 2b. Builder profiles | Subsequent milestone: profile ownership, visibility, skills/interests, availability, experience and portfolio presentation. Resolve taxonomy/privacy before design. Depends on 2. | Own-profile edit and permitted public read work; private data and other-user writes are blocked; validation and persistence tests pass. |
| 3. Projects and roles | Establish accountable owner, minimum publication requirements, team view, scope/stage/evidence and openings with explicit disclosures. Depends on 2 and agreed project authority. | Publish/edit/close flows enforce management rights and complete disclosures; public views expose only permitted content; closed openings reject new applications. |
| 4. Applications and membership | Decide pending uniqueness, reapplication, role closure effects, capacity and multiple-role membership; implement apply, withdraw, decline, accept, departure/removal and history. Depends on 3. | Atomic acceptance/membership; racing requests cannot duplicate active participation, accept withdrawn applications or exceed agreed capacity; private application and authority checks pass. |
| 5. Deterministic discovery | Agree filters, missing-value behavior and compatibility explanation; implement PostgreSQL queries, stable ordering/pagination and relevant indexes. Depends on profiles and published projects/roles; use phase 4 outcomes when needed. | Repeated inputs yield explainable results; hidden/closed content is excluded according to policy; filter/pagination tests and representative query checks pass without dedicated search infrastructure. |
| 6. Milestones and evidence | Define completion, acknowledgment, evidence revision/dispute and visibility policies; implement milestones, attributable contributions and material project history. Depends on 4. | Same-project linkage and author/reviewer permissions enforced; departures preserve accurate attribution; owner acknowledgment never appears as independent verification. |
| 7. Essential notifications | Select minimum channel and event/recipient rules; implement private notices, committed-event consistency, deduplication and any needed retries. Depends on 4 and 6; design transactional consistency with those phases. | Required application/membership/delivery events notify only authorized recipients; retries and rollbacks do not produce duplicate or false notices; failure handling is inspectable. |
| 8. Trust and safety readiness | Implement reporting, restricted review, accountable moderation and proportionate abuse controls. Resolve removal/retention, disputes and escalation/appeal policy. Depends on protected foundation and relevant content flows; add controls alongside earlier phases. | Cross-user and privileged-access tests pass; reports remain private; restrictions take effect predictably; moderation operator and escalation process exist. |
| 9. Frontend integration | Choose frontend repository/location and authentication integration; build Next.js/TypeScript creator/contributor/moderator journeys against accepted contracts. Depends on 1 and relevant backend phases; incremental integration may begin earlier. | End-to-end journeys cover normal, invalid, unauthorized and conflicting states; accessible forms and public/private views match policy; no invented backend behavior. |
| 10. Private alpha | Agree cohort/measurement targets, instrument minimal events, validate recovery and invite a bounded group. Depends on 2-9 and operating safety/support. | Clean deployment and migration/recovery rehearsals, end-to-end checks and moderation readiness pass; evaluate accepted collaborators, active collaborations, 30-day survival and meaningful deliverables against pre-agreed targets. |
| 11. Post-MVP funding exploration | Research reward support for whole projects and concurrent milestones, explicit allocation/dependencies and optional donations. Evaluate creator subscription demand separately. Depends on alpha learning and demonstrated demand. | Reviewed feasibility and commercial decision address double funding, legal/provider/operational prerequisites; no money handling authorized merely by completing research. Financial-return offerings remain separate. |

## Outstanding decisions and risks

Hosted Flyway history, actual database roles/grants, Data API isolation and verified TLS remain user-operated setup gates before hosted migrations/startup. This work connected to no hosted database and used no provider credentials. Testcontainers covers vanilla PostgreSQL, not Supabase-specific services/policies. Hibernate now validates the account entity on isolated PostgreSQL; this does not verify the hosted schema.

Phase 2 repository implementation does not establish hosted Auth settings. Before hosted activation, the user must verify exact issuer/JWKS/audience, current asymmetric signing mode and token shape, confirmation settings and a controlled user. Token lifetime/sign-out choices require operator review; provider deletion/revocation retains documented expiry/cache windows. The one-time lookup confirms provider state, not historical mailbox verification for auto-confirmed identities.

Future gates still need feature action matrices, skills/publication policy, application uniqueness/capacity/reapplication, evidence review, notification delivery, retention/moderation and alpha targets. AWS compute/TLS and frontend repository location remain deferred. Do not turn proposals into schema defaults. No infrastructure is needed to compensate for unvalidated demand.

## Next task

**User-only hosted readiness and artifact review:** inspect designated development project/history/V1, roles/grants/exposure/TLS and signing/confirmation settings using the ordered [Auth runbook](docs/AUTHENTICATION.md). Review exact V2 before separately authorizing hosted execution and local migration activation. Stop on conflicts. No profile, frontend, deployment or provider-change work is implied.
