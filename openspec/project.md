# Project Context

## Purpose
sdd-modeler is a Java library, CLI, and Gradle plugin implementing State-Driven Design (SDD).
It loads declarative YAML/JSON models of entities, states, transitions, and extensions, validates
them, and generates PostgreSQL DDL, diagrams, and JSON schema artifacts. The CLI also manages
versioned State Definition Records (SDR) in a local repository and includes optional
AI-assisted migration tooling. The Java generator targets a Spring Data / Spring Boot stack and
can emit HTTP API artifacts.

## Tech Stack
- Java 21 toolchain (records, modern language features) with Gradle 8.x multi-module build
- Modules: `state-modeler-core` (model/validation/SQL/diagram), `state-modeler-app` (CLI + SDR repo),
  `state-modeler-gradle-plugin`, and `sample`
- Parsing/validation: Jackson (YAML/JSON), Vavr (Validation/Try)
- CLI: Picocli; logging: SLF4J + Logback
- Persistence: H2 embedded DB for SDR repository; PostgreSQL as the DDL target dialect
- Testing: JUnit 5, Testcontainers (Postgres), JaCoCo coverage
- Formatting: Spotless + Palantir Java Format
- Code generation targets: Spring Data JDBC + Spring Boot (controllers, HTTP clients)
- Optional: LangChain4j + Ollama for migrations; GraalVM native image for CLI builds

## Project Conventions

### Code Style
- Favor immutability: Java records or Lombok `@Value`; avoid mutable POJOs
- Use Lombok `@Builder` for types with many optional parameters
- Null safety via `@NonNull` or `Objects.requireNonNull()`
- Prefer runtime exceptions (`IllegalArgumentException`, `IllegalStateException`) except for real I/O
- Formatting enforced by Spotless (Palantir Java Format), remove unused imports, trim whitespace
- Logging: SLF4J/Logback for diagnostics; use `System.out` only for primary CLI output; avoid direct
  `System.err` usage

### Architecture Patterns
- Pipeline: load YAML/JSON -> validate -> generate SQL plan -> render PostgreSQL DDL
- Two-schema layout: entity tables in `schema`, state/extension/projection tables in `state_schema`
  (default `<schema>_states`)
- States are append-only facts; no status column; current state derived via projections
- Explicit transitions (`from`, `from_any_of`) with mapping tables and FK constraints
- Extensions for optional/non-decisional data; projections for intervals and current state views
- Generate FK indexes for join performance using `idx_<table>_<column>` naming
- Postgres type validation rejects unsupported/invalid attribute types early
- SDR repository stores immutable snapshots (schema + DDL + hashes) in a local H2 database with
  path resolution: CLI flag -> env var -> config -> default

### Code Generation
- Generator driven by `database.generator_options.packageName` in the SDD model
- Java is the only supported target language
- Per-entity artifacts: strong `{{Entity}}Id` record, sealed `{{Entity}}State` ADT, DTOs, state
  repositories, and a `{{Entity}}DomainState` projection for current state
- Service layer exposes explicit transition methods and enforces non-null inputs; transitions are
  transactional in the default implementation
- Spring Boot auto-configuration wires repositories and services; HTTP controllers and declarative
  clients (`@HttpExchange`) are generated, along with example `.http` files

### Testing Strategy
- Unit tests for parsing, validation, and SQL/diagram generation
- Integration tests compare generated DDL to expected outputs; Postgres via Testcontainers
- CLI end-to-end tests
- No Mockito; direct tests only
- JaCoCo aggregated coverage threshold >= 80%

### Git Workflow
- Fork -> feature branch -> PR (per README)
- Run `./gradlew spotlessApply` and tests before submitting
- No documented commit message convention; keep commits descriptive

## Domain Context
State-Driven Design (SDD) models domains as immutable state facts with explicit transitions.
Entities hold stable identity data; each state is a separate table/record with its own attributes.
Models are defined in YAML/JSON and drive SQL/diagram/code generation. SDR (State Definition Record)
is an immutable snapshot of schema + DDL with cryptographic hashes for versioning.
See the SDD specs in `@openspec/specs/sdd.md` for the core principles and SQL mapping notes.

## Important Constraints
- PostgreSQL is the primary supported SQL dialect; attribute types are validated against Postgres
- Two-schema DDL layout is core to the model (entity vs. state schema)
- CLI output to stdout is treated as stable, pipe-friendly output
- LLM-based migration features require network access and an external model provider
- SDR lookups typically require full schema hashes; short hashes are not a stable interface

## External Dependencies
- PostgreSQL (DDL target and integration tests)
- H2 embedded database for local SDR repository
- LLM providers via LangChain4j (optional, for migrations)
