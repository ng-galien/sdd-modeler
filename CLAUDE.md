# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**sdd-modeler** is a Java 21 multi-module project implementing State-Driven Design (SDD) — a domain modeling paradigm that treats application states as immutable facts rather than status columns. It generates PostgreSQL DDL, Mermaid diagrams, and Java code from declarative YAML/JSON schema definitions.

## Build Commands

```bash
# Build & test
./gradlew build
./gradlew test

# Single module tests
./gradlew :state-modeler-core:test
./gradlew :state-modeler-app:test

# Format code (CI enforced)
./gradlew spotlessApply
./gradlew spotlessCheck

# Coverage report
./gradlew jacocoAggregatedReport

# Run CLI
./gradlew :state-modeler-app:run --args="validate scripts/examples/orders-sdd-model.yaml"
./gradlew :state-modeler-app:run --args="sql scripts/examples/orders-sdd-model.yaml"
./gradlew :state-modeler-app:run --args="diagram scripts/examples/orders-sdd-model.yaml"

# Native image (requires GraalVM)
./gradlew :state-modeler-app:nativeCompile
```

## Architecture

### Module Structure

| Module | Purpose |
|--------|---------|
| `state-modeler-core` | DSL parsing, model validation, SQL/diagram generation |
| `state-modeler-app` | Picocli CLI, H2 SDR repository, LLM migration |
| `state-modeler-gradle-plugin` | Gradle build integration |
| `state-modeler-maven-plugin` | Maven build integration |
| `sample/` | Spring Boot example (Gradle) |
| `maven-sample/` | Maven example |

### Core Package Structure

- `io.statemodeler.core` — Domain model (SddModel, EntityDef, StateDef)
- `io.statemodeler.dsl` — YAML/JSON parsing with Jackson
- `io.statemodeler.validation` — Model validation with Vavr
- `io.statemodeler.sql.postgres` — PostgreSQL DDL generation (Pebble templates)
- `io.statemodeler.diagram` — Mermaid diagram generation
- `io.statemodeler.sdr` — State Definition Record (versioning & hashing)
- `io.statemodeler.cli` — Picocli commands
- `io.statemodeler.repository` — H2-based SDR repository
- `io.statemodeler.migration` — LLM-powered migration generation

### Key Architectural Principle: Two-Schema Separation

- **Entity Schema** (`public`): Stable entity data (orders, customers)
- **State Schema** (`public_states`): State tables, extension tables, projections, transitions

No global `status` column — current state is derived via `state_intervals`/`current_state` views.

## Code Patterns & Conventions

### Java Style

- **Java 21 records** for domain objects and DTOs
- **Compact constructors** validate inputs (throw `IllegalArgumentException`)
- **Vavr** (`Try<T>`, `Validation<E, T>`) for functional error handling — methods return `Try<T>` rather than throwing
- **SLF4J + Logback** for logging; `System.out` only for CLI output
- **Runtime exceptions** preferred except for genuine I/O

### SQL Design Patterns

- **Composite FKs**: `(previous_<state>_id, entity_id) -> (id, entity_id)` for same-entity transitions
- **Index naming**: `idx_<table>_<columns>`
- **OR transitions**: `from_any_of` generates mapping tables with CHECK constraints
- **Constraint ordering**: Create UNIQUE constraints before foreign keys referencing them

### Repository Pattern

- Methods return `Try<T>` not exceptions
- In-memory test instances: `H2SdrRepository.createInMemory("test-" + System.nanoTime())`
- Default path: `~/.sdd-modeler/repository`

## Testing

### CLI Testing with Picocli

**Black-box** (preferred):
```java
var command = new ShowCommand();
command.repositoryMixin = createMixin();
var out = new StringWriter();
command.setOutput(new PrintWriter(out, true));
var cmd = new CommandLine(command);
int exitCode = cmd.execute("test-model", "--format=all");
assertEquals(0, exitCode);
assertTrue(out.toString().contains("expected content"));
```

**White-box**: Instantiate command, set fields (`repositoryMixin`, `testRepository`, `llmProvider`), call `command.call()`.

### LLM Testing

Use `MockChatModelProvider` for deterministic behavior without network calls.

### Integration Tests

- PostgreSQL tests use Testcontainers
- Functional DDL tests: `scripts/test-ddl-functional.sh`
- Snapshots in `scripts/examples/` and `state-modeler-app/src/test/resources/examples/`

## Where to Edit Code

| Task | Location |
|------|----------|
| SQL generation | `state-modeler-core/src/main/java/io/statemodeler/sql/postgres/` |
| DSL & validation | `state-modeler-core/src/main/java/io/statemodeler/dsl/` and `.../validation/` |
| CLI commands | `state-modeler-app/src/main/java/io/statemodeler/cli/commands/` |
| LLM/migration | `state-modeler-app/src/main/java/io/statemodeler/migration/` |
| Postgres types | `PostgresTypeValidator` (update when adding new types) |

## LLM Integration

- Uses LangChain4j with Ollama (default) or OpenAI (via `OPENAI_API_KEY`)
- `migrate` command outputs JSON with `confidence`, `migrationScript`, `comments`
- Test outputs in `build/test-migration-output/`

## OpenSpec Workflow

This project uses spec-driven development via `openspec/`:

```bash
openspec list              # Active changes
openspec list --specs      # Existing capabilities
openspec validate [id] --strict
openspec archive <change-id> --yes
```

- **Changes** (`openspec/changes/`): Proposals with `proposal.md`, `tasks.md`, spec deltas
- **Specs** (`openspec/specs/`): Current truth — what IS built
- See `openspec/AGENTS.md` for full workflow

## PR Checklist

1. Run `./gradlew spotlessApply` and `./gradlew test`
2. For DDL changes: update snapshots in `scripts/examples/` and `state-modeler-app/src/test/resources/examples/`
3. If adding Postgres types: update `PostgresTypeValidator` with tests
4. Document breaking changes in `PR_SUMMARY.md`
