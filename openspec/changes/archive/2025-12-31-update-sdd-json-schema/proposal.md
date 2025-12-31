# Change: Improve SDD JSON Schema fidelity and scalarize `from`

## Why
The published SDD JSON Schema is generated from the domain model, so it exposes camelCase keys, omits required fields, and lacks semantic constraints. This lets invalid YAML (e.g., `generator_options`, `from_any_of`, non-SemVer versions, unsupported Postgres types) pass IDE validation while still failing at runtime. In addition, `from` is currently an array while the generator expects at most one predecessor, creating ambiguity versus `from_any_of` and producing noisy FKs. We need a schema and DSL shape that match the runtime rules.

## What Changes
- Generate the JSON Schema from the YAML-facing DTOs so snake_case keys (`generator_options`, `state_schema`, `from_any_of`, `primary_key`, `default`) are surfaced and camelCase-only aliases are removed.
- Add required-field lists and `additionalProperties: false` on structural objects while keeping map containers flexible, to catch typos early.
- Encode semantic constraints (SemVer pattern for `version`, `dialect` enum for PostgreSQL, projection kind enum, Postgres type pattern) so IDE validation aligns with the runtime validator.
- Regenerate the distributed `sdd-model-schema.json` and refresh tests/docs accordingly.
- BREAKING: Make `state.from` a single optional string (one predecessor) and require `from_any_of` for multi-source transitions; update validation, DDL/codegen, samples, and migration guidance.

## Impact
- Affected specs: sdd-model-schema
- Affected code: `state-modeler-core` schema generator/resources, validation, DDL/codegen/diagram generation, sample fixtures, documentation snippets referencing the schema URL
