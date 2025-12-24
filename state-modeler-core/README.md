# SDD Modeler Core

The core library for State-Driven Design (SDD) modeling, validation, and SQL generation.

## 🚀 Features

### Declarative State Modeling

- Define entities with immutable state transitions in YAML/JSON
- Explicit state graphs with simple (`from`) and OR transitions (`from_any_of`)
- Separation of stable entity data from mutable state facts
- Support for optional, non-decisional data via extensions
- Derived projections (state intervals, current state views)

### Production-Ready SQL Generation

- **Complete PostgreSQL DDL**: Entity tables, state tables, extension tables, projection views
- **Automatic foreign key indexing**: Performance-optimized with `idx_<table>_<column>` naming convention
- **Type validation**: Comprehensive PostgreSQL type checking (NUMERIC, TIMESTAMP, arrays, etc.)
- **OR transition handling**: Automatic mapping tables for `from_any_of` transitions
- **Schema separation**: Configurable entity vs. state schema isolation

### Robust Validation

- State graph coherence (initial state, valid transitions, no cycles)
- Attribute type validation for PostgreSQL compatibility
- Extension and projection reference validation
- Clear, actionable error messages

## 📖 Example Model

### YAML Definition

```yaml
version: "0.1.0"
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

## 🏗️ Architecture

### Core Principles

- **Immutable state facts**: No UPDATE on state tables - only INSERT (audit trail)
- **No status columns**: Derive current state from projections/views
- **Separation of concerns**: Entity data vs. state data in different schemas
- **Type safety**: Compile-time validation with Java 21 records

### Technology Stack

- **Language**: Java 21 (records, pattern matching, sealed types)
- **Parsing**: Jackson (YAML/JSON)
- **Validation**: Vavr (functional error accumulation)

## 🧪 Testing

### Running Integration Tests

PostgreSQL integration tests verify that generated DDL executes correctly. These tests require a running PostgreSQL instance.

**Quick start with Docker:**

```bash
# Start PostgreSQL in Docker
docker run -d --name sdd-postgres \
  -p 5432:5432 \
  -e POSTGRES_DB=sdd_test \
  -e POSTGRES_USER=test \
  -e POSTGRES_PASSWORD=test \
  postgres:16-alpine

# Run integration tests
./gradlew :state-modeler-core:test

# Stop and remove container when done
docker stop sdd-postgres && docker rm sdd-postgres
```

**Custom PostgreSQL configuration:**

Use environment variables to connect to a different PostgreSQL instance:

```bash
export POSTGRES_HOST=your-host
export POSTGRES_PORT=5432
export POSTGRES_DB=your_db
export POSTGRES_USER=your_user
export POSTGRES_PASSWORD=your_password

./gradlew :state-modeler-core:test
```

**CI/CD:** GitHub Actions automatically provides a PostgreSQL service container for integration tests.

**Note:** If PostgreSQL is not available, integration tests will be automatically skipped with a clear message.
