# Business direction and funding constraints

This document owns commercial hypotheses and future funding boundaries. [MVP.md](MVP.md) owns initial scope; [DECISIONS.md](DECISIONS.md) distinguishes accepted constraints from proposals. There is no observed traction, validated pricing, revenue forecast or market sizing in this repository.

## Initial model

The initial network is free so creators and contributors can establish the collaboration loop. A possible later creator subscription could offer useful administrative or presentation capabilities, but its feature bundle, price, demand and willingness to pay are unvalidated hypotheses. Do not gate essential trust information or imply that payment verifies a creator. Paid visibility and leaderboards are excluded from MVP.

Future reward-based crowdfunding could generate transaction-fee revenue if projects demonstrate credible delivery and supporter demand. Fee percentage, unit economics, processor costs, refunds, tax handling and margins are unknown; no price or projection is accepted. Optional donations are a separate possibility with disclosure, receipt and refund questions still open. Neither subscriptions nor funding is authorized for implementation by these documents.

## Whole-project and concurrent milestone funding

Future design may allow whole-project campaigns and multiple milestones to be fundable at the same time. A milestone can depend on another without implying that its funding guarantees delivery. Dependencies, prerequisites, delivery order, blocked status and consequences of an unfunded prerequisite must be visible before support is requested.

Before enabling funding, define an expense/allocation model showing which costs a whole-project campaign covers and which costs each milestone covers. The same expense must not be silently counted in two funding goals or allocations. If a shared expense is deliberately split, identify the portions and reconcile them. Show how whole-project proceeds affect milestone needs and vice versa; totals must not imply that overlapping commitments are independent unmet costs.

Resolve how concurrent pledges/reservations, funding limits, failed prerequisites, scope changes, overfunding, cancellations, refunds and reallocations affect remaining needs. Preserve an auditable allocation history and obtain any required supporter agreement before changing purposes. Public presentation must explain overlap and dependency risks, not merely sum campaign totals. These are future design constraints; no ledger, campaign tables, payment endpoints or custody infrastructure should be built in MVP.

## Rewards, donations and financial returns

Reward support describes a non-financial reward or project benefit under explicit terms; it must not promise equity, profit participation or investment returns. Donations may be considered separately and must not imply charitable/tax status without an applicable basis.

Financial-return investments, equity and revenue sharing are separate potential regulated offerings. They are not ordinary reward payments and cannot be enabled by renaming a reward campaign. Jurisdiction-specific specialist review and a separate business/architecture decision would be needed before pursuing them. No regulatory eligibility, exemption or compliance guarantee is asserted here.

Roles still require clear compensation disclosures in MVP, including when work is unpaid or terms are uncertain. A disclosed future intention does not mean Buildat executes, validates or guarantees an equity/revenue-share arrangement. Buildat provides no funds custody, escrow, investment execution or money movement in MVP.

## Commercial assumptions and risks

- Hypothesis: credible projects with defined openings attract more suitable collaborators than informal recruitment posts. Validate through application quality, accepted collaborators and retention.
- Hypothesis: attributable delivery history improves future recruitment. Validate through repeated collaboration and evidence use, not follower counts.
- Hypothesis: creators will pay for additional utility after the free loop works. Validate with research and explicit willingness-to-pay experiments before pricing or billing work.
- Risks: insufficient quality projects or builders on either side, abandoned projects, unpaid-work disputes, falsified evidence, unclear ownership/IP expectations, moderation costs and incentives to game acknowledgment.
- Competitive risk: existing communities, portfolio sites and coordination tools may already meet users' needs; switching costs and network effects may dominate feature quality. No fabricated competitor statistics or differentiation claims should substitute for user evidence.

## Prerequisites for handling money

Before any money-handling implementation, establish demand and delivery reliability; select operating jurisdictions, legal entity and supported campaign types; obtain relevant legal/tax review; choose an appropriate payment platform and define who holds funds. Resolve identity checks, prohibited activity, supporter terms, rewards, IP expectations, fees, disputes, refunds, chargebacks, taxes, fraud and support operations.

Also define allocation accounting, reconciliation, access controls, auditability, concurrent funding invariants, provider failure handling and recovery. Evaluate provider capabilities and costs from current evidence at that time. These prerequisites neither select a provider nor promise compliance. Funding exploration follows private-alpha learning in [PLANS.md](../PLANS.md).
