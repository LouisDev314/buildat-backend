-- Review before hosted execution. The NOLOGIN buildat_runtime group must already exist.
-- Keep Flyway history in its verified schema; application objects are explicitly qualified.
CREATE SCHEMA buildat;
REVOKE ALL ON SCHEMA buildat FROM PUBLIC;

CREATE TABLE buildat.accounts (
    id uuid PRIMARY KEY,
    identity_issuer text NOT NULL CHECK (btrim(identity_issuer) <> ''),
    identity_subject uuid NOT NULL,
    standing text NOT NULL CHECK (standing IN ('ACTIVE', 'SUSPENDED', 'DELETED')),
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL CHECK (updated_at >= created_at),
    CONSTRAINT accounts_external_identity_unique UNIQUE (identity_issuer, identity_subject)
);
REVOKE ALL ON buildat.accounts FROM PUBLIC;
-- Remove provider/browser grants inherited from the migration owner's default privileges.
DO $$
DECLARE browser_role text;
BEGIN
    FOREACH browser_role IN ARRAY ARRAY['anon', 'authenticated'] LOOP
        IF EXISTS (SELECT FROM pg_roles WHERE rolname = browser_role) THEN
            EXECUTE format('REVOKE ALL ON SCHEMA buildat FROM %I', browser_role);
            EXECUTE format('REVOKE ALL ON buildat.accounts FROM %I', browser_role);
        END IF;
    END LOOP;
END $$;
GRANT USAGE ON SCHEMA buildat TO buildat_runtime;
GRANT SELECT, INSERT ON buildat.accounts TO buildat_runtime;
-- PostgreSQL SELECT FOR UPDATE requires UPDATE privilege on at least one column.
-- Permit only timestamp updates: no identity, standing, or deletion authority.
GRANT UPDATE (updated_at) ON buildat.accounts TO buildat_runtime;
ALTER TABLE buildat.accounts ENABLE ROW LEVEL SECURITY;
CREATE POLICY accounts_backend_read ON buildat.accounts FOR SELECT TO buildat_runtime USING (true);
CREATE POLICY accounts_backend_create ON buildat.accounts FOR INSERT TO buildat_runtime
    WITH CHECK (standing = 'ACTIVE');
CREATE POLICY accounts_backend_lock ON buildat.accounts FOR UPDATE TO buildat_runtime
    USING (true) WITH CHECK (true);
