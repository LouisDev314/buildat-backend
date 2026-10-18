-- Disposable vanilla PostgreSQL prerequisites; never used by hosted startup.
CREATE ROLE buildat_runtime NOLOGIN NOSUPERUSER NOBYPASSRLS;
CREATE ROLE buildat_test_app LOGIN PASSWORD 'test-only' IN ROLE buildat_runtime NOSUPERUSER NOBYPASSRLS;
CREATE ROLE anon NOLOGIN;
CREATE ROLE authenticated NOLOGIN;
