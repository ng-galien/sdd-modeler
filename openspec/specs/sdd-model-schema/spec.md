# sdd-model-schema Specification

## Purpose
Define the JSON/YAML DSL contract for SDD models, including transition semantics and schema constraints used by validators, generators, and IDE tooling.

## Requirements
### Requirement: Schema Mirrors YAML DSL Field Names
The generated SDD JSON Schema SHALL expose the snake_case property names used in the YAML/JSON DSL (e.g., `generator_options`, `state_schema`, `from_any_of`, `primary_key`, `default`) and MUST NOT rely on camelCase-only aliases.

#### Scenario: Snake-case keys are present
- **WHEN** the JSON Schema is generated for the SDD DSL
- **THEN** the schema contains properties named `generator_options`, `state_schema`, `from_any_of`, `primary_key`, and `default`, and IDE validation works with those keys without requiring camelCase variants.

### Requirement: Scalar Simple Transition
State simple transitions SHALL be expressed with a single optional `from` value (string) referencing exactly one predecessor state. Using multiple predecessors SHALL require `from_any_of`.

#### Scenario: One predecessor allowed
- **WHEN** a state defines `from: "draft"`
- **THEN** the DSL accepts the model and the generated table has one `previous_draft_id` FK.

#### Scenario: Multiple predecessors rejected in `from`
- **WHEN** a state attempts `from: ["draft", "pending"]`
- **THEN** validation fails with a “use from_any_of for multiple sources” error.

### Requirement: Mutual Exclusivity With OR Transitions
The DSL SHALL forbid combining `from` with `from_any_of`; only one transition form may be present on a state.

#### Scenario: Mixed transition forms
- **WHEN** a state defines both `from: "draft"` and `from_any_of: ["pending", "approved"]`
- **THEN** validation fails and no schema is generated for that model.

### Requirement: Generated Artifacts Match Transition Semantics
The generators SHALL emit a single `previous_<state>_id` FK column for scalar `from` transitions, and SHALL only emit OR mapping tables (`<state>_source` with XOR CHECK) when `from_any_of` is present.

#### Scenario: DDL for scalar transition
- **WHEN** a state uses `from: "draft"`
- **THEN** the generated DDL includes one `previous_draft_id` column with a composite FK to the draft state table, and no mapping table for that state.

#### Scenario: DDL for OR transition
- **WHEN** a state uses `from_any_of: ["pending", "approved"]`
- **THEN** the generated DDL includes a `<state>_source` mapping table with one nullable FK column per source state and an XOR CHECK, and the state table references this mapping via `previous_source_id`.

### Requirement: Schema Enforces Required Core Structure
The JSON Schema SHALL declare required fields for root and nested objects (model, database, entity, state, attribute) and set `additionalProperties: false` on structural objects while allowing map containers (entities, states, attributes, extensions, projections) to accept arbitrary keys.

#### Scenario: Missing required property is rejected
- **WHEN** a model omits `database.dialect` or an entity state lacks `table`
- **THEN** JSON Schema validation fails with a missing required property error, preventing the DSL from passing IDE validation.

### Requirement: Schema Captures Semantic Constraints
The JSON Schema SHALL encode domain constraints: `version` MUST match SemVer (MAJOR.MINOR.PATCH), `database.dialect` MUST be `postgres`, `projection.kind` MUST be one of `CURRENT_STATE` or `INTERVALS`, and `attribute.type` MUST match the PostgreSQL type patterns accepted by `PostgresTypeValidator`, including array forms.

#### Scenario: Invalid semantic value is rejected
- **WHEN** the model uses `version: "latest"`, `database.dialect: "mysql"`, or an `attribute.type` of `BLOB`
- **THEN** JSON Schema validation reports enum/pattern violations, while a model with `version: "1.2.3"`, `dialect: "postgres"`, and `attribute.type: "TIMESTAMPTZ(3)[]"` passes validation.
