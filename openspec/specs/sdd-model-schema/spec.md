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

### Requirement: Schema Validates Naming Conventions
The JSON Schema SHALL enforce lower_snake_case identifiers for `database.schema`, `database.state_schema`, entity ids, state ids, table names, extension ids, and attribute names, rejecting uppercase or hyphenated names.

#### Scenario: Invalid identifier is rejected
- **WHEN** an entity id is `Order-Events` or a state table name is `PaidState`
- **THEN** JSON Schema validation fails with a pattern violation indicating lower_snake_case is required.

#### Scenario: Lower snake case accepted
- **WHEN** identifiers use `orders`, `order_paid`, and `order_items`
- **THEN** JSON Schema validation succeeds.

### Requirement: Schema Validates Transition Targets Exist
The JSON Schema generator SHALL constrain `from` and `from_any_of` values to the set of states declared for the same entity so that transitions cannot reference unknown states.

#### Scenario: Transition references unknown state is rejected
- **WHEN** a state declares `from: "archived"` but no `archived` state exists in the entity
- **THEN** schema validation fails with an enum violation listing the declared states.

#### Scenario: Transition enum matches declared states
- **WHEN** an entity defines states `draft` and `pending` and a state declares `from_any_of: ["draft"]`
- **THEN** schema validation passes.

### Requirement: Schema Enforces Transition Form Exclusivity
The JSON Schema SHALL forbid combining `from` with `from_any_of` on the same state definition; validation MUST fail when both are present.

#### Scenario: Mixed transition forms are rejected
- **WHEN** a state defines both `from: "draft"` and `from_any_of: ["pending", "approved"]`
- **THEN** JSON Schema validation fails with an error indicating the transition forms are mutually exclusive.

### Requirement: Schema Defines Extensions and Projections Structure
The JSON Schema SHALL describe required fields for `extensions` and `projections`, including `table` and `attributes` for extensions, and kind-specific properties for projections (e.g., interval column for `INTERVALS`, current state view config for `CURRENT_STATE`), while keeping map keys flexible.

#### Scenario: Extension missing table is rejected
- **WHEN** an extension object omits `table`
- **THEN** schema validation fails with a missing required property error.

#### Scenario: Projection missing kind-specific config is rejected
- **WHEN** a projection of kind `INTERVALS` omits its interval column configuration
- **THEN** schema validation fails with a missing required property error.

