# Change: Strengthen SDD model JSON Schema constraints

## Why
Common DSL errors (bad identifiers, transitions to unknown states, incomplete extensions/projections) are slipping through until runtime validation. Tightening the JSON Schema lets IDEs and CI catch these early.

## What Changes
- Enforce lower_snake_case naming for schemas, entities, states, tables, and attributes directly in the JSON Schema.
- Constrain `from`/`from_any_of` values to declared states so invalid transitions are rejected by schema validation.
- Define required shapes for `extensions` and `projections`, including kind-specific fields, while keeping map keys open.

## Impact
- Affected specs: sdd-model-schema
- Affected code: JSON Schema generator, model validators, schema fixtures/tests
