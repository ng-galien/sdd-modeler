# Copilot Instructions — sdd-modeler

Purpose: Short, actionable guidance to make AI coding agents productive in this Java multi-module repository. Keep changes small and test-driven; follow the project's conventions.

Big picture
- Two main modules:
	- `state-modeler-core`: Model definitions (records), loaders (YAML/JSON), validation, SqlPlan and PostgreSQL DDL generators.
	- `state-modeler-app`: Picocli-based CLI, an H2 SDR repository, persistence DAOs, and LLM-driven migration orchestration (LangChain4j).
- Flow: YAML/JSON model -> internal SddModel -> SqlPlan -> Postgres DDL -> SdrRecord (schema + DDL + hashes) -> optional migration.

Developer workflows
- Build & run: `./gradlew build` and `./gradlew test` (root) or run a single module: `./gradlew :state-modeler-core:test`.
- Formatting: `./gradlew spotlessApply` (CI enforces Spotless). Run `./gradlew spotlessCheck` to validate.
- CLI quick runs: `./gradlew :state-modeler-app:run --args="validate|sql|diagram|register|list|show|delete|diff|migrate ..."`.
- Generate JSON Schema for distribution: `./gradlew :state-modeler-core:generateJsonSchema` and `./gradlew distributeSchema` to copy to repo root.

Conventions & patterns (must follow)
- Java toolchain: Gradle uses Java 21 (toolchain configured in `build.gradle.kts`). Prefer records for DTOs and domain objects.
- Validation: Use Vavr (`io.vavr.control.Try`, `Validation`) and prefer `Try`/`Validation` over throwing runtime exceptions unless validation is appropriate in a compact record constructor.
- Records must validate inputs in compact constructors (throw `IllegalArgumentException` for invalid values). See `SdrRecord`, `EntityDef`.
- SQL generation: separate entity schema vs state schema; state tables are append-only (no UPDATE), `previous_<state>_id` links for previous-state relations, `idx_<table>_<column>` for indexes; `from_any_of` generates `<state>_source` mapping tables with `CHECK` constraints.
- No `status` column: rely on `state_intervals` / `current_state` views for current state derivation.

Where to change behavior (key files)
- Core & SQL generation: `state-modeler-core/src/main/java/io/statemodeler/sql/postgres/` (generate table/constraint/index/view order via `PostgresDdlGenerator` -> `PostgresTableGenerator`, `PostgresConstraintGenerator`, `PostgresIndexGenerator`, `PostgresViewGenerator`).
- DSL, model, validation: `state-modeler-core/src/main/java/io/statemodeler/dsl` and `.../core` (`SddModel`, `EntityDef`, `StateDef`, `AttributeDef`).
- SDR factory: `state-modeler-core/src/main/java/io/statemodeler/sdr/DefaultSdrFactory.java` and `SdrRecord.java` (canonicalization & hashing). Use `DefaultSdrFactory` to create SdrRecord objects.
- CLI & repository: `state-modeler-app/src/main/java/io/statemodeler/cli/**` (commands) and `state-modeler-app/src/main/java/io/statemodeler/repository/**` (H2 repo + DAOs).
- LLM & migration: `state-modeler-app/src/main/java/io/statemodeler/migration/**`. Test-friendly `ChatModelProvider` abstraction with `LangChainModelProvider` (Ollama) and `MockChatModelProvider` in tests.

Testing & snapshots
- Use `scripts/examples/` and `state-modeler-app/src/test/resources/examples/` for canonical model fixtures and expected SQL snapshots. Add tests when you change DDL output.
- CLI tests use Picocli test harness under `state-modeler-app/src/test/java/`.
- Run aggregated coverage: `./gradlew jacocoAggregatedReport` or `./gradlew jacocoAggregatedCoverageVerification`.

LLM and Runtime Notes
- `migrate` command depends on LangChain4j runtime; the default provider is Ollama (`LangChainModelProvider`). OpenAI is supported via `OPENAI_API_KEY` env var and `OpenAiChatModel` if configured. The CLI will log a clear error if LangChain jars are missing.
- Unit tests: prefer mocking `ChatModelProvider` (see test `MockChatModelProvider`) to exercise migration orchestration without network calls.

Common pitfalls & tips
- Constraint FK ordering: unique constraints must exist before foreign keys that depend on them. The generators follow an ordered rendering, but tests should exercise composite FKs.
- If you change a generator, update both unit tests and `scripts/examples` fixtures; add new integration snapshot tests as needed.
- Update `PostgresTypeValidator` when adding new SQL types or parameterized types (e.g., NUMERIC, TIMESTAMP, arrays).
- Keep CLI outputs, exit codes, and JSON/YAML output stable; automation scripts depend on them.

