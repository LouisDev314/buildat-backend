# Authentication and internal account runbook

Phase 2 is backend-only. Supabase owns signup, passwords, confirmation, recovery, sessions and refresh tokens. Buildat validates access JWTs and owns account standing. This document describes repository behavior; it does not claim hosted activation. See [PLANS.md](../PLANS.md) for executed validation and remaining gates, [DECISIONS.md](DECISIONS.md) for accepted decisions and [FOUNDATION.md](FOUNDATION.md) for foundation operations.

## Configuration and local startup

[.env.example](../.env.example) lists public configuration and secret placeholders. Spring does not automatically load `.env`. Set process environment or unshared IDE configuration; never paste passwords, tokens or real configuration dumps into chat.

`SUPABASE_URL`, `SUPABASE_ISSUER`, `SUPABASE_JWKS_URI`, `SUPABASE_AUDIENCE`, `SUPABASE_SIGNING_ALGORITHMS` and `SUPABASE_PUBLISHABLE_KEY` are required. URLs must be HTTPS, with issuer `/auth/v1` and JWKS `/auth/v1/.well-known/jwks.json` under the configured project origin. ES256 and RS256 are supported; configure only the project's verified actual algorithm. An approved overlap is possible for rotation. HS256/legacy JWT secrets are unsupported. The publishable key is public configuration, not a service-role/admin secret.

Runtime uses `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`; keep existing verified TLS (`sslmode=verify-full` plus provider CA), direct JDBC or session pooling and bounded Hikari pooling. No frontend origins or CORS permissions are configured yet.

Default startup disables Flyway and documentation. It requires an already migrated schema and validates it with Hibernate. No Auth discovery call occurs at startup. Liveness is process-only; readiness checks the DB, not Auth/JWKS availability.

The `local-development` profile binds to 127.0.0.1, enables local Swagger/OpenAPI and wires separate `MIGRATION_DATABASE_URL`, `MIGRATION_DATABASE_USERNAME`, `MIGRATION_DATABASE_PASSWORD` and `FLYWAY_HISTORY_SCHEMA`. All three migration connection settings must be supplied; do not rely on Boot's fallback to runtime credentials. It enables automatic pending migrations only when `BUILDAT_LOCAL_MIGRATIONS_APPROVED=true` after the gates below. The flag records operator approval; it does not inspect a cloud project or replace review.

After all hosted prerequisites and exact SQL authorization, the user may start:

```sh
sh mvnw spring-boot:run -Dspring-boot.run.profiles=local-development
```

Do not run this command as isolated verification. No hosted startup was performed during implementation. The `production` profile explicitly disables Flyway and docs; do not deploy `local-development` or supply migration credentials to application replicas. Avoid externally overriding these safety settings.

## HTTP and identity contract

| Route | Behavior |
| --- | --- |
| POST `/api/v1/me/bootstrap` | No request body required; identity-like supplied fields cannot select an account. Creates once (`201`, Location `/api/v1/me`), otherwise returns active mapping (`200`). |
| GET `/api/v1/me` | Returns current active mapping (`200`); never creates. |
| HEAD `/api/v1/me` | Same access checks, no response body. |

JSON success is exactly `{"requestId":"server UUID","data":{"id":"internal account UUID"}}`, with `Cache-Control: no-store`. DTOs expose no provider subject, issuer, email, metadata or standing history. The generic success advice applies only to marked application controllers; Actuator, Problem Details, docs, empty/HEAD/204/304 responses, resources, strings and streams are excluded. Existing framework status/headers survive.

Errors use RFC 9457 `application/problem+json`, safe generic detail, `code`, query-free `instance` and server-generated `requestId` matching `X-Request-ID`.

| Status | Codes |
| --- | --- |
| 401 | `AUTHENTICATION_REQUIRED`, `INVALID_TOKEN` |
| 403 | `ANONYMOUS_IDENTITY_NOT_ALLOWED`, `ACCOUNT_BOOTSTRAP_REQUIRED`, `EMAIL_CONFIRMATION_REQUIRED`, `ACCOUNT_RESTRICTED`, existing `ACCESS_DENIED` |
| 409 | `ACCOUNT_STATE_CONFLICT` for transaction lock/deadlock/timeout conflicts; ordinary concurrent bootstrap succeeds |
| 503 | `DATABASE_UNAVAILABLE`, `IDENTITY_PROVIDER_UNAVAILABLE` |
| 500 | Existing generic `INTERNAL_ERROR` |

