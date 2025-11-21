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
./gradlew :state-modeler-app:run

# Validate a model
./gradlew :state-modeler-app:run --args="validate scripts/examples/orders-sdd-model.yaml"

./gradlew :state-modeler-app:run --args="validate scripts/examples/orders-sdd-model.yaml"
 
# Generate SQL DDL
./gradlew :state-modeler-app:run --args="sql scripts/examples/orders-sdd-model.yaml"

./gradlew :state-modeler-app:run --args="sql scripts/examples/orders-sdd-model.yaml"
 
# Generate SQL to file
./gradlew :state-modeler-app:run --args="sql scripts/examples/orders-sdd-model.yaml -o output.sql"

./gradlew :state-modeler-app:run --args="sql scripts/examples/orders-sdd-model.yaml -o output.sql"
 
# Generate Mermaid state diagram
./gradlew :state-modeler-app:run --args="diagram scripts/examples/orders-sdd-model.yaml"

./gradlew :state-modeler-app:run --args="diagram scripts/examples/orders-sdd-model.yaml"
 
# Generate diagram to file
./gradlew :state-modeler-app:run --args="diagram scripts/examples/orders-sdd-model.yaml -o diagram.mmd"

./gradlew :state-modeler-app:run --args="diagram scripts/examples/orders-sdd-model.yaml -o diagram.mmd"
```

```bash
# Generate diagram for specific entity
./gradlew :state-modeler-app:run --args="diagram scripts/examples/orders-sdd-model.yaml --entity order"

./gradlew :state-modeler-app:run --args="diagram scripts/examples/orders-sdd-model.yaml --entity order"
 
### Native image (GraalVM)

To build a native executable for the CLI with GraalVM, follow these steps (developer machine must have GraalVM + native-image installed):

```bash
# Instrument runs to collect reflection / resource metadata
./scripts/generate-native-config.sh

# Build the native image (Gradle nativeCompile task provided by the plugin)
./gradlew :state-modeler-app:nativeCompile

# Smoke test the native binary
./state-modeler-app/build/native/nativeCompile/sdd-modeler --help
./state-modeler-app/build/native/nativeCompile/sdd-modeler validate scripts/examples/orders-sdd-mini-model.yaml
```

Notes:

- The repository contains skeleton `META-INF/native-image` config files; the script `scripts/generate-native-config.sh` will collect data via `native-image-agent` and copy results into `src/main/resources/META-INF/native-image/` for further refinement.

- The `migrate` command (LLM integration) relies on external libs and network; consider excluding it from the native build or running it via the JVM distribution if integration proves complex for the first pass.

## Installing GraalVM & native-image (macOS / Linux)

On macOS using Homebrew (preferred for a system-wide install):

```bash
brew tap graalvm/tap
brew install --cask graalvm/tap/graalvm-ce-java21
export JAVA_HOME="/Library/Java/JavaVirtualMachines/graalvm-ce-java21.jdk/Contents/Home"
${JAVA_HOME}/bin/gu install native-image
```

If you prefer SDKMAN (cross-platform):

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install 24.0.1-graalce
sdk use java 24.0.1-graalce
gu install native-image
```

After installing GraalVM, verify `native-image` is available:

```bash
native-image --version
```

If `native-image` reports a version number, you can re-run:

```bash
./scripts/generate-native-config.sh
./gradlew :state-modeler-app:nativeCompile
```

## SDR Repository Management
 
 See [SDR Repository Guide](REPOSITORY.md) for detailed instructions on managing the repository.
 
 ## DDL Comparison & Migration
 
 See [CLI Documentation](state-modeler-app/README.md#migration--diff) for migration commands.

## Project Structure

```text
sdd-modeler/
├── state-modeler-core/          # Core SDD model + SQL/Diagram generation
│   └── src/main/java/io/statemodeler/
│       ├── core/                # Model classes (SddModel, EntityDef, etc.)
│       ├── dsl/                 # YAML/JSON loading
│       ├── validation/          # Model validation
│       ├── sql/                 # SQL generation (PostgreSQL DDL)
│       ├── diagram/             # Diagram generation (Mermaid, PlantUML)
│       ├── sdr/                 # SDR (State Definition Record) factory
│       └── schema/              # JSON Schema generation
├── state-modeler-app/           # CLI application with repository
│   └── src/main/java/io/statemodeler/
│       ├── cli/                 # CLI commands
│       │   ├── Main.java        # CLI entry point
│       │   ├── ValidateCommand.java   # validate subcommand
│       │   ├── SqlCommand.java        # sql subcommand
│       │   ├── DiagramCommand.java    # diagram subcommand
│       │   ├── RegisterCommand.java   # register subcommand
│       │   ├── ListCommand.java       # list subcommand
│       │   ├── ShowCommand.java       # show subcommand
│       │   └── DeleteCommand.java     # delete subcommand
│       └── repository/          # H2-based SDR repository
│           ├── SdrRepository.java     # Repository interface
│           ├── H2SdrRepository.java   # H2 implementation
│           ├── SdrMetadata.java       # Metadata record
│           ├── RepositoryConfig.java  # Path resolution
│           └── RepositoryMixin.java   # Picocli mixin
└── scripts/examples/       # Reference model & expected outputs used by scripts/tests
    ├── orders-sdd-model.yaml    # Sample SDD model
    ├── orders-sdd-ddl.sql       # Expected SQL output
    └── orders-sdd-diagram.mmd   # Mermaid state diagram