Make PRs easy to review
- Run `./gradlew spotlessApply` and `./gradlew test` before PRs. CI verifies Spotless and Jacoco.
- If a PR introduces a change that breaks DDL snapshots, update the appropriate example SQL under `scripts/examples` and add/update tests under `state-modeler-core`/`state-modeler-app`.
- If the change is behavioral or public API-breaking, add migration notes to `PR_SUMMARY.md`.

Where to ask for help
- Check `instructions/ARCHITECTURE.md`, `instructions/SDR_REPOSITORY_DESIGN.md`, and `DEV_README.md` for architecture and design decisions. If a convention is unclear, prefer changing the docs versus the code.

If something is missing or causes confusion, open an issue or ask for a guided update; I’ll iterate on these instructions.
````markdown
# Copilot Instructions — sdd-modeler

This repo contains a Java 21 multi-module library + CLI for State-Driven Design (SDD): model YAML/JSON -> internal SDD model -> PostgreSQL DDL; it supports LLM-powered migration generation via LangChain4j.

Quick references
- `state-modeler-core`: SDD model, loaders, validation, SqlPlan and Postgres DDL generators
- `state-modeler-app`: Picocli CLI, H2 SDR repository, and migration orchestration
- `scripts/examples/`: canonical fixtures (used by tests and scripts)

Essential patterns & architecture (do not deviate without reason)
- Prefer Java 21 records for domain objects; use compact constructors with explicit null checks and throw `IllegalArgumentException` on invalid input.
- Use Vavr (`io.vavr.control.Try`, `Validation`) for IO & validations; avoid mixing `Try` with unchecked exceptions where `Try` is expected by callers.
- SQL design: separate entity vs state schemas, use append-only state tables (no UPDATE), use `previous_<state>_id` for chained FK references, `idx_<table>_<column>` indexes, and `<state>_source` OR mapping tables with CHECKs for `from_any_of` transitions.
- Projections: derive current state with `state_intervals` and `current_state` views — do not add a global `status` column.

LLM & migration patterns
- LangChain4j is the library for LLM integration. `LangChainMigrationGenerationService` uses an `AiServices` assistant to generate structured migration output.
- Abstraction: `ChatModelProvider` (interface) exists so the app code depends on a provider rather than ChatModel API directly. `LangChainModelProvider` provides Ollama via `OllamaChatModel`. The CLI also supports `OpenAiChatModel` if `OPENAI_API_KEY` is present.
- Tests: prefer mocking `ChatModelProvider`/`LangChainModelProvider` rather than `ChatModel` to make unit tests resilient to LangChain4j CLI API changes.

Tests / build / runnable commands
- Build & tests: `./gradlew build`, `./gradlew test`, `./gradlew jacocoTestReport`.
- Formatting: `./gradlew spotlessApply` (CI enforces Spotless).
- CLI (via Gradle wrapper):
  - Validate: `./gradlew :state-modeler-app:run --args="validate scripts/examples/orders-sdd-model.yaml"`
  - Generate SQL: `./gradlew :state-modeler-app:run --args="sql scripts/examples/orders-sdd-model.yaml -o output.sql"`
  - Register: `./gradlew :state-modeler-app:run --args="register scripts/examples/orders-sdd-model.yaml -n orders -v 1.0"`
  - Migrate: `./gradlew :state-modeler-app:run --args="migrate orders:1.0 orders:2.0 --llm ollama --model qwen3:8b -o migration.sql"`
- Migration test script: `scripts/test-migration-generation.sh --mini --llm ollama --model qwen3:8b --ollama-url http://localhost:11434`.
  - OpenAI: set `OPENAI_API_KEY` env var, or pass `--openai-key` to the script when using `--llm openai`.

Where to edit code for changes
- SQL generation: `state-modeler-core/src/main/java/io/statemodeler/sql/postgres/` (Postgres generators) and `PostgresDdlGenerator` for overall rendering.
- CLI: `state-modeler-app/src/main/java/io/statemodeler/cli/` (Picocli commands; `MigrateCommand` orchestrates migration flow).
- Migration/LLM: `state-modeler-app/src/main/java/io/statemodeler/migration/` (`ChatModelProvider`, `LangChainModelProvider`, `LangChainMigrationGenerationService`, `MigrationPromptBuilder`, `MigrationOrchestrationService`).
- SDR persistence: `state-modeler-app/src/main/java/io/statemodeler/repository/` (H2 repository, DAOs, and connection manager); check `sdr_records` and `sdr_migrations` DDL there.

