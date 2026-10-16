# MVP scope and acceptance

This document owns required product behavior and alpha outcomes. All capabilities below are planned. [PLANS.md](../PLANS.md) owns implementation sequencing and observed completion; [DOMAIN.md](DOMAIN.md) owns invariants.

## Scope

| Capability | Minimum acceptable behavior |
| --- | --- |
| Accounts and builder profiles | A user can access their account and edit their own skills, interests, availability, experience and portfolio links. Public/private fields are explicit; another user cannot edit them. |
| Public projects | A creator can publish a project showing stage, bounded scope, current team, expectations and supporting evidence. Incomplete or misleading required disclosures block publication; minimum required fields must be decided before implementation. |
| Open roles | A project manager can publish responsibilities, required/preferred skills, commitment and explicit compensation disclosures. Missing compensation is never presented as paid work. Closed roles cannot receive new applications. |
| Applications and memberships | A builder can apply to an open role and withdraw a pending application. Authorized project managers can accept or decline. Acceptance creates membership atomically; duplicate or racing requests cannot create duplicate active membership or exceed any agreed capacity. Departures preserve accurate history. |
| Discovery | Builders can filter public eligible projects/roles by agreed skills, interests, availability, commitment and preferences. Compatibility is deterministic and explainable; distinguish missing information from a mismatch. Stable pagination and ordering avoid accidental duplicates. |
| Milestones and evidence | Authorized team members can record bounded deliverables and attributable contributions with evidence links. Completion and acknowledgment follow defined permissions. Evidence displays its provenance and review state. |
| History | Material project, membership, milestone and contribution changes remain attributable and chronologically understandable. Public history excludes private applications, reports and sensitive account data. |
| Notifications | Users receive private, deduplicated notices for application decisions, membership changes and relevant milestone/contribution events. Minimum channel and delivery policy remain to be chosen. |
| Trust and safety | Users can report relevant content or conduct; authorized moderators can review reports and take accountable action. Server authorization, input validation and abuse controls cover every exposed flow. |

## Exclusions

No crowdfunding, payments, investment products, equity/revenue-share execution, funds custody, escrow, built-in chat, full project management, complex AI matching, native mobile applications, mentorship marketplace, paid visibility or leaderboards. No Redis, Kafka, Kubernetes, microservices, distributed architecture or dedicated search infrastructure without a demonstrated requirement. Funding exploration belongs after MVP in [BUSINESS.md](BUSINESS.md).

## Principal journeys and workflow gates

1. Creator: establish profile -> draft credible project -> publish project and openings -> review applicants -> accept collaborator -> record a milestone and acknowledged contribution.
2. Contributor: establish profile -> filter and inspect project/role -> apply -> see decision or withdraw -> participate as member -> submit attributable work evidence -> leave with accurate history when appropriate.
3. Moderator: receive report -> inspect only authorized context -> record reason and action -> apply restriction/removal -> retain an audit trail. Escalation and appeal policy must be settled before alpha.

Each workflow must handle invalid input, missing resources, unauthorized requests and state conflicts clearly. Retrying acceptance must not create another membership or notification. A withdrawn application cannot be accepted; simultaneous acceptance/withdrawal must yield one consistent outcome. Application withdrawal after acceptance is not membership departure. Closing a role stops new applications; handling existing pending applications must be decided explicitly before that feature ships.

An applicant sees their own applications and decisions; authorized project managers see applications for their projects. Public visitors see published project data, permitted team information and public evidence only. Project management authority must not automatically be granted to every member.

## Trust baseline and reputation

Validate text and external links and render user content safely. Apply reasonable abuse limits to registration, applications and reports; choose the mechanism from demonstrated requirements. Provide report handling, moderation reasons and accountable privileged actions. Define account/project removal, evidence disputes, privacy and retention rules before private alpha. Restriction must not silently erase historical attribution or disclose private information.

Display account age, membership duration, self-reported hours, project-acknowledged contributions and independent verification separately. Hours are claims, not productivity scores. Browsing time is not work. Owner-approved evidence must never display as independently verified. Independent verification requires a defined independent reviewer and method; do not offer that label until those exist.

## Private-alpha outcomes and measurement

Alpha should demonstrate that creators recruit suitable collaborators, collaborators remain engaged, and teams produce inspectable deliverables. The following are metric definitions, not observed results or validated targets:

| Measure | Operational definition |
| --- | --- |
| Accepted collaborators | Unique contributor-project pairs first entering active membership through accepted applications in the period; exclude owners, test accounts and repeated acceptances. |
| Active collaborations | Contributor-project memberships with at least one substantive contribution or milestone action in the trailing 30 days. Page views, notification reads and claimed hours do not qualify. |
| 30-day collaboration survival | For accepted pairs with a full 30-day observation window, fraction still active members and showing qualifying collaboration activity during days 1-30. Report early departures and missing activity separately. |
| Meaningful deliverables | Distinct milestone deliverables marked complete with attributable evidence and project acknowledgment in the period. Repeated edits do not add deliverables; acknowledgment does not imply independent verification. |
| Recruitment effectiveness | Eligible applications resulting in acceptance, plus time from role publication to first accepted collaborator; show unfilled roles rather than dropping them. |
| Safety and reliability | Report volume, time to review, unresolved serious reports, authorization failures and failed state transitions, with enough context to distinguish abuse from defects. |

Finalize event definitions, measurement windows, cohort size and numerical go/no-go targets before alpha invitations. No analytics is implemented. Use minimal privacy-conscious instrumentation; DAU and time spent are secondary diagnostics. Alpha exit requires successful end-to-end journeys, permission/concurrency checks, tested migration and recovery procedures, an operating moderation process, and evidence reviewed against the agreed cohort targets. Do not claim success from sign-ups alone.
