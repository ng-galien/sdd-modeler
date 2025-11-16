# Copilot Instructions for sdd-modeler

## Project Overview
**sdd-modeler** is a Java 25 library + CLI for implementing State-Driven Design (SDD). It generates PostgreSQL DDL from declarative YAML/JSON models describing entities, states, transitions, extensions, and projections.

## Core SDD Principles (Critical Context)
- **Entities vs States**: Separate stable entity data (`orders` table) from mutable state facts (`order_pending`, `order_paid` tables)
- **States as immutable facts**: Each state is an append-only record with non-null attributes, not status columns
- **Explicit state graphs**: Transitions defined via `from` (simple) or `from_any_of` (OR transitions requiring mapping tables like `canceled_source`)
- **Extensions for optionals**: Non-decisional, mutable data goes in separate extension tables (e.g., `order_paid_extensions`)
- **Derived current state**: No `status` column - current state derived from projections/views (`current_order_states`)

## Architecture & Module Structure
Multi-module Gradle project (not yet initialized):
- `state-modeler-core`: SDD model, YAML/JSON parsing, validation, SQL generation
- `state-modeler-cli`: Command-line interface wrapping core functionality
- `state-modeler-spring`: Future Java/Spring code generation

### Core Package Structure
```
io.statemodeler.core     // SddModel, EntityDef, StateDef, TransitionDef
io.statemodeler.dsl      // ModelLoader, YamlModelLoader, JsonModelLoader  
io.statemodeler.validation // ModelValidator, rules validation
io.statemodeler.sql      // SqlPlan, SqlPlanGenerator (dialect-agnostic)
io.statemodeler.sql.postgres // PostgresDdlRenderer
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

## Development Workflow
Since project structure isn't initialized yet:
1. Create Gradle multi-module setup with `settings.gradle.kts`
2. Implement core model classes first (`SddModel`, `EntityDef`, etc.)
3. Add YAML parsing with Jackson
4. Build validation rules matching SDD principles
5. Generate SQL plans, then PostgreSQL-specific DDL
6. CLI wraps core with Picocli for commands: `validate model.yaml`, `sql model.yaml --dialect postgres`

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