Tests & tooling recommendations
- Overriding LLM behavior: prefer `LangChainModelProvider`/`ChatModelProvider` to swap providers in tests; update `MockChatModelProvider` tests if LangChain4j API changes.
- If adding new SQL generation behavior, add snapshot/integration tests using `scripts/examples` fixtures.
- If the LangChain4j API breaks tests, update the `MockChatModelProvider` and tests to the new API — but prefer to keep the test surface at the `ChatModelProvider` interface.

Common pitfalls
- Constraint ordering matters: add UNIQUE indices or constraints before FK constraints if FK refers to composite unique constraints.
- Keep `PostgresTypeValidator` in mind when adding new types; ensure case-insensitive parsing and param / arrays supported.
- Keep CLI exit codes and outputs stable for automation / scripts — tests and scripts rely on CLI behavior.

Extra notes
- Examples & snapshots: `scripts/examples/*` and `state-modeler-app/src/test/resources/examples/*` are canonical fixtures used across tests and documentation.
- If you introduce a new public API or break behavior, update `PR_SUMMARY.md` and add migration notes.

Need help or clarification? Ask for specific examples or tests to be updated — I'll add file/line examples.

```` # Copilot Instructions for sdd-modeler (summary)

 This project is a multi-module Java 21 library + CLI for State-Driven Design (SDD) that generates PostgreSQL DDL from YAML/JSON models and supports LLM-powered migrations.

 Quick references:
 - CLI & App: `state-modeler-app` (Picocli commands: validate, sql, diagram, register, list, show, delete, diff, migrate)
 - Core: `state-modeler-core` (model records, DSL loaders, validators, SQL plan, PostgreSQL DDL generation)
 - Examples: `scripts/examples/` (model + expected DDL + diagram)

 Key guidance for AI agents working in this repository:
 1. Prefer Java 21 records for DTOs (see `io.statemodeler.core.*`). Keep inline null checks in record constructors and use `IllegalArgumentException` for validation errors.
 2. Use Vavr for functional I/O & validations (`Try`, `Validation`). When parsing/loading models, prefer `Try<T>` and return clear messages for failures.
 3. Use project SQL generation patterns — entity vs state tables, `previous_<state>_id` naming, `created_at TIMESTAMPTZ DEFAULT now()`, `idx_<table>_<cols>` index names, and OR transition mapping tables with a CHECK constraint.
 4. No global status columns — current state is derived via projections/views (intervals / current_state). See `PostgresViewGenerator` & `orders-sdd-ddl.sql`.
 5. Keep CLI behavior intact: `validate`, `sql`, `register`, `list`, `show`, `delete`, `diff`, and `migrate` are implemented; keep arguments and exit codes consistent.
 6. Migration (`migrate`) uses LangChain4j for LLM-based SQL generation and is runtime-optional: migration commands depend on LangChain4j libs (error shown when missing). Both Ollama and OpenAI are supported as `--llm` providers. For OpenAI, set `OPENAI_API_KEY` environment variable before use. See `LangChainMigrationGenerationService`, `LangChainModelProvider`, and `MigrateCommand` for details.
 7. Tests & formatting are required: run `./gradlew test`, `./gradlew jacocoTestReport`, and `./gradlew spotlessApply` before commits. CI enforces Spotless.
 8. Keep code style aligned to current patterns: prefer records, prefer explicit null checks, prefer standard java.util collections, JUnit 5 for tests — do not introduce new testing frameworks or assertion libraries.
 9. Avoid unnecessary Lombok annotations — the project relies primarily on records. If a Lombok annotation is added, justify why a record cannot accomplish the same goal.

 Useful files to inspect before changing behavior:
 - `state-modeler-core/src/main/java/io/statemodeler/core/` (SddModel, EntityDef, StateDef, AttributeDef)
 - `state-modeler-core/src/main/java/io/statemodeler/sql/postgres/` (Table/Constraint/Index/View generators)
 - `state-modeler-app/src/main/java/io/statemodeler/cli/` (Picocli command implementations)
 - `state-modeler-app/src/main/java/io/statemodeler/migration/` (LLM migration support)
 - `instructions/ARCHITECTURE.md`, `scripts/examples/*` (or `state-modeler-app/src/test/resources/examples/*`) for domain and SQL examples

 Quick build/test commands (copyable):
 ```bash
 # Full build + tests
 ./gradlew build
 ./gradlew test
 ./gradlew jacocoTestReport

 # Run CLI commands (validate / sql / migrate)
 ./gradlew :state-modeler-app:run --args="validate scripts/examples/orders-sdd-model.yaml"
 ./gradlew :state-modeler-app:run --args="sql scripts/examples/orders-sdd-model.yaml -o output.sql"
 ./gradlew :state-modeler-app:run --args="migrate orders:1.0 orders:2.0 -o migration.sql"
 ```

 If you need to change SQL generation patterns, update Postgres*Generator classes and verify expected DDL output by comparing to `scripts/examples/` or `state-modeler-app/src/test/resources/examples/` examples and adding integration tests to `state-modeler-core`.

 When in doubt:
 - Read `instructions/ARCHITECTURE.md` and the `orders-sdd-*` examples first
 - Keep changes small and test-driven; run `./gradlew test` locally
 - Ask for clarification on ambiguous naming or dialect changes — the `PostgresDdlGenerator` is authoritative for how DDL should be emitted

