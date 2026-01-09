## 1. Implementation
- [ ] 1.1 Add lower_snake_case identifier patterns for schemas, entities, states, tables, and attributes in the JSON Schema generator.
- [ ] 1.2 Emit enums for `from` and `from_any_of` values based on declared states per entity.
- [ ] 1.3 Enforce mutual exclusivity of `from` and `from_any_of` in the JSON Schema (oneOf/anyOf pattern) and validators.
- [ ] 1.4 Define required shapes for `extensions` and `projections`, including kind-specific fields (e.g., interval columns), while keeping map keys flexible.
- [ ] 1.5 Update JSON Schema artifacts/fixtures and developer docs reflecting the stricter validation.
- [ ] 1.6 Extend parser/validation tests to cover invalid identifiers, bad transitions, mixed transition forms, and incomplete extension/projection configs.
- [ ] 1.7 Run `openspec validate update-sdd-schema-constraints --strict`.
