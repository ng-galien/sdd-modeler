# Copilot Instructions for sdd-modeler

## Project Overview
**sdd-modeler** is a Java 21 library + CLI for implementing State-Driven Design (SDD). It generates PostgreSQL DDL from declarative YAML/JSON models describing entities, states, transitions, extensions, and projections.

**Current Implementation Status**: Core model classes (✅), YAML/JSON parsing (✅), model validation with Vavr (✅), SQL plan framework (✅), PostgreSQL DDL generation (✅ complete with views + automatic FK indexing). CLI fully integrated with validation and SQL generation (✅). JSON Schema generation (✅). **SDR Repository (✅), DDL comparison service (✅), AI-powered migration generation with LangChain4j (✅)**.

## Core SDD Principles (Critical Context)
- **Entities vs States**: Separate stable entity data (`orders` table) from mutable state facts (`order_pending`, `order_paid` tables)
- **States as immutable facts**: Each state is an append-only record with non-null attributes, not status columns
- **Explicit state graphs**: Transitions via `from` (simple) or `from_any_of` (OR transitions → mapping tables like `canceled_source`)
- **Extensions for optionals**: Non-decisional, mutable data in separate extension tables (e.g., `order_paid_extensions`)
- **Derived current state**: No `status` column - derive from projections/views (`current_order_states` filters `end_at IS NULL`)
- **Schema separation**: Entities in `schema` (e.g., `public`), states/extensions/projections in `stateSchema` (e.g., `public_states`)

## Architecture & Module Structure
Multi-module Gradle 8.11.1 project with Java 21 toolchain:
- `state-modeler-core`: Model classes, YAML/JSON parsing (Jackson), validation (Vavr), SQL plan, PostgreSQL DDL
- `state-modeler-app`: Picocli-based CLI (validate, sql, register, list, show, delete, diff, migrate) + SDR repository + migration services
- `state-modeler-spring`: Future Java/Spring code generation

### Core Package Structure (Current Implementation)
```
io.statemodeler.core          // ✅ SddModel, EntityDef, StateDef (records w/ null checks)
io.statemodeler.dsl           // ✅ YamlModelLoader, JsonModelLoader (Jackson + Try<T>)
io.statemodeler.dsl.yaml      // ✅ YamlModelDto records for deserialization
io.statemodeler.validation    // ✅ DefaultModelValidator (Vavr Validation<List<ValidationError>, SddModel>)
io.statemodeler.sql           // ✅ SqlPlan, TableDefinition, ViewDefinition, DdlGenerator interface
io.statemodeler.sql.postgres  // ✅ PostgresDdlGenerator (complete: tables, views, constraints, indexes)
io.statemodeler.schema        // ✅ JSON schema generation (victools/jsonschema-generator)
io.statemodeler.cli           // ✅ ValidateCommand, SqlCommand, DiffCommand, MigrateCommand (Picocli with full integration)
io.statemodeler.repository    // ✅ SdrRepository, H2SdrRepository, SdrMigration (SDR + migration persistence)
io.statemodeler.comparison    // ✅ DdlComparisonService (DDL diff analysis)
io.statemodeler.migration     // ✅ MigrationGenerationService, LangChainMigrationGenerationService (LLM-based migrations)
```

### Build & Development Commands
```bash
# Run CLI (validates YAML, generates SQL)
./gradlew :state-modeler-app:run --args="validate instructions/examples/orders-sdd-model.yaml"
./gradlew :state-modeler-app:run --args="sql instructions/examples/orders-sdd-model.yaml -o output.sql"

# Compare DDL between versions
./gradlew :state-modeler-app:run --args="diff orders:1.0 orders:2.0"

# Generate migration with Ollama (requires Ollama server running)
./gradlew :state-modeler-app:run --args="migrate orders:1.0 orders:2.0 -o migration.sql"

# Code formatting (REQUIRED before commit - CI enforces this)
./gradlew spotlessApply          # Auto-format with Palantir Java Format
./gradlew spotlessCheck          # Verify formatting

# Testing with coverage
./gradlew test                   # Run all tests (JUnit 5 standard assertions)
./gradlew jacocoTestReport       # Generate coverage reports
./gradlew build                  # Full build (tests + formatting + JAR)

# JSON Schema generation (automatic during build)
./gradlew :state-modeler-core:generateJsonSchema  # Manual trigger
```

