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
 
# Generate diagram for specific entity
./gradlew :state-modeler-app:run --args="diagram scripts/examples/orders-sdd-model.yaml --entity order"

./gradlew :state-modeler-app:run --args="diagram scripts/examples/orders-sdd-model.yaml --entity order"
 
# === SDR Repository Management ===

# Register a model in the repository
./gradlew :state-modeler-app:run --args="register scripts/examples/orders-sdd-model.yaml"

./gradlew :state-modeler-app:run --args="register scripts/examples/orders-sdd-model.yaml"
# Register with custom name and version
./gradlew :state-modeler-app:run --args="register model.yaml --name my-model --version 2.0.0"

# Register to custom repository path
./gradlew :state-modeler-app:run --args="register model.yaml --repository /path/to/repo"

# List all registered models (table format)
./gradlew :state-modeler-app:run --args="list"

# List in JSON format
./gradlew :state-modeler-app:run --args="list --format json"

# List in YAML format
./gradlew :state-modeler-app:run --args="list --format yaml"

# List with limit
./gradlew :state-modeler-app:run --args="list --limit 10"

# Show model by hash
./gradlew :state-modeler-app:run --args="show 222fa0d3e1b4c5d6a7f8e9d0c1b2a3f4"

# Show model by name (latest version)
./gradlew :state-modeler-app:run --args="show orders-sdd-example"

# Show model by name and version
./gradlew :state-modeler-app:run --args="show orders-sdd-example:1.0.0"

# Show only metadata
./gradlew :state-modeler-app:run --args="show orders-sdd-example --format metadata"

# Show only schema
./gradlew :state-modeler-app:run --args="show orders-sdd-example --format schema"

# Show only DDL
./gradlew :state-modeler-app:run --args="show orders-sdd-example --format ddl"

# Delete a model (interactive confirmation)
./gradlew :state-modeler-app:run --args="delete 222fa0d3e1b4c5d6a7f8e9d0c1b2a3f4"

# Delete without confirmation
./gradlew :state-modeler-app:run --args="delete 222fa0d3e1b4c5d6a7f8e9d0c1b2a3f4 --yes"

# === DDL Comparison & Migration ===

# Compare DDL between two SDR versions
./gradlew :state-modeler-app:run --args="diff orders:1.0 orders:2.0"

# Generate LLM-powered migration script
./gradlew :state-modeler-app:run --args="migrate orders:1.0 orders:2.0"

# Generate migration with Ollama (requires Ollama server running)
./gradlew :state-modeler-app:run --args="migrate orders:1.0 orders:2.0 --llm ollama --model llama3.2"

# Save migration to file
./gradlew :state-modeler-app:run --args="migrate orders:1.0 orders:2.0 -o migration.sql"

# Force regeneration (skip cached migration)
./gradlew :state-modeler-app:run --args="migrate orders:1.0 orders:2.0 --force"
```

### Runtime Dependencies for Migration Commands

**Important**: The `migrate` command requires LangChain4j dependencies at runtime:

- `dev.langchain4j:langchain4j:0.36.2` (core)
- `dev.langchain4j:langchain4j-ollama:0.36.2` (for Ollama LLM provider)

These dependencies are **already included** in the Gradle build, so `./gradlew run` will work.

However, if you distribute the JAR independently, users must ensure these libraries are on the classpath. If missing, the CLI will show a clear error:

```
ERROR: LangChain4j dependencies not found
  The 'migrate' command requires LangChain4j libraries.
  Please ensure the following dependencies are available:
    - dev.langchain4j:langchain4j:0.36.2
    - dev.langchain4j:langchain4j-ollama:0.36.2
  Missing class: ...
```

Other commands (`validate`, `sql`, `diagram`, `register`, `list`, `show`, `delete`, `diff`) work without LangChain4j dependencies.

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
- **Gradle 8.11.1**: Build system

## SDR Repository Details

### Database Schema

The H2 repository uses a single table:

```sql
CREATE TABLE sdr_records (
    schema_hash VARCHAR(64) PRIMARY KEY,
    model_name VARCHAR(255) NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    schema CLOB NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    ddl CLOB NOT NULL,
    ddl_hash VARCHAR(64) NOT NULL,
    sdr_version VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_name (model_name),
    INDEX idx_name_version (model_name, model_version)
);
```

### Repository Configuration

Repository path resolution follows this cascade:

1. **CLI option**: `--repository /custom/path`
2. **Environment variable**: `SDD_REPOSITORY_PATH`
3. **Config file**: (planned)
4. **Default**: `~/.sdd-modeler/repository`

### SDR Record Structure

```java
record SdrRecord(
    String schema,          // Canonical JSON model
    String contentType,     // "application/yaml" or "application/json"
    String ddl,             // Generated PostgreSQL DDL
    String schemaHash,      // SHA-256 of schema
    String ddlHash,         // SHA-256 of DDL
    String version          // SDR format version
)
```

### Hash Computation

- **Schema hash**: SHA-256 of canonical JSON (format-independent)
- **DDL hash**: SHA-256 of generated DDL
- **Build fingerprint**: `schemaHash + ddlHash + version`

Identical models produce identical hashes regardless of YAML vs JSON input.
