# Buildat backend

Buildat is a project-first collaboration network, initially for indie-game development. Creators publish credible projects and recruit for defined roles; builders find suitable work, join teams and build a history of meaningful contributions. AI is an optional assistant for scoping, role descriptions and matching, not the core product.

This repository contains the early `buildat-server` backend foundation. Product capabilities described in the docs are planned unless explicitly identified as implemented. There are no controllers, domain entities, repositories or frontend sources in the inspected tree.

## Stack

Observed on 2026-10-16 in [pom.xml](pom.xml) and the [wrapper configuration](.mvn/wrapper/maven-wrapper.properties):

| Component | Repository evidence |
| --- | --- |
| Java | Target version 25 |
| Spring Boot | Parent 4.1.1 |
| Maven | Wrapper distribution 3.9.16; wrapper 3.3.4 |
| Web and validation | Spring MVC and validation starters |
| Persistence | Spring Data JPA/Hibernate, PostgreSQL runtime driver, Flyway with PostgreSQL support |
| Security and operations | Spring Security and Actuator starters |
| Tests | Boot test starters and one JUnit Jupiter context-load test; no Testcontainers dependency |

The declared Java and Boot versions match the intended backend stack. Dependency versions other than the parent and wrapper are managed through Maven and have not been resolved during this documentation task. PostgreSQL server version is unspecified. Next.js with TypeScript is the intended frontend; no frontend exists here. Intended architecture: modular monolith.

## Repository organization

- `src/main/java/com/buildat/`: application entry point only.
- `src/main/resources/application.yaml`: environment-based datasource, Hibernate validation, disabled open-in-view, enabled Flyway.
- `src/main/resources/db/migration/V1__init.sql`: empty initial migration; establishes no domain schema.
- `src/test/java/com/buildat/`: one `@SpringBootTest` context-load test.
- `.mvn/`, `mvnw`, `mvnw.cmd`: Maven wrapper.
- `docs/`: authoritative product, domain, architecture, business and decision documents.
- `target/`: ignored generated output; its presence does not prove the current checkout builds.

## Local setup and verification

Prerequisites inferred from checked-in configuration: JDK 25 with `JAVA_HOME` configured, an accessible PostgreSQL database, and network access for Maven distribution/dependency downloads when not cached. Installed JDK, PostgreSQL availability and build success have not been verified. A separate Maven installation is unnecessary when using the wrapper.

Provide these environment variables to the process, using local values that are never committed:

- `DATABASE_URL`: PostgreSQL JDBC URL, for example `jdbc:postgresql://localhost:5432/buildat`.
- `DATABASE_USERNAME`: local database user.
- `DATABASE_PASSWORD`: local database password.

The database must already exist and the user must have the permissions needed for migrations. There is no provisioning script or container configuration. TODO: choose a supported PostgreSQL version and document reproducible local provisioning and isolated test database setup. `.env` files are ignored but no `.env` loader is configured; set process environment variables or use an IDE run configuration.

Commands supported by the checked-in wrapper, POM and test source:

| Purpose | PowerShell / Windows | POSIX shell |
| --- | --- | --- |
| Inspect Maven/JDK | `.\mvnw.cmd --version` | `./mvnw --version` |
| Run backend | `.\mvnw.cmd spring-boot:run` | `./mvnw spring-boot:run` |
| Run current test suite | `.\mvnw.cmd test` | `./mvnw test` |
| Maven verification lifecycle | `.\mvnw.cmd verify` | `./mvnw verify` |

These commands are established from repository files, not execution results. Startup and the current context test depend on datasource configuration and PostgreSQL access. `verify` has no project-specific acceptance gate configured beyond the Maven lifecycle. Do not use a production database for local runs or tests. TODO: establish isolated integration tests and confirm clean-checkout execution. No frontend setup command is available.

## Documentation

| Document | Owns |
| --- | --- |
| [AGENTS.md](AGENTS.md) | Standing instructions for future Codex work |
| [PLANS.md](PLANS.md) | Current evidence, implementation order and acceptance gates |
| [PRODUCT.md](docs/PRODUCT.md) | Thesis, audience, positioning and long-term vision |
| [MVP.md](docs/MVP.md) | MVP scope, behavior and private-alpha measures |
| [DOMAIN.md](docs/DOMAIN.md) | Concepts, ownership, permissions and consistency |
| [ARCHITECTURE.md](docs/ARCHITECTURE.md) | Observed architecture and intended engineering direction |
| [BUSINESS.md](docs/BUSINESS.md) | Commercial hypotheses and future funding constraints |
| [DECISIONS.md](docs/DECISIONS.md) | Accepted, proposed and deferred decisions |

Read the roadmap and relevant documents before implementation. Existing ignored `HELP.md`, when present, contains generated Spring/Maven references and remains unchanged.
