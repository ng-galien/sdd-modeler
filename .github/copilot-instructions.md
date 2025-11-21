<!--
  Copilot Instructions for sdd-modeler
  — concise, actionable guidance for AI coding agents working in this repository
-->

# Copilot Instructions — sdd-modeler

Purpose: Help AI coding agents be productive quickly in this Java 21, Gradle-managed, multi-module project.

Quick summary
- Two modules: `state-modeler-core` (DSL, model validation, SQL generation) and `state-modeler-app` (Picocli CLI, H2 SDR persistence, LLM-driven migration generation).
- Build & tests: `./gradlew build` / `./gradlew test`. Run single-module tests with `./gradlew :state-modeler-core:test` and `./gradlew :state-modeler-app:test`.
- Formatting: `./gradlew spotlessApply`. CI enforces Spotless.

What to read first (order matters)
- README.md — high-level project overview and module links.
- state-modeler-core/README.md — DSL, modeling concepts, and architecture.
- state-modeler-app/README.md — CLI commands, repository management, and migration.
- state-modeler-gradle-plugin/README.md — Gradle plugin configuration.
- DEV_README.md — developer setup and contribution guidelines.
- state-modeler-core/src/main/java/io/statemodeler/dsl — DSL parsing (SddModel, EntityDef).
- state-modeler-core/src/main/java/io/statemodeler/sql/postgres — SQL generation logic.

Key project patterns & conventions
- Java 21 records for domain objects and DTOs. Use compact constructors to validate inputs (throw IllegalArgumentException for invalid data).
- Use Vavr (`io.vavr.control.Try`, `Validation`) extensively for functionally-returned errors. Methods (especially repo methods) return `Try<T>` rather than throwing.
- SQL design: entity vs state schema separation; append-only state tables; composite predecessor columns like `previous_<state>_id` paired with `entity_id` and Composite FKs `(previous_xxx_id, entity_id) -> (id, entity_id)` to ensure same-entity transitions.
- Naming: indexes are prefixed with `idx_<table>_<columns>`. Unique constraints often include `(id, entity_id)` and a `(entity_id)` unique constraint where appropriate.
- Constraint & index order matters: create UNIQUE constraints before foreign keys which reference them. `PostgresDdlGenerator` and integration tests rely on a specific ordering.

SQL generation & migration
- Edit DDL generation in: `state-modeler-core/src/main/java/io/statemodeler/sql/postgres/` (PostgresDdlGenerator and generator classes). Unit & integration tests live next to generator code (look for tests in core module).
- `migrate` command uses LangChain4j (Ollama by default). OpenAI is supported via `OPENAI_API_KEY`. The `migrate` command expects the LLM to output a JSON structure with `confidence`, `migrationScript`, and `comments`. See outputs in `build/test-migration-output/` for examples.
- For tests and deterministic behavior use `MockChatModelProvider` (`state-modeler-app/src/test/java/io/statemodeler/migration`) instead of calling a real LLM.

CLI & Picocli testing guidelines
- CLI entrypoint: `state-modeler-app/src/main/java/io/statemodeler/cli/Main.java`. Run the CLI from Gradle with:
  `./gradlew :state-modeler-app:run --args="validate|sql|register|list|show|delete|diff|migrate ..."`
- Test patterns:
  - Black-box tests: start from `new CommandLine(cmd).execute(args)`. Capture output with `CommandLine.setOut(PrintWriter)` or use the repository helper: `PicocliTestHelper.capture(cmd)` or `CliTestHelper.runWithCapture`.
  - White-box tests: instantiate a command directly, set `repositoryMixin`, `testRepository`, or `llmProvider`, and call `command.call()` or `command.run()`.
  - Example (preferred):
    var command = new ShowCommand();
    command.repositoryMixin = createMixin();
    var cmd = new picocli.CommandLine(command);
    try (var capture = PicocliTestHelper.capture(cmd)) {
      int exitCode = cmd.execute("test-model", "--format=all");
    }
- Keep CLI outputs and exit codes stable — tests and scripts rely on them.

Repository & test utilities
- `H2SdrRepository` implements the SDR repository for persistence and is the default implementation for CLI. For unit tests, create an in-memory repository:
  `H2SdrRepository.createInMemory("test-<name>" + System.nanoTime())`.