Implementation tips for AI agents (do these first when editing):
- When adding or changing SQL generation patterns, update the specific Postgres generators (see `state-modeler-core/src/main/java/io/statemodeler/sql/postgres/`):
	- `PostgresTableGenerator` - table layouts (entity, state, extension, OR mapping)
	- `PostgresConstraintGenerator` - UNIQUE, FK, CHECK constraints (ordering matters)
	- `PostgresIndexGenerator` - indexes for FK columns using `idx_<table>_<col>` naming
	- `PostgresViewGenerator` - ensure `intervals` views are generated before `current_state`
- DDL rendering occurs in `PostgresDdlGenerator` and follows these steps:
	1. Build SqlPlan with tables, views, constraints and indexes
	2. Render schema creation statements
	3. Render tables (CREATE TABLE)
	4. Render constraints (ALTER TABLE ... ADD CONSTRAINT ...)
	5. Render indexes (CREATE INDEX ...)
	6. Render views (CREATE VIEW ...)
- Preserve constraint dependency order when adding new constraints or foreign keys. The code relies on unique constraints existing before FK constraints are added to support composite FK references.
- To add a new CLI behavior or option, update `state-modeler-app/src/main/java/io/statemodeler/cli/` and add unit/CLI tests under `state-modeler-app/src/test/java/`.
- For LLM-based migration generation:
	- `LangChainMigrationGenerationService` implements `MigrationGenerationService` and uses structured JSON schema outputs.
	- `LangChainModelProvider` provides runtime-specific ChatModel (Ollama is the main supported provider).
	- `MigrationPromptBuilder` builds the prompt including old/new DDL and diffs. Check `MigrationOrchestrationService` for how migrations are persisted in `SdrMigration`.
- Repository persistence (SDR + migrations) is implemented using H2 (see `state-modeler-app/src/main/java/io/statemodeler/repository/`):
	- `H2SdrRepository` (high-level) and DAOs (`H2SdrRecordDao`, `H2SdrMigrationDao`) perform DB operations.
	- The `H2ConnectionManager` constructs the schema and indices (see `sdr_records` and `sdr_migrations` DDL in code).
- Tests to run for confirmation:
	- `state-modeler-core` unit & integration tests: `./gradlew :state-modeler-core:test` or `./gradlew test`
	- `state-modeler-app` CLI functional tests: `./gradlew :state-modeler-app:test`
- If you introduce new public APIs or break backward compatibility, bump `sdr-version` or add migration notes to `PR_SUMMARY.md`.

Small code patterns & examples:
- Use `io.vavr.control.Try` for IO calls; return `Try.success` or `Try.failure` as in `YamlModelLoader`/`JsonModelLoader`.
- Use `io.vavr.control.Validation<List<ValidationError>, T>` for model validation results (see `DefaultModelValidator`).
- Use `Objects.requireNonNull()` or `if (x==null) throw new IllegalArgumentException("x cannot be null")` in records' compact constructors for input validation.
- Index name example: `idx_order_pending_order_id` (generated in `PostgresIndexGenerator`).
- Naming for OR transition table: `<state>_source` (e.g., `canceled_source`) with a `CHECK` constraint created by `PostgresConstraintGenerator`.

If anything above is ambiguous or incomplete, tell me which area you'd like expanded and I’ll add specific file/line examples or code snippets.