Security errors preserve safe Bearer challenge semantics; no exception/provider messages are returned. Unlisted routes and methods remain denied. The only unconditional public routes are exact GET/HEAD liveness/readiness probes. Local documentation GET/HEAD routes are allowlisted only with the local profile and documentation switch; production is denied even if local is also selected.

JWT validation uses Spring Security/Nimbus with a trusted configured JWKS, issuer, audience, expiry, issued-at consistency, optional not-before, canonical UUID subject/session ID, role `authenticated`, and Boolean `is_anonymous`. Timestamp tolerance is 60 seconds. A valid anonymous identity is authenticated but forbidden. Missing/malformed claims are authentication failures. No provider metadata, email or client-supplied owner ID establishes Buildat authority. No scopes/roles are converted to Buildat privileges. Only Authorization-header Bearer tokens are accepted.

JWKS is cached by the framework (currently five minutes), with rotation/unknown-key refresh support and bounded two-second connect/three-second read timeouts. Provider edge caches also delay key revocation. A cached trusted key may work during outage; fetching a required unavailable key returns 503 and grants no access. Invalid signatures/unknown keys after successful retrieval return 401. Provider clients do not follow redirects. Never trust token-supplied key URLs or log tokens. There is no per-request Auth user lookup.

## Provisioning and standing

A valid permanent identity is resolved by unique issuer/subject. Existing mapping standing is read from PostgreSQL on every account operation, without a cross-request cache. Restricted/deleted mappings return the same generic restriction code and are never resurrected. The account module owns persistence and returns immutable internal views; future modules must use its service and enforce object authorization in their own use cases.

For an absent mapping only, the backend calls Supabase `/auth/v1/user` with the incoming access token plus publishable key. It requires matching ID, non-anonymous identity, nonempty email and `email_confirmed_at`. Definitive 401/404 rejection becomes 401. Timeouts, throttling, bad/mismatched responses and server failures become 503 and create nothing. Unconfirmed email is 403. The check occurs outside the DB transaction. Concurrent first calls can issue multiple read-only lookups, but create one mapping.

Creation uses READ COMMITTED, `INSERT ... ON CONFLICT ON CONSTRAINT ... DO NOTHING`, followed by a locked read and standing check in a short transaction. No uniqueness exception is caught inside an aborted transaction. The creator gets 201, contenders/retries 200. Creation failures roll back. Existing bootstrap calls do not change timestamps or standing. Native insert is encapsulated in the account repository, using the same transaction manager as JPA.

Accounts contain only internal UUID, issuer/subject, ACTIVE/SUSPENDED/DELETED standing and timestamps. Email is neither stored nor an identity key. No FK/triggers touch `auth.users`. The domain supports ACTIVE→SUSPENDED and ACTIVE/SUSPENDED→DELETED, with idempotent repeated restriction and no reactivation. No externally callable restriction service/admin API or full deletion workflow is shipped. Future restriction use cases must lock rows and receive separately reviewed privileges. Ongoing operations authorized before a restriction are not automatically cancelled.

Provider deletion/sign-out can leave an access JWT valid until expiry (plus timestamp tolerance); an existing active Buildat mapping still works during that window. Initial verification reduces provisioning from deleted provider identities but cannot eliminate races. Immediate provider revocation synchronization is deferred. Never advertise instantaneous provider-account revocation.

`email_confirmed_at` is provider confirmation state. It cannot prove historical mailbox ownership for users created when confirmation was disabled or bypassed by an administrator. Do not store an invented email-verification claim or use user-editable metadata. The setup gate requires confirmation enabled and a deliberately verified controlled test user.

## Migration artifact and authority

[V2__create_accounts.sql](../src/main/resources/db/migration/V2__create_accounts.sql) is an offline-prepared, reviewable candidate. It has only run on disposable PostgreSQL tests. Hosted history has not been inspected; **do not apply it until the user confirms consistency and authorizes the exact hosted changes**. V1 stays zero bytes and unchanged. Stop if V2 conflicts with existing history or an existing `buildat` schema; do not hide the conflict using IF NOT EXISTS, repair, baseline, clean or history relocation.