## Key DSL Format (Reference: `instructions/examples/orders-sdd-model.yaml`)
```yaml
version: "0.1"
name: "orders-sdd-example"
database:
  dialect: postgres
  schema: public        # optional, defaults to 'public'
entities:
  order:
    table: orders
    id:
      name: id          # entity PK attribute
      type: serial
      primary_key: true
    attributes:         # stable entity data (immutable after creation)
      customer_id: { type: int, nullable: false }
      total_amount: { type: "numeric(10,2)", nullable: false }
    states:
      pending:
        initial: true   # exactly ONE per entity
        table: order_pending
        attributes:     # state-specific non-null data
          pending_reason: { type: text, nullable: false }
      paid:
        from: [pending]           # simple transition
        table: order_paid
        attributes:
          payment_method: { type: text, nullable: false }
      cancelled:
        from_any_of: [pending, paid]  # OR transition → creates canceled_source mapping table
        attributes:
          cancel_reason: { type: text, nullable: false }
    extensions:         # optional/mutable non-decisional data
      paid_extensions:
        target_state: paid
        attributes:
          notes: { type: text, nullable: true }
    projections:        # generated views
      state_intervals:
        kind: intervals           # columns: state_name, start_at, end_at (timeline)
      current_state:
        kind: current_state       # filters intervals WHERE end_at IS NULL
```

## Critical SQL Generation Patterns
Follow these patterns from `instructions/examples/orders-sdd-ddl.sql`:

1. **Entity Table**: `orders` with stable attributes + `created_at TIMESTAMPTZ DEFAULT now()`
2. **State Tables**: `order_pending`, `order_paid`, etc. with:
   - `<entity>_id BIGINT NOT NULL REFERENCES <entity_table>(id)`
   - `previous_<state>_id BIGINT` FK to source state(s)
   - State-specific non-null attributes
   - `created_at TIMESTAMPTZ DEFAULT now()`
   - **Automatic FK indexing**: `idx_<table>_<column>` for all FK columns (performance optimization)
3. **OR Transition Mapping**: `from_any_of` creates tables like `canceled_source`:
   - FKs to each source state table (nullable)
   - CHECK constraint: exactly ONE source FK is non-null
   - Referenced by target state's `previous_canceled_source_id`
4. **Extension Tables**: 1:1 with state tables via `<state>_id PK/FK`, optional attributes only
5. **Interval Views**: Complex UNION ALL calculating state timelines with LEAD() for `end_at`
6. **Current State Views**: `SELECT * FROM <entity>_state_intervals WHERE end_at IS NULL`
7. **Schema Separation**: 
   - Entity tables in `schema` (default: `public`)
   - State/extension/OR/projection tables in `stateSchema` (default: `<schema>_states`)
   - Configured via `DatabaseConfig.effectiveStateSchema()` method

## Implementation Status & Next Steps
**✅ Completed:**
- Core model records: `SddModel`, `EntityDef`, `StateDef`, etc. with inline null validation
- YAML/JSON parsing: `YamlModelLoader`, `JsonModelLoader` using Jackson + `io.vavr.control.Try<T>`
- Validation: `DefaultModelValidator` returns `Validation<List<ValidationError>, SddModel>`
- SQL plan: `SqlPlan` abstraction (tables, views, constraints)
- PostgreSQL DDL generation: Complete implementation including entity, state, extension, OR transition tables, and projection views (intervals, current_state)
- CLI integration: `ValidateCommand`, `SqlCommand`, `DiffCommand`, `MigrateCommand` fully integrated
- CLI testing: Comprehensive tests for all commands (valid/invalid models, file I/O, error handling)
- JSON Schema generation: Using victools/jsonschema-generator (auto-generated during build)
- **SDR Repository**: H2-based persistence with in-memory test optimization (~3x faster)
- **DDL Comparison**: Structural diff analysis with DdlComparisonService
- **Migration Generation**: LangChain4j integration (Ollama) with intelligent prompting
- **Migration Persistence**: Cache layer for LLM-generated migrations

**⏳ TODO Priority:**
1. Multi-dialect migration support (MySQL, SQL Server)
2. Migration execution tracking (apply/rollback history)
3. Custom LLM endpoints (OpenAI, Anthropic, Azure)
4. Advanced projection types (aggregations, custom queries)

## Testing Strategy
- **Unit tests**: JUnit 5 standard assertions ONLY (no AssertJ or other assertion libraries)
- **Test structure**: Given-When-Then pattern (see `SddModelTest.java`)
- **Validation testing**: Check both `isValid()` and `isInvalid()` on Vavr `Validation` results
- **Example-driven**: `orders-sdd-model.yaml` in `src/test/resources/` drives integration tests
- **Coverage**: JaCoCo reports enforced via CI (codecov.io integration)

