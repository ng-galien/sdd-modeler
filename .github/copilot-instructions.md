 # Copilot Instructions for sdd-modeler (summary)

 This project is a multi-module Java 21 library + CLI for State-Driven Design (SDD) that generates PostgreSQL DDL from YAML/JSON models and supports LLM-powered migrations.

 Quick references:
 - CLI & App: `state-modeler-app` (Picocli commands: validate, sql, diagram, register, list, show, delete, diff, migrate)
 - Core: `state-modeler-core` (model records, DSL loaders, validators, SQL plan, PostgreSQL DDL generation)
 - Examples: `instructions/examples/` (model + expected DDL + diagram)

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
 - `instructions/ARCHITECTURE.md`, `instructions/examples/*` for domain and SQL examples

 Quick build/test commands (copyable):
 ```bash
 # Full build + tests
 ./gradlew build
 ./gradlew test
 ./gradlew jacocoTestReport

 # Run CLI commands (validate / sql / migrate)
 ./gradlew :state-modeler-app:run --args="validate instructions/examples/orders-sdd-model.yaml"
 ./gradlew :state-modeler-app:run --args="sql instructions/examples/orders-sdd-model.yaml -o output.sql"
 ./gradlew :state-modeler-app:run --args="migrate orders:1.0 orders:2.0 -o migration.sql"
 ```

 If you need to change SQL generation patterns, update Postgres*Generator classes and verify expected DDL output by comparing to `instructions/examples/` examples and adding integration tests to `state-modeler-core`.

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

