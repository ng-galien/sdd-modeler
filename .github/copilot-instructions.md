# Copilot Instructions for sdd-modeler

## Project Overview
**sdd-modeler** is a Java 21 library + CLI for implementing State-Driven Design (SDD). It generates PostgreSQL DDL from declarative YAML/JSON models describing entities, states, transitions, extensions, and projections.

**Current Implementation Status**: Core model classes (✅), YAML/JSON parsing (✅), model validation with Vavr (✅), SQL plan framework (✅), PostgreSQL DDL generation (🚧 views incomplete). CLI functional but needs full core integration.

## Core SDD Principles (Critical Context)
- **Entities vs States**: Separate stable entity data (`orders` table) from mutable state facts (`order_pending`, `order_paid` tables)
- **States as immutable facts**: Each state is an append-only record with non-null attributes, not status columns
- **Explicit state graphs**: Transitions via `from` (simple) or `from_any_of` (OR transitions → mapping tables like `canceled_source`)
- **Extensions for optionals**: Non-decisional, mutable data in separate extension tables (e.g., `order_paid_extensions`)
- **Derived current state**: No `status` column - derive from projections/views (`current_order_states` filters `end_at IS NULL`)

## Architecture & Module Structure
Multi-module Gradle 8.11.1 project with Java 21 toolchain:
- `state-modeler-core`: Model classes, YAML/JSON parsing (Jackson), validation (Vavr), SQL plan, PostgreSQL DDL
- `state-modeler-cli`: Picocli-based CLI (`validate`, `sql` commands) - integration in progress
- `state-modeler-spring`: Future Java/Spring code generation

### Core Package Structure (Current Implementation)
```
io.statemodeler.core          // ✅ SddModel, EntityDef, StateDef (records w/ null checks)
io.statemodeler.dsl           // ✅ YamlModelLoader, JsonModelLoader (Jackson + Try<T>)
io.statemodeler.dsl.yaml      // ✅ YamlModelDto records for deserialization
io.statemodeler.validation    // ✅ DefaultModelValidator (Vavr Validation<List<ValidationError>, SddModel>)
io.statemodeler.sql           // ✅ SqlPlan, TableDefinition, ViewDefinition, DdlGenerator interface
io.statemodeler.sql.postgres  // 🚧 PostgresDdlGenerator (tables done, views TODO)
io.statemodeler.schema        // ✅ JSON schema generation (victools/jsonschema-generator)
```

### Build & Development Commands
```bash
# Run CLI (validates YAML, generates SQL)
./gradlew :state-modeler-cli:run --args="validate instructions/examples/orders-sdd-model.yaml"
./gradlew :state-modeler-cli:run --args="sql instructions/examples/orders-sdd-model.yaml -o output.sql"

# Code formatting (REQUIRED before commit - CI enforces this)
./gradlew spotlessApply          # Auto-format with Palantir Java Format
./gradlew spotlessCheck          # Verify formatting

# Testing with coverage
./gradlew test                   # Run all tests (JUnit 5 + AssertJ)
./gradlew jacocoTestReport       # Generate coverage reports
./gradlew build                  # Full build (tests + formatting + JAR)
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
3. **OR Transition Mapping**: `from_any_of` creates tables like `canceled_source`:
   - FKs to each source state table (nullable)
   - CHECK constraint: exactly ONE source FK is non-null
   - Referenced by target state's `previous_canceled_source_id`
4. **Extension Tables**: 1:1 with state tables via `<state>_id PK/FK`, optional attributes only
5. **Interval Views**: Complex UNION ALL calculating state timelines with LEAD() for `end_at`
6. **Current State Views**: `SELECT * FROM <entity>_state_intervals WHERE end_at IS NULL`

## Implementation Status & Next Steps
**✅ Completed:**
- Core model records: `SddModel`, `EntityDef`, `StateDef`, etc. with inline null validation
- YAML/JSON parsing: `YamlModelLoader`, `JsonModelLoader` using Jackson + `io.vavr.control.Try<T>`
- Validation: `DefaultModelValidator` returns `Validation<List<ValidationError>, SddModel>`
- SQL plan: `SqlPlan` abstraction (tables, views, constraints)
- PostgreSQL table generation: entity, state, extension, OR transition tables
- CLI framework: Picocli with `validate` and `sql` subcommands
- JSON Schema generation: Using victools/jsonschema-generator (auto-generated during build)

**🚧 In Progress:**
- PostgreSQL view generation for projections (intervals, current_state)
- CLI integration with YamlModelLoader + DefaultModelValidator

**⏳ TODO Priority:**
1. Complete `PostgresDdlGenerator` view rendering (intervals + current_state projections)
2. Wire YamlModelLoader into CLI ValidateCommand
3. Add integration test: `orders-sdd-model.yaml` → DDL matches `orders-sdd-ddl.sql` structure
4. Document validation error codes in `ValidationError` javadoc

## Testing Strategy
- **Unit tests**: JUnit 5 + AssertJ for fluent assertions
- **Test structure**: Given-When-Then pattern (see `SddModelTest.java`)
- **Validation testing**: Check both `isValid()` and `isInvalid()` on Vavr `Validation` results
- **Example-driven**: `orders-sdd-model.yaml` in `src/test/resources/` drives integration tests
- **Coverage**: JaCoCo reports enforced via CI (codecov.io integration)

### Test Examples
```java
// Records with null validation
assertThatThrownBy(() -> new SddModel(null, "test", db, entities))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("version cannot be null");

// Vavr Validation assertions
var result = validator.validate(model);
assertThat(result.isInvalid()).isTrue();
assertThat(result.getError()).hasSize(1);
assertThat(result.getError().get(0).code()).isEqualTo("ENTITY_NO_STATES");

// YAML parsing with Try<T>
var result = yamlLoader.loadFromFile(path);
assertThat(result.isSuccess()).isTrue();
var model = result.get();
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