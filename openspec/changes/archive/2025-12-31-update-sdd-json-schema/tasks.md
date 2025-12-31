## 1. Implementation
- [x] 1.1 Generate the JSON Schema from the YAML DTOs (snake_case keys) instead of the domain model, and include missing fields (`generator_options`, `state_schema`, `from_any_of`, `primary_key`, `default`).
- [x] 1.2 Add required-field declarations and `additionalProperties: false` on structural objects while keeping map containers permissive.
- [x] 1.3 Encode semantic constraints: SemVer pattern for `version`, `dialect` enum for PostgreSQL, projection kind enum, and a Postgres type pattern aligned with `PostgresTypeValidator` (including array forms).
- [x] 1.4 BREAKING: Make `state.from` a nullable string (scalar) instead of an array; keep `from_any_of` for OR transitions. Update YAML DTOs/domain model, validation rules, DDL/codegen/diagram generation accordingly.
- [x] 1.5 Regenerate `sdd-model-schema.json` artifacts and update schema generator/tests to assert the new constraints and scalar `from`.
- [x] 1.6 Update fixtures/sample models/tests to the scalar form; add regression tests covering single vs multi-source transitions.

## 2. Validation
- [x] 2.1 Add/extend tests proving snake_case keys, required/semantic constraints, and scalar `from` fail fast for invalid models.
- [x] 2.2 Run `openspec validate update-sdd-json-schema --strict`.
