# Design: Maven Plugin Module

## Overview
Create a Maven plugin module that mirrors the existing Gradle plugin behavior for SDD code/DDL
generation. Goals and parameters should feel native to Maven while mapping one-to-one with the
Gradle extension defaults.

## Module Layout
- New Maven module: `state-modeler-maven-plugin` at repository root.
- Packaging: `maven-plugin`; Java 21 toolchain.
- Coordinates: `io.statemodeler:sdd-maven-plugin` (align with Gradle plugin id naming).
- Reactor wiring: depend on `state-modeler-core` via module/reac tor reference; avoid publish steps
  (`mvn install` not required in CI or locally).

## Plugin Goals
- `generate-sdd-code`: validate model, generate code; adds output dir to compile source root when
  `addToSource` true (default true, mirroring Gradle `addToSourceSet`).
- `generate-sdd-ddl`: validate model, emit `schema.sql` by default; when `liquibase=true`, write
  `changelog.yaml` (matching Gradle behavior).

## Parameters (mirror Gradle extension)
- `modelFile` (File, default `src/main/resources/sdd.yaml`).
- `outputDir` (File, default `target/generated-sources/sdd`).
- `ddlOutputDir` (File, default `target/generated-sources/sdd/ddl`).
- Toggles: `generateController`, `generateRepository`, `generateMcp` (all default true).
- `language` (String, default `java`).
- `addToSource` (boolean, default true) — if true, register `outputDir` as compile source root.
- `liquibase` (boolean, default false) — selects changelog output for DDL goal.

## Implementation Notes
- Reuse existing core services for validation and generation; do not duplicate logic.
- Shared option parsing: map Maven params to the core generator config DTO already used by Gradle.
- Error handling: fail build on validation errors; surface readable messages consistent with Gradle.
- Testing: unit tests for parameter mapping/defaults; integration test running goals against sample
  model (similar to Gradle plugin tests) to assert outputs exist.

## Documentation & Sample
- Add Maven section to plugin docs describing parameters and defaults (aligned with Gradle docs).
- Provide `maven/maven-sample` using the plugin; runnable via `mvn clean verify` to demonstrate
  both goals and Liquibase toggle.

## CI
- Extend CI to build `state-modeler-maven-plugin` and run `maven-sample` within the same workflow as
  Gradle plugin to ensure parity and reactor-based resolution.