The migration creates private `buildat.accounts`, constraints, explicit grants and RLS. Preserve the verified Flyway history schema separately; do not make `buildat` the Flyway default merely because it owns application objects. No schema-history relocation or Supabase CLI migration system is introduced.

Before migration, the user must establish a NOLOGIN, NOSUPERUSER, NOBYPASSRLS `buildat_runtime` group and a separate runtime login inheriting it. Provision the migration owner/login separately with required database/schema-creation and existing-history privileges. Do not use `postgres`, a table owner or BYPASSRLS role as runtime. No role creation/password management is performed by V2. The disposable test-only initialization script is not hosted setup SQL.

V2 grants runtime schema USAGE, account SELECT/INSERT and **column-only UPDATE(updated_at)**. PostgreSQL SELECT FOR UPDATE requires UPDATE privilege on at least one column, so this narrow grant and an UPDATE RLS policy are necessary for bootstrap's locked read. Runtime still cannot UPDATE standing, ID/identity or creation time, DELETE accounts or issue DDL. SELECT/INSERT/UPDATE policies apply only to the trusted runtime group; INSERT requires ACTIVE. RLS complements Spring authorization; these backend policies do not grant browser access or implement per-user RLS. Owner bypass is reserved for migrations/explicit operator work.

The schema is not exposed through Data API. PUBLIC/browser grants are revoked, including inherited default table grants for existing `anon` and `authenticated` roles. User verification must also inspect exposed schemas, existing role memberships and default privileges; accidental membership in `buildat_runtime` would defeat isolation. Runtime credentials are private server secrets. No general policy grants browser users access to internal accounts.

One designated local development operator/process is the migration authority for the shared development database. Local approved startup runs pending Flyway SQL automatically; Northflank runtime does not migrate and has no DDL credentials. Coordinate compatible schema changes before using a newer local artifact against an older deployed app. Flyway locking is execution safety, not permission for independently chosen releases.

Production will use one dedicated automatic release/migration job, isolated credentials and versioned artifacts, before compatible runtime rollout. Every replica keeps Flyway disabled. This AWS-independent release contract is documented only; no runner, CI, deployment or production resources are provisioned.

V2 is additive. Roll back application binaries while retaining the table; forward-fix schema problems in a new migration. PostgreSQL transactional DDL rolls back this migration on failure. Do not automatically drop data or reverse applied SQL. Inspect failed history/transaction state and review a recovery artifact before further hosted action. Later schemas/columns/backfills require additive compatible migrations and separate recovery planning.

## Ordered user-only setup

None of these instructions authorize the agent to access/change hosted services. Inspection/copying values does not invalidate sessions. Never share credentials or real tokens in chat.

