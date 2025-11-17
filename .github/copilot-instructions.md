# Copilot Instructions — sdd-modeler

This repository contains a Java 21 multi-module library + CLI for State-Driven Design (SDD). It converts YAML/JSON SDD models to an internal model and renders production-ready PostgreSQL DDL. It also supports LLM-driven migration generation via LangChain4j.

Quick references
- `state-modeler-core`: model records, DSL loaders, validation, SqlPlan generation and Postgres DDL generators
- `state-modeler-app`: Picocli CLI (validate, sql, register, list, show, delete, diff, migrate), SDR repository using H2, and migration orchestration
- `scripts/examples/`: canonical examples and expected DDL used by test scripts

Essential patterns (do not deviate without reason)
- Use Java 21 records for domain objects; validate inputs in compact constructors and throw `IllegalArgumentException` for invalid inputs.
- Use Vavr (`Try`, `Validation`) for IO and validation flows; avoid mixing `Try` with unchecked exceptions where `Try`/`Validation` is expected.
- SQL generation patterns: entity vs state schema separation, append-only state tables (no UPDATE), `previous_<state>_id` for previous-state references, `idx_<table>_<column>` index naming, and `<state>_source` for `from_any_of` OR transitions (with CHECK constraints).
- No `status` column: current state is derived from `state_intervals` / `current_state` projections/views.

LLM & migration notes
- LangChain4j is used for structured LLM outputs; `LangChainMigrationGenerationService` builds prompts via `MigrationPromptBuilder` and uses a typed assistant interface for structured JSON outputs.
- `ChatModelProvider` abstracts ChatModel creation. `LangChainModelProvider` wires Ollama (default). The CLI supports OpenAI via `OpenAiChatModel` when `OPENAI_API_KEY` is set.
- For unit tests, prefer mocking `ChatModelProvider` or using `MockChatModelProvider` rather than mocking `ChatModel` directly.

Common workflows and commands
- Build + test: `./gradlew build`, `./gradlew test`, `./gradlew jacocoTestReport`.
- Format: `./gradlew spotlessApply` (CI enforces Spotless)
- CLI examples (via Gradle wrapper):
  - `./gradlew :state-modeler-app:run --args="validate scripts/examples/orders-sdd-model.yaml"`
  - `./gradlew :state-modeler-app:run --args="sql scripts/examples/orders-sdd-model.yaml -o output.sql"`
  - `./gradlew :state-modeler-app:run --args="register scripts/examples/orders-sdd-model.yaml -n orders -v 1.0"`
  - `./gradlew :state-modeler-app:run --args="migrate orders:1.0 orders:2.0 --llm ollama --model qwen3:8b -o migration.sql"`
- Migration test script: `scripts/test-migration-generation.sh` (supports `--mini`, `--llm [ollama|openai]`, `--model`, `--ollama-url`, `--openai-key`)

Where to edit code
- SQL generators: `state-modeler-core/src/main/java/io/statemodeler/sql/postgres/` (`PostgresDdlGenerator`, `PostgresTableGenerator`, `PostgresConstraintGenerator`, `PostgresIndexGenerator`, `PostgresViewGenerator`)
- CLI: `state-modeler-app/src/main/java/io/statemodeler/cli/` (`MigrateCommand`, `SqlCommand`, etc.)
- LLM & migration code: `state-modeler-app/src/main/java/io/statemodeler/migration/` (`ChatModelProvider`, `LangChainModelProvider`, `LangChainMigrationGenerationService`, `MigrationPromptBuilder`, `MigrationOrchestrationService`)
- SDR persistence: `state-modeler-app/src/main/java/io/statemodeler/repository/` (H2 repo, DAOs, `H2ConnectionManager`)

Testing notes & recommendations
- Prefer mocking `ChatModelProvider` to swap the LLM provider in tests, and keep `MockChatModelProvider` up to date on LangChain4j changes.
- Add integration/snapshot tests for SQL generation using `scripts/examples` and `state-modeler-app/src/test/resources/examples/` fixtures.
- CLI tests live under `state-modeler-app/src/test/java/` and use the Picocli test harness.

Common pitfalls
- Constraint ordering matters: add unique constraints before FK constraints that rely on them (composite unique/FK ordering matters).
- `PostgresTypeValidator` validates types (case-insensitive, arrays, parameterized types). Add tests if you extend supported types.
- Keep CLI exit codes and stdout/stderr behavior stable for automation scripts.

Extra notes
- `scripts/examples/` and `state-modeler-app/src/test/resources/examples/` provide canonical fixtures and snapshots used by tests and scripts.
- If introducing backward-incompatible changes, update `PR_SUMMARY.md` and include migration notes in PR.

If anything is unclear or missing, ask for specific areas (LLM tests, SQL generation, CLI commands) and I’ll add targeted examples or tests.
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

