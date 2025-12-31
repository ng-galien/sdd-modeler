# Change: Add integration tests for sample module

## Why
The sample module currently lacks validated end-to-end coverage to ensure generated Spring Boot artifacts run correctly against PostgreSQL. We need reliable integration tests to prevent regressions in code generation outputs and sample documentation, assuming a PostgreSQL instance is always available (developer machine or GitHub CI service container).

## What Changes
- Add an integration test suite that boots the generated sample application against an existing PostgreSQL instance.
- Exercise generated REST endpoints and state transitions using the generated domain model.
- Ensure Gradle wiring and developer docs describe required PostgreSQL connection settings (no Testcontainers, no skip logic).

## Impact
- Affected specs: sample-module
- Affected code: `sample` module (tests, Gradle config, docs)
