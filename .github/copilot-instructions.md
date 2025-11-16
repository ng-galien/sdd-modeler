# Copilot Instructions for sdd-modeler

## Project Overview
**sdd-modeler** is a Java 21 library + CLI for implementing State-Driven Design (SDD). It generates PostgreSQL DDL from declarative YAML/JSON models describing entities, states, transitions, extensions, and projections.

**Current Implementation Status**: Core model classes and SQL generation framework are implemented. YAML/JSON parsing and model validation are partially complete. CLI framework is functional but needs integration with core features.

## Core SDD Principles (Critical Context)
- **Entities vs States**: Separate stable entity data (`orders` table) from mutable state facts (`order_pending`, `order_paid` tables)
- **States as immutable facts**: Each state is an append-only record with non-null attributes, not status columns
- **Explicit state graphs**: Transitions defined via `from` (simple) or `from_any_of` (OR transitions requiring mapping tables like `canceled_source`)
- **Extensions for optionals**: Non-decisional, mutable data goes in separate extension tables (e.g., `order_paid_extensions`)
- **Derived current state**: No `status` column - current state derived from projections/views (`current_order_states`)

## Architecture & Module Structure
Multi-module Gradle project with Java 21:
- `state-modeler-core`: ✅ SDD model classes, SQL generation framework, partial YAML/JSON parsing
- `state-modeler-cli`: ✅ CLI framework with Picocli (validate/sql commands) - needs core integration
- `state-modeler-spring`: Future Java/Spring code generation

### Core Package Structure (Current Implementation)
```
io.statemodeler.core     // ✅ SddModel, EntityDef, StateDef, etc. (records with validation)
io.statemodeler.dsl      // 🚧 ModelLoader interfaces, YamlModelLoader skeleton
io.statemodeler.validation // ⏳ Package structure only
io.statemodeler.sql      // ✅ SqlPlan, TableDefinition, ViewDefinition, DdlGenerator
io.statemodeler.sql.postgres // ✅ PostgresDdlGenerator (partial implementation)
io.statemodeler.schema   // ✅ JSON schema generation support
```

### Build & Development Commands
```bash
# Run CLI during development
./gradlew :state-modeler-cli:run --args="validate instructions/examples/orders-sdd-model.yaml"
./gradlew :state-modeler-cli:run --args="sql instructions/examples/orders-sdd-model.yaml"

# Code formatting (required before commit)
./gradlew spotlessApply

# Run tests with coverage
./gradlew test jacocoTestReport
```

## Key DSL Format (Reference: `instructions/examples/orders-sdd-model.yaml`)
```yaml
entities:
  order:
    table: orders
    attributes: # stable, non-state data
    states:
      pending:
        initial: true  # required for each entity
        from: [other_state]  # simple transitions
        from_any_of: [state1, state2]  # OR transitions (needs mapping table)
        attributes: # state-specific, non-null data
      # ... other states
    extensions: # optional, mutable, non-decisional
      paid_extensions:
        target_state: paid
    projections: # generated views
      state_intervals:
        kind: intervals  # start_at/end_at timeline
      current_state:
        kind: current_state  # active states only
```

## Critical SQL Generation Patterns
When implementing DDL generation, follow these patterns from `instructions/examples/orders-sdd-ddl.sql`:

1. **OR Transitions**: `from_any_of` creates mapping tables (e.g., `canceled_source`) with CHECK constraints ensuring exactly one source reference
2. **State References**: Each state table has `previous_*_id` FK to its source state(s)
3. **Extension Tables**: 1:1 with state tables, optional/mutable attributes only
4. **Interval Views**: Complex UNION ALL queries calculating state start/end times
5. **Current State Views**: Filter intervals where `end_at IS NULL`

## Implementation Status & Next Steps
**✅ Completed:**
- Core model classes with immutable records (`SddModel`, `EntityDef`, `StateDef`, etc.)
- SQL plan framework (`SqlPlan`, `TableDefinition`, `ViewDefinition`)
- PostgreSQL DDL generator skeleton (`DdlGenerators.forDialect()`)
- CLI framework with Picocli subcommands (`validate`, `sql`)
- Code formatting with Spotless + Palantir Java Format

