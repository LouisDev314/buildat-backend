# Decision register

Initial register dated 2026-10-16. **Accepted** means agreed direction, not implemented behavior. **Proposed** means a candidate requiring a decision before dependent work. **Deferred** means intentionally outside the current scope or postponed to its prerequisite gate. Preserve rationale when superseding a decision; add a dated entry rather than silently deleting history. [PLANS.md](../PLANS.md) owns scheduling and completion evidence.

## Accepted direction

| ID | Date | Status | Decision, rationale and implications |
| --- | --- | --- | --- |
| D01 | 2026-10-16 | accepted | Project-first collaboration network, initially indie games. Discovery and recruitment revolve around credible projects and bounded roles; evaluate sustained collaboration and delivery. See [PRODUCT.md](PRODUCT.md). |
| D02 | 2026-10-16 | accepted | MVP includes accounts/profiles, public projects/roles, applications/memberships, deterministic discovery, basic milestones/evidence/history, essential notifications and baseline safety. Scope and acceptance belong to [MVP.md](MVP.md); these are planned capabilities. |
| D03 | 2026-10-16 | accepted | No MVP money movement, crowdfunding, investment execution, custody or escrow; also exclude built-in chat, full project management, complex AI matching, native mobile, mentorship marketplace, paid visibility and leaderboards. Protect scope and avoid premature operational obligations. |
| D04 | 2026-10-16 | accepted | Java 25, Boot 4.1.1, Maven, PostgreSQL, Flyway, JPA/Hibernate and Spring Security; Next.js/TypeScript frontend direction. POM matches backend target; frontend and Testcontainers are absent. Verify differences and propose separate changes; never silently alter dependencies. |
| D05 | 2026-10-16 | accepted | Modular monolith, one backend/database initially. No Redis, Kafka, Kubernetes, microservices, distributed architecture or dedicated search infrastructure without a demonstrated requirement and decision. Boundary intentions are in [ARCHITECTURE.md](ARCHITECTURE.md). |
| D06 | 2026-10-16 | accepted | Flyway owns schemas, Hibernate validates and open-in-view remains disabled, matching current configuration. Applied migrations must not be rewritten; empty V1 application history is unknown. |
| D07 | 2026-10-16 | accepted | Server-enforced ownership and authorization, explicit state transitions, transactional membership acceptance and concurrency/idempotency safeguards. Dependency presence does not establish these guarantees; tests must prove them. |
| D08 | 2026-10-16 | accepted | Separate account age, membership duration, self-reported hours, acknowledged work and independent verification. Browsing and unverified hours are not productive output; owner approval is not independent verification. See [DOMAIN.md](DOMAIN.md). |
| D09 | 2026-10-16 | accepted | AI assists scoping, role descriptions and matching; deterministic filtering and the collaboration loop must work without it. No AI dependency or complex matching is required for MVP. |
| D10 | 2026-10-16 | accepted | Initial network is free. Creator subscription and crowdfunding fees remain business hypotheses; no billing implementation follows from this decision. See [BUSINESS.md](BUSINESS.md). |
| D11 | 2026-10-16 | accepted | If future reward crowdfunding is pursued, allow exploration of whole-project and concurrently fundable milestones only with explicit allocation and visible dependencies preventing silent expense double funding. This is a future constraint, not MVP authorization. |
| D12 | 2026-10-16 | accepted | Financial-return investments, equity and revenue sharing are separate potential regulated offerings, not reward payments. No legal eligibility or compliance guarantee is assumed. |
| D13 | 2026-10-16 | accepted | Alpha success centers on accepted collaborators, active collaborations, 30-day survival and evidenced deliverables. Metric definitions are in [MVP.md](MVP.md); targets and observed outcomes are not yet established. |

## Proposals and unresolved questions

| ID | Date | Status | Question/proposal, rationale and required resolution |
| --- | --- | --- | --- |
| P01 | 2026-10-16 | proposed | Choose authentication, account recovery and session/token handling with frontend CORS/CSRF and privacy implications. No custom security flow exists. Resolve in backend foundation before protected endpoints. |
| P02 | 2026-10-16 | proposed | Define owner/manager/member/moderator authority and ownership transfer/owner departure. Avoid accidental management rights from membership; resolve permission matrix before projects and collaboration. |
| P03 | 2026-10-16 | proposed | Choose supported PostgreSQL version and reproducible local/test provisioning; inspect external Flyway history. Consider Testcontainers for isolated persistence tests; no dependency change yet. Resolve before schema implementation. |
| P04 | 2026-10-16 | proposed | Choose API envelope versus ordinary resource responses, standard error structure/statuses, pagination and request correlation. Neither a response envelope nor requestId mechanism exists. Resolve before API contracts. |
| P05 | 2026-10-16 | proposed | Baseline candidate: one pending application per user/role and one active membership per user/project. Resolve multi-role participation, reapplication, closure/expiry, capacity and duplicate request semantics before collaboration schema. |
| P06 | 2026-10-16 | proposed | Define skills taxonomy, availability/commitment units, preference filters and missing-data compatibility behavior. Resolve profile basics in phase 2 and deterministic rules before discovery; avoid opaque scores. |
| P07 | 2026-10-16 | proposed | Choose required publication/evidence/compensation disclosures and permitted content, including uncertain future compensation claims. Resolve before project publication; disclosures never authorize Buildat to execute financial arrangements. |
| P08 | 2026-10-16 | proposed | Define milestone completion, contribution acknowledgment, evidence revision/disputes and public visibility. Independent verification requires a separate method and reviewer policy before the label is available. |
| P09 | 2026-10-16 | proposed | Choose minimum notification channel, recipients, retry/deduplication policy and transaction integration. In-app notices are a candidate, not a selected or implemented channel. |
| P10 | 2026-10-16 | proposed | Define reporting/moderation process, restrictions, appeals/escalation, account removal, privacy and retention. Resolve before alpha, with relevant permission decisions made before each content flow. |
| P11 | 2026-10-16 | proposed | Choose frontend location and deployment target, environment/secrets/backup/recovery policy. No frontend or deployment setup is present; external changes require authorization. |
| P12 | 2026-10-16 | proposed | Agree alpha cohort, substantive activity event definitions and numerical targets before invitations. Product metrics are definitions, not traction claims. |

## Deferred business possibilities

| ID | Date | Status | Hypothesis or possibility and prerequisite |
| --- | --- | --- | --- |
| F01 | 2026-10-16 | deferred | Creator subscription bundle and pricing; validate recurring utility and willingness to pay after the free loop shows value. No price or projection is validated. |
| F02 | 2026-10-16 | deferred | Reward crowdfunding fees and optional donations; require demonstrated demand plus allocation, dependency, provider, legal/tax and operating decisions in [BUSINESS.md](BUSINESS.md). |
| F03 | 2026-10-16 | deferred | Financial-return investment, equity or revenue-share offerings; require a separate regulated-product assessment and authorization. Never treat as ordinary rewards. |
| F04 | 2026-10-16 | deferred | Independent verification service and advanced reputation/matching. Establish evidence standards and real collaboration data first; no automatic endorsement or leaderboard. |

## Existing-document conflicts

No conflicting user-authored product/planning decisions were found during inspection. The prior README title identified the backend; it is retained as the introduction's context. Generated HELP.md remains untouched. Reassess conflicts when additional documentation or external decisions are supplied.