```

## Current Status & Next Steps

### ✅ Implemented

#### Core Features
- [x] Gradle multi-module build with Java 21
- [x] Core model classes (SddModel, EntityDef, StateDef, etc.)
- [x] YAML/JSON model loading with Jackson
- [x] Model validation with Vavr (functional error accumulation)
- [x] PostgreSQL DDL generation (tables, views, constraints, indexes)
- [x] Mermaid diagram generation
- [x] PostgreSQL type validation (NUMERIC, TIMESTAMP, arrays, etc.)
- [x] JSON Schema generation for IDE support
- [x] Code formatting with Spotless + Palantir Java Format

#### CLI & Repository
- [x] Picocli-based CLI (validate, sql, diagram commands)
- [x] **H2-based SDR Repository** (embedded, file-based at `~/.sdd-modeler/repository`)
- [x] **RegisterCommand**: Save models to repository with name/version
- [x] **ListCommand**: List models (table/JSON/YAML formats, --limit)
- [x] **ShowCommand**: Display model details (hash/name/name:version lookup)
- [x] **DeleteCommand**: Remove models with interactive confirmation
- [x] Cryptographic hashing (SHA-256 for schema + DDL integrity)
- [x] RepositoryConfig with cascade resolution (CLI → env → config → default)

#### Testing
- [x] 249+ tests with JUnit 5
- [x] 87%+ instruction coverage, 70%+ branch coverage
- [x] PostgreSQL integration tests with Testcontainers
- [x] Comprehensive CLI command tests

### 🚧 Next Implementation Priorities

1. **Model Comparison Service** (`io.statemodeler.comparison.ComparisonService`)
   - JSON schema diff using zjsonpatch
   - SQL DDL diff using java-diff-utils
   - Side-by-side comparison reports

2. **Migration Generator** (`io.statemodeler.migration.MigrationGenerator`)
   - Analyze schema differences
   - Generate ALTER TABLE scripts
   - Safe migration strategies

3. **Java/Spring Code Generation** (`state-modeler-spring` module)
   - Entity classes from SDD models
   - Repository interfaces
   - Service layer scaffolding

## Development Guidelines

### Code Style

- **Immutability**: Use Java records and Lombok `@Value` for immutable classes
- **Simplicity**: Avoid traditional POJOs - prefer records or Lombok annotations
- **Records**: For simple data containers, DTOs, value objects
- **Lombok `@Value`**: For domain models with validation or business logic
- **Lombok `@Builder`**: For classes with many optional parameters
- **Formatting**: Auto-enforced by Spotless with Palantir Java Format
- **Null Safety**: Required fields checked with `@NonNull` or `Objects.requireNonNull()`
- **Modern Exceptions**: Prefer runtime exceptions (`IllegalArgumentException`, `IllegalStateException`) over checked exceptions
- **Exception Strategy**: Use `IOException` only for genuine I/O operations, runtime exceptions for validation/parsing errors

### Logging

- Use SLF4J (with Logback) for all informational, warning and error messages that are intended for logs or diagnostic output. This ensures proper routing to STDOUT/STDERR depending on the Logback configuration.
- Use System.out.println/System.out.print only for primary CLI output that represents content produced by the command (DDL, diagram text, JSON/YAML output, migration SQL, or interactive prompts). This keeps program output stable for piping and consumption by other tools.
- Avoid using System.err directly in application code; prefer logger.error for error conditions. System.err should only be used if you intentionally want to bypass logging and target the error stream directly.

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

### Core Module
- **Jackson 2.18.0**: YAML/JSON parsing
- **Vavr 0.10.7**: Functional validation (Try, Validation)
- **victools/jsonschema-generator**: JSON Schema generation
- **zjsonpatch 0.4.16**: JSON diff for comparison (planned)
- **java-diff-utils 4.12**: SQL diff for comparison (planned)

### App Module
- **Picocli 4.7.6**: CLI framework
- **H2 Database 2.2.224**: Embedded repository storage
- **Testcontainers 1.20.4**: PostgreSQL integration tests

### Testing
- **JUnit 5.11.3**: Testing framework
- **JaCoCo 0.8.12**: Code coverage
- **Gradle 8.14.3**: Build system
 
 ## See Also
 
 - [Main Documentation](README.md)
 - [Core Library Documentation](state-modeler-core/README.md)
 - [CLI Documentation](state-modeler-app/README.md)
 - [Gradle Plugin Documentation](state-modeler-gradle-plugin/README.md)
 - [Architecture Guide](instructions/ARCHITECTURE.md)
 - [Examples](scripts/examples/)
