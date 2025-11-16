# sdd-modeler Development Guide

## Quick Start

### Prerequisites
- Java 25+ (set up with toolchain, project will handle this automatically)
- No need to install Gradle (using wrapper)

### Build & Test
```bash
# Build the entire project
./gradlew build

# Run tests
./gradlew test

# Format code (Spotless with Palantir Java Format)
./gradlew spotlessApply

# Check formatting
./gradlew spotlessCheck
```

### CLI Usage
```bash
# Show help
./gradlew :state-modeler-cli:run

# Validate a model
./gradlew :state-modeler-cli:run --args="validate instructions/examples/orders-sdd-model.yaml"

# Generate SQL DDL
./gradlew :state-modeler-cli:run --args="sql instructions/examples/orders-sdd-model.yaml"

# Generate SQL to file
./gradlew :state-modeler-cli:run --args="sql instructions/examples/orders-sdd-model.yaml -o output.sql"
```

## Project Structure

```
sdd-modeler/
├── state-modeler-core/          # Core SDD model + SQL generation
│   └── src/main/java/io/statemodeler/
│       ├── core/                # Model classes (SddModel, EntityDef, etc.)
│       ├── dsl/                 # YAML/JSON loading
│       ├── validation/          # Model validation (TODO)
│       └── sql/                 # SQL generation (TODO)
├── state-modeler-cli/           # CLI application
│   └── src/main/java/io/statemodeler/cli/
│       ├── Main.java            # CLI entry point
│       ├── ValidateCommand.java # validate subcommand
│       └── SqlCommand.java      # sql subcommand
└── instructions/examples/       # Reference model & expected DDL
    ├── orders-sdd-model.yaml    # Sample SDD model
    └── orders-sdd-ddl.sql       # Expected SQL output
```

## Current Status & Next Steps

### ✅ Implemented
- [x] Gradle multi-module build with Java 25
- [x] Core model classes (SddModel, EntityDef, StateDef, etc.)
- [x] CLI skeleton with Picocli (validate/sql commands)
- [x] Basic tests with JUnit 5 + AssertJ
- [x] Code formatting with Spotless + Palantir Java Format
- [x] Jackson dependency setup for YAML/JSON parsing

### 🚧 Next Implementation Priorities
1. **YAML Model Loader** (`io.statemodeler.dsl.YamlModelLoader`)
   - Parse `instructions/examples/orders-sdd-model.yaml`
   - Map YAML structure to `SddModel` objects

2. **Model Validation** (`io.statemodeler.validation.ModelValidator`)
   - Validate SDD constraints (initial state, valid transitions, etc.)
   - Implementation guide in `instructions/ARCHITECTURE.md`

3. **SQL Plan Generation** (`io.statemodeler.sql.SqlPlanGenerator`)
   - Transform `SddModel` → abstract `SqlPlan`
   - Handle OR transitions, extensions, projections

4. **PostgreSQL DDL Renderer** (`io.statemodeler.sql.postgres.PostgresDdlRenderer`)
   - Generate DDL matching `instructions/examples/orders-sdd-ddl.sql`

## Development Guidelines

### Code Style
- **Formatting**: Auto-enforced by Spotless with Palantir Java Format
- **Immutability**: All model classes are immutable with final fields
- **Null Safety**: Required fields checked with `Objects.requireNonNull()`
- **Validation**: Use AssertJ for test assertions

### Testing Strategy
- **Unit tests**: Core classes and parsing logic
- **Integration test**: Load `orders-sdd-model.yaml` → validate generated SQL matches expected `orders-sdd-ddl.sql`
- **No mocking**: Direct testing without Mockito

### Key SDD Patterns to Implement
- **OR transitions**: `from_any_of` generates mapping tables with CHECK constraints
- **State references**: Each state table has FK to previous state(s)
- **Extension tables**: 1:1 with state tables for optional/mutable data
- **Projection views**: Complex queries for state intervals and current state

## Dependencies
- **Jackson 2.18.0**: YAML/JSON parsing
- **Picocli 4.7.6**: CLI framework
- **JUnit 5.11.3**: Testing framework
- **AssertJ 3.26.3**: Fluent assertions
- **Gradle 8.11.1**: Build system