**🚧 In Progress:**
- YAML/JSON model parsing (classes exist, deserialization needs work)
- PostgreSQL DDL generation (views need implementation)

**⏳ TODO Priority:**
1. Complete `YamlModelLoader` - parse `orders-sdd-model.yaml` to `SddModel`
2. Implement model validation (`ModelValidator` in `validation` package)
3. Complete PostgreSQL DDL generation for projections/views
4. Integrate model loading into CLI commands

## External Libraries Policy
**CRITICAL:** When working with external libraries that you don't know well:
1. **DO NOT** code manually based on assumptions
2. **ALWAYS** ask where to find the official documentation first
3. **READ** the documentation to understand the proper API usage
4. **FOLLOW** the documented examples and patterns exactly
5. Only then implement the feature using the documented approach

Example: "I need to use victools/jsonschema-generator but I'm not familiar with the API. Where can I find the documentation?"

## Testing Strategy
- **Core**: Unit tests for YAML parsing, validation rules, SQL generation
- **Integration**: Given `orders-sdd-model.yaml`, generated DDL should be structurally equivalent to `orders-sdd-ddl.sql`
- **CLI**: End-to-end command testing with sample models

## Key Files for Context
- `instructions/ARCHITECTURE.md`: Detailed technical architecture and design decisions
- `instructions/examples/orders-sdd-model.yaml`: Reference DSL format
- `instructions/examples/orders-sdd-ddl.sql`: Expected SQL output with detailed comments
- `README.md`: Project vision and current state

## Code Generation Priorities
Focus on PostgreSQL dialect first. Maintain clear separation between abstract SQL planning (`SqlPlan`) and dialect-specific rendering (`PostgresDdlRenderer`) for future database support.

When implementing validation, enforce SDD constraints:
- Each entity has ≥1 state with exactly one `initial: true`
- All transition references point to existing states
- `from_any_of` transitions are structurally valid
- Extension target_state references exist

## Code Style Guidelines
**Immutability & Simplicity First:**
- **Records**: Use Java records for simple immutable data classes (DTOs, value objects)
- **Lombok**: Use `@Value` for complex immutable classes with validation/business logic
- **No traditional POJOs**: Avoid manual getters/setters/equals/hashCode/toString
- **Null Safety**: Required fields checked with `Objects.requireNonNull()` or `@NonNull`

**Exception Handling (Modern Java + Vavr):**
- **Runtime exceptions**: Prefer `IllegalArgumentException`, `IllegalStateException` for validation/programming errors
- **No custom exceptions**: Use runtime exceptions with clear messages instead of custom checked exceptions
- **Vavr Validation**: Use `io.vavr.control.Validation<Error, Success>` for collecting multiple validation errors
- **Vavr Either**: Use `io.vavr.control.Either<Error, Success>` for single error/success operations  
- **Java collections**: Use standard Java `List`, `Set`, `Map` collections (NOT Vavr collections)
- **Keep IOException**: Only for genuine I/O operations where recovery is possible
- **Fail fast**: Validate inputs early with clear runtime exceptions

**Validation Strategy:**
- **Vavr Validation ONLY**: Use `io.vavr.control.Validation` to accumulate validation errors
- **Java collections everywhere else**: `java.util.List`, `java.util.Set`, `java.util.Map` for all data structures
- **ValidationError records**: Immutable error descriptions with context using Java collections
- **Either for single operations**: File parsing, database operations (but prefer exceptions)
- **Validation for business rules**: Model structure, SDD constraints, referential integrity

**When to use what:**
- **Records**: Simple data containers, API DTOs, test fixtures
- **@Value classes**: Domain models with validation, computed properties, or Jackson annotations
- **@Builder**: For classes with many optional parameters
- **@Data**: Avoid - prefer immutable alternatives

**Exception Guidelines:**
- **IllegalArgumentException**: Invalid input parameters, malformed data, parsing errors
- **IllegalStateException**: Object in wrong state for operation
- **IOException**: File/network operations only
- **RuntimeException**: Custom runtime exceptions if needed (rare)