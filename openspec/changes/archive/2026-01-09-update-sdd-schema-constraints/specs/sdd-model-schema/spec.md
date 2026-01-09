## ADDED Requirements
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