1. **Before hosted activation:** Select the existing project; compare project reference with **Connect** and confirm disposable development, not production. No new project is assumed. Project reference/URL are public configuration. [Connection reference](https://supabase.com/docs/guides/database/connecting-to-postgres).
2. **Before final hosted SQL review:** Use **SQL Editor** for read-only PostgreSQL version, schemas, role memberships/grants and Flyway-history inspection. Find actual history location and versions/checksums, compare V1, confirm no conflicting V2 or `buildat` objects. Keep full output private. Tests retain 17.11 pending deliberate hosted compatibility review. [Flyway history](https://www.red-gate.com/hub/product-learning/flyway/exploring-the-flyway-schema-history-table/).
3. **Before manual Auth testing:** **Authentication → Sign In / Providers → Email**: verify email/password enabled. Allow signup only if testing self-signup; existing controlled users suffice. This need not invalidate sessions. [Password Auth](https://supabase.com/docs/guides/auth/passwords).
4. **Required policy:** In Email settings, enable/verify **Confirm Email**. Test that an unconfirmed signup cannot log in and an actually confirmed account can. This is not retroactive proof for old auto-confirmed accounts; changing settings does not replace existing-token/standing controls. [Auth configuration](https://supabase.com/docs/guides/auth/general-configuration).
5. **Before hosted testing:** **Authentication → Sign In / Providers → Anonymous Sign-Ins**: disable if unused. Backend rejects anonymous users independently; disabling signup does not clean up existing identities. [Anonymous users](https://supabase.com/docs/guides/auth/auth-anonymous).
6. **Before JWT integration:** Open **JWT Signing Keys** through the official guide's Dashboard link. Inspect current algorithm/kid and public JWKS; no keys/secrets need copying. A legacy-only setup requires separately reviewed transition. Do not rotate/revoke now: revocation and rejecting old algorithms can invalidate existing tokens, and caches delay propagation. [Signing keys](https://supabase.com/docs/guides/auth/signing-keys).
7. **Before hosted integration:** Inspect a controlled JWT locally against the claims reference. Confirm issuer, audience, subject/session UUIDs, algorithm and anonymous flag. A token is secret; do not use a third-party decoder or paste it in chat. Record only public configuration, not token contents. [Claims](https://supabase.com/docs/guides/auth/jwt-fields).
8. **Required:** **Connect**, or **Settings → API Keys**: record project URL and existing publishable key locally. These are public application values; passwords, access/refresh tokens and DB credentials are secrets. No Auth admin/service-role key is needed. Do not rotate keys. [API keys](https://supabase.com/docs/guides/getting-started/api-keys).
9. **Inspect now; change later only if approved:** **Authentication → Sessions** and JWT expiry settings linked from the sessions guide. Record actual token lifetime/revocation window without imposing the old provisional 15 minutes. New expiry settings generally apply to newly issued tokens; sign-out/revocation actions can affect existing sessions. [Sessions](https://supabase.com/docs/guides/auth/sessions).
10. **Before hosted startup:** Establish reviewed DB roles/grants/exposure/TLS/history settings above and prepare unshared process/IDE configuration. Review exact V2 and separately authorize hosted execution. Only then set `BUILDAT_LOCAL_MIGRATIONS_APPROVED=true` for the designated local authority. Auth URLs/publishable key are public; runtime/migration passwords and tokens remain private. [Connection guidance](https://supabase.com/docs/guides/database/connecting-to-postgres).
11. **Before manual acceptance:** **Authentication → Users**: prepare a controlled account using a real confirmation flow to an authorized team address. Built-in SMTP only sends to team addresses and is best effort; do not require external-user testing or SMTP setup now. Admin auto-confirmed users do not demonstrate mailbox verification. [Users](https://supabase.com/docs/guides/auth/managing-user-data), [SMTP restrictions](https://supabase.com/docs/guides/auth/auth-smtp).
12. **Later, when actual client routes exist:** **Authentication → URL Configuration**: configure exact web/native callbacks then. Do not invent frontend routes or add wildcard redirects now. Redirect changes affect future flows and must match the actual client. [Redirect URLs](https://supabase.com/docs/guides/auth/redirect-urls).
13. **After implementation and separately approved hosted migration:** Obtain a controlled token through official Auth requests, then test local API/Swagger. GET before bootstrap returns 403; first POST 201; repeat POST 200 with the same internal ID; GET 200; invalid tokens 401. Keep tokens out of shell history, logs, saved collections and persisted Swagger authorization. Manual testing is not claimed by isolated test success. [Password sign-in](https://supabase.com/docs/guides/auth/passwords).

Google OAuth/cloud applications, web/native clients, frontend redirects without actual routes, Resend/SMTP, external signup, domains, storage, Northflank/Vercel/AWS deployment and production secrets remain deferred. Google token-exchange/branding choices belong to its later feature. Resend remains the accepted provider when external signup testing begins.

## Isolated validation

Unit/MVC `*Test` uses synthetic signing keys, loopback JWKS or HTTP mocks; `*IT` uses disposable PostgreSQL 17.11 and real HTTP. No live Supabase calls or credentials are required. Test-only role/bootstrap SQL and deliberately failing migrations are outside production migration locations. Runtime-role HTTP integration separates DML login from Flyway ownership. Missing Docker fails visibly.

Run `sh mvnw --batch-mode test`, focused suites as needed, and `sh mvnw --batch-mode verify`. Preserve the existing foundation tests. Do not count container evidence as proof of Supabase settings, hosted history, TLS, grants or deployment. Final execution counts and limitations belong in [PLANS.md](../PLANS.md).
