# Domain model

This document owns conceptual relationships and invariants, not a database or endpoint specification. All domain implementation is planned. Rules here are accepted direction unless marked proposed or unresolved; [DECISIONS.md](DECISIONS.md) tracks remaining choices.

## Concepts and relationships

| Concept | Meaning and relationships |
| --- | --- |
| User/account | Authenticated identity and account standing. Owns a builder profile and can create projects, apply, join teams and report issues. Account identity is separate from public profile presentation. |
| Builder profile | User-controlled skills, interests, availability, experience and portfolio links. Self-description is not proof of competence. Public visibility and private contact fields require explicit policy. |
| Skill | Shared vocabulary used by profiles and roles; required and preferred role skills have different meanings. Taxonomy, aliases, proficiency and free-text extensions are unresolved. |
| Project | Owned collaboration context containing scope, stage, expectations, evidence, roles, team, milestones and history. Publication does not prove feasibility or endorsement. |
| Role | A project's recruitment opening with responsibilities, skills, commitment and compensation disclosures. Role availability is distinct from project stage and membership status. |
| Application | A user's request to join a specific project role, with private application context and an attributable decision history. An application is not membership. |
| Membership | A user's participation in a project, including authority, joining and departure history. May originate from an accepted application; creator participation must be represented explicitly without counting it as recruitment. |
| Milestone | Bounded project deliverable with expectations, completion evidence and history. It is not a payment entitlement or a full task-management system. |
| Contribution | Attributable work in a project, optionally associated with a milestone. Author claims, project acknowledgment and independent verification are separate facts. |
| Evidence | A reference supporting a project, milestone or contribution, with attribution, provenance and review context. A portfolio URL alone is not verification; external content may change or disappear. |
| Notification | Recipient-private notice of a domain event. It reflects committed state and does not grant authority or become the source of truth for that state. |
| Report | A user's private allegation about content or conduct, linked to relevant subjects, review actions and outcomes. Submission is not proof of wrongdoing. |
| Project history | Attributable record of material changes, participation and delivery. Public history and restricted audit records have different audiences. |

## Ownership and permissions

- A user edits their own profile. Account administration requires explicit privileged authority; profile ownership does not confer project authority.
- Every project has an accountable owner. Ownership transfer, co-ownership and delegated management are unresolved; do not infer them from membership or a role title. A project cannot be left without an accountable owner through ordinary departure.
- The owner and explicitly authorized managers control project publication, roles, application decisions and team administration. Members receive only the rights their project authority grants. The exact permission matrix is a backend-foundation prerequisite.
- Applicants control submission and pending withdrawal. Only the applicant and authorized project managers may read private application content, except narrowly authorized moderation access.
- Members may submit attributable contributions under an agreed project policy. Project acknowledgment requires an authorized reviewer. Authors cannot impersonate another contributor or unilaterally grant their own work independent verification.
- Moderators can inspect reports and act only within defined moderation authority. Project owners do not automatically see reporter identities or private reports about their projects.
- Public visibility is explicit for projects, team presentation and evidence. Public APIs must not expose private account fields, applications, notification recipients or restricted report/audit data.

## Consistency and state transitions

Validate application eligibility against current role/project state inside the write operation. Proposed baseline: one pending application per user per role and one active membership per user per project, with historical episodes retained. Multiple-role membership, reapplication and capacity semantics must be accepted before schema design; never create duplicate active memberships accidentally.

Acceptance must atomically record an application decision and establish the associated membership, subject to any agreed capacity limit. Two acceptances, or acceptance racing withdrawal/closure, must not produce contradictory outcomes. Define the winner through transaction/concurrency rules and return a clear conflict when appropriate. Withdrawal applies to pending applications; leaving an accepted team is a separate membership transition. Decline, expiry, role closure and reapplication policy remain unresolved.

Membership removal/departure ends current permissions while preserving attributable contributions. Historic membership must not imply ongoing authority. Project ownership requires an explicit transfer or closure policy before an owner can leave. Account deletion/retention and redaction rules must reconcile privacy with history; no immutable-public-history guarantee is made.

Milestones, contributions and their evidence must belong to the same project when linked. Completion cannot automatically validate every linked claim. Changing or removing evidence after acknowledgment requires visible review/history semantics; decide the exact revision and dispute behavior before implementation. Cross-project references must not accidentally grant write access.

Record actor, time and relevant transition for material history and moderation actions. Only committed changes generate notifications; retrying a change must not duplicate memberships, history entries or notices. Use constraints and transactions as well as application validation; implementation choices belong in [ARCHITECTURE.md](ARCHITECTURE.md).

## Reputation semantics

| Signal | What it establishes | What it does not establish |
| --- | --- | --- |
| Account age | Time since account creation | Active participation, skill or delivery |
| Membership duration | Time associated with a project | Continuous productive work |
| Self-reported hours | A person's unverified time claim | Verified effort, output or quality |
| Acknowledged contribution | Authorized project reviewer recognized a contribution | Independent verification |
| Independently verified evidence | A specified independent method/reviewer checked a specified claim | Universal endorsement or a guarantee of future performance |

No browsing-time productivity signal or composite leaderboard score is part of MVP. Independent verification procedure, disputes and conflict-of-interest rules are unresolved; absence of a procedure means the label must not be used.

Future funding introduces separate allocation and financial concepts; do not embed payment, investment or escrow semantics in MVP roles or milestones. See [BUSINESS.md](BUSINESS.md).
