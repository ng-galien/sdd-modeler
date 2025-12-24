# State-Driven Design (SDD)

This spec is split into two focused documents:
- `@openspec/specs/sdd-principles.md` for modeling principles and anti-patterns.
- `@openspec/specs/sdd-sql-mapping.md` for SQL mapping rules and examples.

Both documents reflect the same core ideas: states are first-class, explicit, append-only facts,
with transitions encoded in the model, and the current state derived from history.