- Repository interface methods return `Try<T>` (e.g., `Try<Boolean> exists(String schemaHash)`), see `REPOSITORY.md` for signatures.

Validation & type rules
- PostgreSQL type validation is centralized in `PostgresTypeValidator` — update it when adding a new type.
- Model validation uses Vavr's Validation type; `ModelValidators` orchestrates validations for SDD parsing & semantic checks.

Testing & snapshot strategy
- Use `MockChatModelProvider` to avoid network calls when testing LLM features.
- Update snapshot files under `scripts/examples/` and `state-modeler-app/src/test/resources/examples/` when changing DDL generation or operator outputs.
- Functional DDL tests: `scripts/test-ddl-functional.sh` validates the SQL behaviour (FKs, unique constraints, composite FKs, etc.). Use `report_sql_block` helper for descriptive test blocks.

Where to edit code for common tasks
- Add/modify SQL generation code: `state-modeler-core/src/main/java/io/statemodeler/sql/postgres/`
- Update DSL & model validation: `state-modeler-core/src/main/java/io/statemodeler/dsl/` and `state-modeler-core/src/main/java/io/statemodeler/validation/`
- Add CLI commands or change CLI behaviour: `state-modeler-app/src/main/java/io/statemodeler/cli/commands/`
- Change or mock LLM providers: `state-modeler-app/src/main/java/io/statemodeler/migration/` and corresponding tests in `state-modeler-app/src/test/java/io/statemodeler/migration/`

Important PR checklist (apply for changes that affect DDL/CLI/LLM behavior)
- Run `./gradlew spotlessApply` and `./gradlew test` locally.
- For DDL changes: update snapshots under `/scripts/examples` and `state-modeler-app/src/test/resources/examples` and add/update integration tests.
- If adding Postgres types: update `PostgresTypeValidator` and add tests.
- Document breaking changes in `PR_SUMMARY.md`.

Quick debugging tips
- If the `migrate` command fails due to LLM libs, check DEV_README.md for required LangChain4j dependencies and the default provider settings (Ollama) or `OPENAI_API_KEY`.
- If tests fail due to missing formatting, run `./gradlew spotlessApply` to standardize formatting.
- Use local functional DDL script `scripts/test-ddl-functional.sh` and debug the generated SQL in `build/test-output/*`.

Notes / gotchas
- Respect Vavr Try/Validation for repo and parsing flows.
- Composite foreign keys enforce the order of constraint creation. Tests may fail if constraint ordering is wrong.
- Always run both unit and integration tests for DDL generator changes.

If anything is missing or unclear, leave a short note as a PR comment referencing the file(s) you'd like more context for; we're happy to expand the docs.
# Copilot Instructions — sdd-modeler (concise)

Purpose: Help AI coding agents be productive quickly in this Java 21, Gradle-managed, multi-module project.

Overview
- Three modules: `state-modeler-core` (DSL, validation, SQL gen), `state-modeler-app` (CLI, Repository, AI), and `state-modeler-gradle-plugin` (Build integration).

Quick commands
- Build & tests: `./gradlew build` / `./gradlew test`.
- Format: `./gradlew spotlessApply` (CI enforces Spotless).
- CLI: `./gradlew :state-modeler-app:run --args="validate|sql|register|list|show|delete|diff|migrate ..."`.

Core conventions
- Java 21 records for DTOs and domain objects; validate input in compact constructors (IllegalArgumentException on invalid input).
- Use Vavr (`io.vavr.control.Try`, `Validation`) for IO/validation; return Try/Validation rather than throwing where expected.
- SQL patterns: entity vs state schema separation, append-only state tables, use `previous_<state>_id`, index names prefixed `idx_`, `from_any_of` => `<state>_source` mapping tables with CHECK constraints.
- No global `status` column — current state is derived via `state_intervals` / `current_state` views.