### Test Examples
```java
// Records with null validation using JUnit assertions
IllegalArgumentException exception = assertThrows(
    IllegalArgumentException.class,
    () -> new SddModel(null, "test", db, entities)
);
assertTrue(exception.getMessage().contains("version cannot be null"));

// Vavr Validation assertions with JUnit
var result = validator.validate(model);
assertTrue(result.isInvalid());
assertEquals(1, result.getError().size());
assertEquals("ENTITY_NO_STATES", result.getError().get(0).code());

// YAML parsing with Try<T> using JUnit
var result = yamlLoader.loadFromFile(path);
assertTrue(result.isSuccess());
var model = result.get();
assertNotNull(model);
```

## Key Files for Context
- `instructions/ARCHITECTURE.md`: Detailed SDD principles, module structure, design decisions
- `instructions/examples/orders-sdd-model.yaml`: Complete DSL reference example
- `instructions/examples/orders-sdd-ddl.sql`: Expected PostgreSQL output with extensive comments
- `DEV_README.md`: Quick-start guide for builds, tests, CLI usage
- `README.md`: Project vision, high-level overview

## Code Style Guidelines
**Immutability & Simplicity First:**
- **Records**: Use Java records for simple immutable data classes (DTOs, value objects)
  - Inline null checks in compact constructor: `if (field == null) throw new IllegalArgumentException(...)`
  - Use `Map.copyOf()` for immutable collections
  - Example: `SddModel`, `EntityDef`, `ValidationError`, all DTO records in `dsl.yaml`
- **NO Lombok annotations**: Project uses plain records and classes (no `@Value`, `@Data`, `@Builder`)
- **Null Safety**: Required fields validated with explicit null checks + clear error messages

**Exception Handling (Modern Java + Vavr):**
- **Runtime exceptions ONLY**: No custom checked exceptions
  - `IllegalArgumentException`: Invalid input, malformed data, parsing errors
  - `IllegalStateException`: Object in invalid state for operation
  - `IOException`: File I/O operations only (wrapped by loaders)
- **Vavr functional types** for controlled error handling:
  - `io.vavr.control.Try<T>`: File parsing operations (see `YamlModelLoader.loadFromFile()`)
  - `io.vavr.control.Validation<List<ValidationError>, T>`: Accumulating multiple validation errors
  - Return type pattern: `Try<SddModel>` for I/O, `Validation<List<ValidationError>, SddModel>` for business rules
- **Java collections**: Use standard `java.util.List/Set/Map` (NOT Vavr collections)
- **Clear error messages**: Always include context in exception messages

**When to use what:**
- **Records**: All DTOs, model classes, value objects (default choice)
- **Final classes**: Utility classes (private constructor), generators, validators
- **Try<T>**: I/O operations that can fail (file reading, network calls)
- **Validation<E, T>**: Business rule validation accumulating multiple errors

**Exception Anti-patterns (avoid):**
- ❌ Custom exception classes (use runtime exceptions with descriptive messages)
- ❌ Checked exceptions for business logic (use Validation instead)
- ❌ Silent failures or null returns (fail fast with exceptions or use Try/Validation)

## SDD Validation Constraints
When implementing or extending validation (`DefaultModelValidator`), enforce these critical rules:
- Each entity has ≥1 state with **exactly one** `initial: true`
- All transition references (`from`, `from_any_of`) point to existing states within the same entity
- `from_any_of` transitions have ≥2 source states (otherwise use simple `from`)
- Extension `target_state` references exist and are valid state names
- Projection `kind` is one of: `intervals`, `current_state`
- No circular state transitions (initial state cannot be transition target)
- State names and entity names are non-empty, valid identifiers

**Validation Error Format**: See `ValidationError` record with `code`, `message`, optional `entityName`, `stateName`

## External Libraries Policy
**CRITICAL:** When working with external libraries you're unfamiliar with:
1. **DO NOT** code based on assumptions or incomplete knowledge
2. **ALWAYS** ask where to find official documentation first
3. **READ** the documentation to understand proper API usage
4. **FOLLOW** documented examples and patterns exactly
5. Only then implement the feature using the documented approach

Example: "I need to use victools/jsonschema-generator but I'm not familiar with the API. Where can I find the documentation?"

## SQL Generation Architecture
- **Separation of concerns**: `SqlPlan` (abstract) → `DdlGenerator` (dialect-specific rendering)
- **Factory pattern**: `DdlGenerators.forDialect("postgres")` returns appropriate generator
- **Future support**: Keep PostgreSQL-specific syntax isolated in `sql.postgres` package
- **View generation priority**: Interval views use `LEAD()` window function for calculating `end_at`

