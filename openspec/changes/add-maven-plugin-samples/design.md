## Context
- Only a Gradle plugin exists; Maven users cannot run the generator or sample.
- `sample` mixes Gradle build wiring with generated app integration tests, making parity testing for other build tools cumbersome.
- Goal: provide a Maven plugin with equivalent behavior and two sample modules (Gradle + Maven) sharing the same model and tests.

## Goals / Non-Goals
- Goals: Maven plugin parity with Gradle plugin tasks/config; shared test fixture (`common-sample`) reused by both samples; CI coverage for both build tools without local publishing.
- Non-Goals: Changing generator features/CLI semantics; altering code generation outputs; publishing to Maven Central/Gradle Portal (assume local or composite build resolution initially).

## Decisions
- Plugin coordinates: group `io.sdd` (align with Gradle plugin); artifact `sdd-modeler-maven-plugin`; goal prefix `sdd`.
- Maven goals: `sdd:generate` (generate sources/resources), `sdd:apply-ddl` (apply to PostgreSQL), `sdd:verify` (boot generated app + run integration tests hook). Bind `sdd:generate` to `generate-sources` by default; others opt-in.
- Configuration surface: mirror Gradle extension fields (model path, output dirs, package base, db connection, flags for artifacts). Support properties and environment overrides.
- Sample layout: `common-sample` holds SDD model, generated sources baseline, test fixtures (MockMvc suites) as reusable test-jar/test-fixtures; `gradle-sample` depends on it and wires Gradle plugin; `maven-sample` depends on it and wires Maven plugin.
- CI: extend workflows to run `./gradlew :gradle-sample:test` and `mvn -pl maven-sample verify` (or wrapper) with shared Postgres setup via Testcontainers/CI services.

## Risks / Trade-offs
- Parity drift between plugins if configuration surfaces diverge → mitigate with shared config contract and cross-plugin tests.
- Longer CI due to running both build tools → mitigate via caching and selective workflows.
- Database requirements for integration tests across both samples → use Testcontainers or shared service container setup.

## Migration Plan
- Introduce `common-sample` and `gradle-sample` while keeping existing `sample` until new modules are green; then retire/replace `sample` references.
- Add Maven plugin and `maven-sample`; align CI; remove old sample tasks after validation.

## Open Questions
- Should Maven plugin be published to local repo during build or resolved via reactor module only?
- Do we need a separate packaging for test fixtures (`common-sample` as `test-fixtures` classifier) or plain jar with tests on test classpath?
