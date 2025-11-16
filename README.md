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
```

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

- **`state-modeler-core`**: Model classes, YAML/JSON parsing, validation, SQL generation
- **`state-modeler-cli`**: Picocli-based command-line interface
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
- **Testing**: JUnit 5 + AssertJ (120+ tests, 85%+ coverage)

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
./gradlew :state-modeler-cli:run --args="validate examples/orders-sdd-model.yaml"

# Generate SQL
./gradlew :state-modeler-cli:run --args="sql examples/orders-sdd-model.yaml -o output.sql"
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
- [x] CLI integration (validate, sql commands)
- [x] Comprehensive test suite (120+ tests)

### 🚧 In Progress

- [ ] Trigger generation for automatic state transitions
- [ ] PL/pgSQL functions for business rule validation

### 🔮 Planned

- [ ] Java/Spring code generation
- [ ] MySQL/MariaDB support
- [ ] Advanced projections (aggregations, custom queries)
- [ ] Interactive CLI mode for model creation
- [ ] Diagram generation (Mermaid, PlantUML)

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
- [AssertJ](https://assertj.github.io/doc/) for fluent test assertions