## GitHub Pull Request Review Workflow

When reviewing or responding to PR feedback, follow this process:

### 1. Reading Pull Request Details and Comments

Use the **MCP GitHub tools** to fetch PR information programmatically:

```bash
# Get PR details (status, commits, file changes, stats)
mcp_github_pull_request_read(method="get", owner="ng-galien", repo="sdd-modeler", pullNumber=8)

# Get conversation comments (includes Codecov bot reports)
mcp_github_pull_request_read(method="get_comments", owner="ng-galien", repo="sdd-modeler", pullNumber=8)

# Get review comments (inline code suggestions from reviewers)
mcp_github_pull_request_read(method="get_review_comments", owner="ng-galien", repo="sdd-modeler", pullNumber=8)
```

**Key information to extract:**
- **Codecov report**: Look for `codecov-commenter` in comments - shows coverage percentage, files with missing lines
- **Review comments**: Copilot PR Reviewer or human reviewers provide inline suggestions with file paths, line numbers, and specific code improvements
- **PR stats**: `additions`, `deletions`, `changed_files`, `commits` count
- **Merge status**: `mergeable`, `mergeable_state`, `merged`

### 2. Analyzing Feedback and Creating Action Plan

After reading PR comments, create a structured action plan:

**A. Categorize feedback by type:**
- **Coverage issues**: Identify files with low coverage (< 80%) from Codecov report
- **Code quality**: Review suggestions from Copilot/reviewers (e.g., pattern improvements, edge cases)
- **Documentation**: Check if new features need documentation updates

**B. Prioritize actions:**
1. **Critical**: Bugs, security issues, breaking changes
2. **High**: Coverage gaps in new code, reviewer suggestions on core logic
3. **Medium**: Code quality improvements, pattern enhancements
4. **Low**: Style suggestions, documentation tweaks

**C. Document the plan** (use `manage_todo_list` or create checklist in response):

Example plan structure:
```markdown
## PR #8 Review Response Plan

### Coverage Improvements (Priority: High)
- [ ] Add tests for `DefaultModelValidator.validateAttributeTypes()` edge cases (12 missing lines)
- [ ] Add tests for `IndexDefinition` null checks (4 partial lines)

### Copilot Suggestions (Priority: High)
- [ ] Fix NUMERIC pattern to support `NUMERIC(p)` (PostgresTypeValidator.java:59)
- [ ] Implement recursive array validation (PostgresTypeValidator.java:55)
- [ ] Enhance TIMESTAMP precision pattern (PostgresTypeValidator.java:63)

### Testing
- [ ] Add tests for new type validation cases (NUMERIC(10), VARCHAR(255)[], TIMESTAMP WITH TIME ZONE(6))
- [ ] Verify coverage increase with `./gradlew test jacocoTestReport`
```

### 3. Implementation Guidelines

When implementing PR feedback:

- **One commit per logical change group**: Don't mix coverage improvements with refactoring
- **Reference review comments**: Use commit messages like `fix: support NUMERIC(p) format (addresses PR #8 review)`
- **Add tests FIRST**: For coverage issues, write failing tests before fixing code
- **Verify before pushing**: Always run `./gradlew test jacocoTestReport` and check coverage locally
- **Update PR description**: If changes are significant, add a comment summarizing what was addressed

### 4. Common Codecov Patterns

**Partial coverage (yellow lines):**
- Usually lambda expressions, ternary operators, or multi-branch conditions
- Add tests exercising both branches

**Missing coverage (red lines):**
- Untested code paths (error handling, edge cases)
- Add dedicated test methods for each case

**Example: Addressing "12 missing lines in DefaultModelValidator":**
```java
// Add tests for each attribute type validation path
@Test
void shouldValidateEntityAttributeTypes() { /* test entity attrs */ }

@Test
void shouldValidateStateAttributeTypes() { /* test state attrs */ }

@Test
void shouldValidateExtensionAttributeTypes() { /* test extension attrs */ }
```

### 5. Post-Implementation Checklist

Before marking PR as ready:
- [ ] All review comments addressed or discussed
- [ ] Coverage increased (check Codecov report in new commit)
- [ ] All tests passing (`./gradlew test`)
- [ ] Code formatted (`./gradlew spotlessApply`)
- [ ] Commits have clear messages referencing PR feedback
- [ ] PR description updated if scope changed