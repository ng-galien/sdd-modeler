# sdd-modeler

[![CI](https://github.com/ng-galien/sdd-modeler/actions/workflows/ci.yml/badge.svg)](https://github.com/ng-galien/sdd-modeler/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/ng-galien/sdd-modeler/graph/badge.svg)](https://codecov.io/gh/ng-galien/sdd-modeler)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-21%2B-blue.svg)](https://openjdk.java.net/)
[![Gradle](https://img.shields.io/badge/Gradle-8.11.1-blue.svg)](https://gradle.org/)

A Java library and CLI tool for implementing State-Driven Design (SDD) with automatic SQL schema generation.

**SDD-Modeler** enables you to define your domain model as a declarative YAML/JSON schema describing entities, states, transitions, extensions, and projections. From this single source of truth, it generates production-ready PostgreSQL DDL with optimized state tracking patterns.

## 🚀 Key Features

### ✅ Declarative State Modeling

- Define entities with immutable state transitions in YAML/JSON
- Explicit state graphs with simple (`from`) and OR transitions (`from_any_of`)
- Separation of stable entity data from mutable state facts
- Support for optional, non-decisional data via extensions
- Derived projections (state intervals, current state views)

### ✅ Production-Ready SQL Generation

- **Complete PostgreSQL DDL**: Entity tables, state tables, extension tables, projection views
- **Automatic foreign key indexing**: Performance-optimized with `idx_<table>_<column>` naming convention
- **Type validation**: Comprehensive PostgreSQL type checking (NUMERIC, TIMESTAMP, arrays, etc.)
- **OR transition handling**: Automatic mapping tables for `from_any_of` transitions
- **Schema separation**: Configurable entity vs. state schema isolation

### ✅ Robust Validation

- State graph coherence (initial state, valid transitions, no cycles)
- Attribute type validation for PostgreSQL compatibility
- Extension and projection reference validation
- Clear, actionable error messages

### ✅ Developer-Friendly CLI

```bash
# Validate your model
./state-modeler validate model.yaml

# Generate PostgreSQL DDL
./state-modeler sql model.yaml --output schema.sql

# Register model in local repository
./state-modeler register model.yaml --name my-model --version 1.0.0

# List registered models
./state-modeler list --format table

# Show model details
./state-modeler show my-model:1.0.0

# Delete a model
./state-modeler delete <hash>

# Compare DDL between two versions
./state-modeler diff my-model:1.0 my-model:2.0

# Generate migration script using AI
./state-modeler migrate my-model:1.0 my-model:2.0 --output migration.sql
```

### ✅ SDR Repository Management

- **State Definition Records (SDR)**: Immutable snapshots of your models with cryptographic hashes
- **Local H2 database**: Automatic persistence in `~/.sdd-modeler/repository`
- **Version tracking**: Compare models across versions
- **Hash-based integrity**: SHA-256 ensures model consistency
- **Multiple output formats**: Table, JSON, or YAML for listing models

### ✅ AI-Powered Migration Generation

- **LLM-based migration scripts**: Automatic SQL migration generation using LangChain4j
- **DDL comparison service**: Structural diff analysis between model versions
- **Migration caching**: Persisted migrations to avoid regeneration costs
- **Multiple LLM providers**: Support for Jlama (in-process) and Ollama (server-based)
- **Intelligent prompts**: PostgreSQL-specific patterns with safety guidelines

## 📖 Example: E-Commerce Order Model

### YAML Definition

```yaml
version: "0.1"
name: "orders-sdd-example"
database:
  dialect: postgres
  schema: public
entities:
  order:
    table: orders
    id: { name: id, type: serial, primary_key: true }
    attributes:
      customer_id: { type: int, nullable: false }
      total_amount: { type: "numeric(10,2)", nullable: false }
    states:
      pending:
        initial: true
        table: order_pending
        attributes:
          pending_reason: { type: text, nullable: false }
      paid:
        from: [pending]
        table: order_paid
        attributes:
          payment_method: { type: text, nullable: false }
      cancelled:
        from_any_of: [pending, paid]  # OR transition
        attributes:
          cancel_reason: { type: text, nullable: false }
    extensions:
      paid_extensions:
        target_state: paid
        attributes:
          notes: { type: text, nullable: true }
    projections:
      state_intervals:
        kind: intervals  # Timeline of state changes
      current_state:
        kind: current_state  # Active state only
```

### Generated PostgreSQL DDL

```sql
-- Entity table (stable data)
CREATE TABLE public.orders (
    id SERIAL PRIMARY KEY,
    customer_id INT NOT NULL,
    total_amount NUMERIC(10,2) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- State tables (immutable state facts)
CREATE TABLE public_states.order_pending (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES public.orders(id),
    pending_reason TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_order_pending_order_id ON public_states.order_pending(order_id);

-- OR transition mapping table
CREATE TABLE public_states.canceled_source (
    id BIGSERIAL PRIMARY KEY,
    previous_pending_id BIGINT REFERENCES public_states.order_pending(id),
    previous_paid_id BIGINT REFERENCES public_states.order_paid(id),
    CHECK (
        (previous_pending_id IS NOT NULL AND previous_paid_id IS NULL) OR
        (previous_pending_id IS NULL AND previous_paid_id IS NOT NULL)
    )
);

-- Projection views
CREATE VIEW public_states.order_state_intervals AS
  -- Complex UNION ALL with LEAD() for state timeline
  ...

CREATE VIEW public_states.current_order_states AS
  SELECT * FROM public_states.order_state_intervals WHERE end_at IS NULL;
```

See `instructions/examples/` for complete examples.

## 🏗️ Architecture

### Multi-Module Structure

- **`state-modeler-core`**: Model classes, YAML/JSON parsing, validation, SQL generation, SDR factory
- **`state-modeler-app`**: Picocli-based CLI with repository management (register, list, show, delete)
- **`state-modeler-spring`** *(planned)*: Java/Spring code generation

### Core Principles

- **Immutable state facts**: No UPDATE on state tables - only INSERT (audit trail)
- **No status columns**: Derive current state from projections/views
- **Separation of concerns**: Entity data vs. state data in different schemas
- **Type safety**: Compile-time validation with Java 21 records

### Technology Stack

- **Language**: Java 21 (records, pattern matching, sealed types)
- **Build**: Gradle 8.11.1 (multi-module)
- **Parsing**: Jackson (YAML/JSON)
- **Validation**: Vavr (functional error accumulation)
- **CLI**: Picocli
- **Repository**: H2 Database 2.2.224 (embedded, in-memory for tests)
- **AI Integration**: LangChain4j 0.36.2 (Jlama, Ollama)
- **Hashing**: SHA-256 for cryptographic integrity
- **Testing**: JUnit 5 (196 tests, ~87% coverage)

## 📦 Installation & Usage

### Prerequisites

- Java 21+
- Gradle 8.11+ (or use included wrapper)

### Build from Source

```bash
git clone https://github.com/ng-galien/sdd-modeler.git
cd sdd-modeler
./gradlew build
```

### Run CLI

```bash
# Validate a model
./gradlew :state-modeler-app:run --args="validate examples/orders-sdd-model.yaml"

# Generate SQL
./gradlew :state-modeler-app:run --args="sql examples/orders-sdd-model.yaml -o output.sql"

# Register model in repository
./gradlew :state-modeler-app:run --args="register examples/orders-sdd-model.yaml"

# List all registered models
./gradlew :state-modeler-app:run --args="list --format table"

# Show model details by name
./gradlew :state-modeler-app:run --args="show orders-sdd-example"

# Show model details by hash
./gradlew :state-modeler-app:run --args="show 222fa0d3..."

# Delete a model (interactive confirmation)
./gradlew :state-modeler-app:run --args="delete 222fa0d3..."

# Delete without confirmation
./gradlew :state-modeler-app:run --args="delete 222fa0d3... --yes"

# Compare DDL between two versions
./gradlew :state-modeler-app:run --args="diff orders:1.0 orders:2.0"

# Generate migration with Jlama (downloads model on first use)
./gradlew :state-modeler-app:run --args="migrate orders:1.0 orders:2.0"

# Use Ollama with custom model
./gradlew :state-modeler-app:run --args="migrate orders:1.0 orders:2.0 --llm ollama --model llama3.2"

# Force regeneration and save to file
./gradlew :state-modeler-app:run --args="migrate orders:1.0 orders:2.0 --force -o migration.sql"
```

### Run Tests

```bash
./gradlew test                # Run all tests
./gradlew jacocoTestReport    # Generate coverage reports
```

## 🗺️ Roadmap

### ✅ Completed (v0.1)

- [x] Core model classes with null safety
- [x] YAML/JSON parsing and validation
- [x] PostgreSQL DDL generation (tables, views, constraints)
- [x] Automatic foreign key indexing
- [x] PostgreSQL type validation
- [x] CLI integration (validate, sql, register, list, show, delete)
- [x] **SDR Repository with H2 database**
- [x] **Repository CLI commands** (register, list, show, delete)
- [x] **Cryptographic hashing** (SHA-256 for schema + DDL)
- [x] **Version tracking and comparison**
- [x] **DDL comparison service** (diff command)
- [x] **AI-powered migration generation** (migrate command with LangChain4j)
- [x] **Migration persistence layer** (cache LLM-generated scripts)
- [x] Comprehensive test suite (196 tests, ~87% coverage)

### 🚧 In Progress

- [ ] Trigger generation for automatic state transitions
- [ ] PL/pgSQL functions for business rule validation

### 🔮 Planned

- [ ] **Migration execution tracking** (apply/rollback history)
- [ ] **Multi-dialect migrations** (MySQL, SQL Server)
- [ ] **Custom LLM endpoints** (OpenAI, Anthropic, Azure)
- [ ] Java/Spring code generation
- [ ] MySQL/MariaDB support
- [ ] Advanced projections (aggregations, custom queries)
- [ ] Interactive CLI mode for model creation
- [ ] Remote repository synchronization

## 📚 Documentation

- **[Architecture Guide](instructions/ARCHITECTURE.md)**: Detailed design principles and package structure
- **[Examples](instructions/examples/)**: Complete working examples with explanations
- **[Development Guide](DEV_README.md)**: Build commands, coding standards, contribution guidelines
- **[JSON Schema](sdd-model-schema.json)**: Auto-generated schema for IDE validation

## 🤝 Contributing

Contributions welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Write tests for new functionality
4. Ensure `./gradlew spotlessApply` passes
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) for details.

## 🙏 Acknowledgments

Built with:

- [Jackson](https://github.com/FasterXML/jackson) for YAML/JSON parsing
- [Vavr](https://www.vavr.io/) for functional validation
- [Picocli](https://picocli.info/) for CLI framework
- [LangChain4j](https://github.com/langchain4j/langchain4j) for LLM integration
- [JUnit 5](https://junit.org/junit5/) for unit testing
