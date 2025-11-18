# Copilot Instructions — sdd-modeler (concise)

Purpose: Help AI coding agents be productive quickly in this Java 21, Gradle-managed, multi-module project.

Overview
- Two modules: `state-modeler-core` (model DSL, SDD -> SqlPlan -> Postgres DDL) and `state-modeler-app` (Picocli CLI, H2 SDR persistence, LLM migration orchestration).

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