Where to edit key behavior
- SQL generation: `state-modeler-core/src/main/java/io/statemodeler/sql/postgres/` (look at `PostgresDdlGenerator` and generator classes).
- DSL & validation: `state-modeler-core/src/main/java/io/statemodeler/dsl/` and `.../core` (SddModel, EntityDef, StateDef).
- CLI & persistence: `state-modeler-app/src/main/java/io/statemodeler/cli/` and `.../repository/` (H2 DAOs).
- LLM/migration: `state-modeler-app/src/main/java/io/statemodeler/migration/` (`ChatModelProvider`, `LangChainModelProvider`, `MockChatModelProvider`).

LLM & migration
- `migrate` uses LangChain4j (Ollama by default); OpenAI supported via `OPENAI_API_KEY`.
- Tests should use `MockChatModelProvider` to avoid network calls.

Tests & snapshots
- Snapshots: update `scripts/examples` and `state-modeler-app/src/test/resources/examples/` when DDL changes.
- CLI tests use Picocli harness in `state-modeler-app/src/test/java/`.

Testing Picocli commands
- Black-box testing: prefer the programmatic API. Create a command instance, configure its dependencies/helpers, capture output with `CommandLine.setOut(PrintWriter)` or the command's `setOutput` method (many commands expose this helper), and call `new CommandLine(cmd).execute(args)` to get an exit code. Assert on both exit code and captured output.
 - Black-box testing: prefer the programmatic API. Create a command instance, configure its dependencies/helpers, and capture output. Two recommended approaches:
		- Set the command output writer: `var out = new StringWriter(); command.setOutput(new PrintWriter(out, true));` and run via `new CommandLine(command).execute(args)`.
		- Use the repository-provided `PicocliTestHelper` for tests that assert against both picocli outputs and logging to STDERR. Example:
			- var command = new ShowCommand();
			- command.repositoryMixin = createMixin();
			- var cmd = new picocli.CommandLine(command);
			- try (var capture = PicocliTestHelper.capture(cmd)) {
					int exitCode = cmd.execute("test-model", "--format=all");
					assertEquals(0, exitCode);
					assertTrue((capture.getOut() + capture.getErr()).contains("=== SDR Metadata ==="));
				}

			- Or use the convenience `CliTestHelper.runWithCapture` wrapper for a shorter pattern:
				- CliTestHelper.runWithCapture(cmd, result -> {
					assertEquals(0, result.exitCode());
					assertTrue(result.out().contains("=== SDR Metadata ==="));
				}, "test-model", "--format=all");
- White-box testing: instantiate the command directly, set fields (e.g., `repositoryMixin`, `testRepository`, or `llmProvider`), and call `command.call()` or `command.run()`. Assert on repository state and object fields.
- Avoid System.exit in tests. If you must validate exit codes from a `main()` that calls `System.exit`, use a helper like SystemLambda or capture with `catchSystemExit()`.
- Mock external dependencies: use `MockChatModelProvider` (in `state-modeler-app/src/test/java/io/statemodeler/migration/`) for deterministic LLM outputs and use in-memory H2 repositories in `H2SdrRepository.createInMemory("test-<name>" + System.nanoTime())` for fast DB tests.
- Example (Java - black-box; repository tests follow this pattern):
	- var command = new ShowCommand();
	- command.repositoryMixin = createMixin();
	- var out = new java.io.StringWriter();
	- command.setOutput(new java.io.PrintWriter(out, true));
	- var cmd = new picocli.CommandLine(command);
	- int exitCode = cmd.execute("test-model", "--format=all");
	- assertEquals(0, exitCode);
	- assertTrue(out.toString().contains("=== SDR Metadata ==="));

Where to look for examples
- `state-modeler-app/src/test/java/io/statemodeler/cli/commands/*CommandTest.java` show standard patterns for capture + assertions (ShowCommandTest, MigrateCommandTest, SqlCommandTest, etc.).
- `state-modeler-app/src/test/java/io/statemodeler/migration/MockChatModelProvider.java` demonstrates how to mock LLM responses for `MigrateCommand` tests.

Tips & common pitfalls
- Constraint ordering matters (UNIQUE before FK referencing it).
- Update `PostgresTypeValidator` when adding new SQL types.
- Preserve CLI outputs/exit codes — scripts & tests depend on them.

PR checklist
- Run `./gradlew spotlessApply` and `./gradlew test` locally.
- If changing DDL, add/update snapshots and tests; document breaking changes in `PR_SUMMARY.md`.


