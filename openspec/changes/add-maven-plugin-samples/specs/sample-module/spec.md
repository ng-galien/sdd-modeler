## MODIFIED Requirements
### Requirement: Sample Module Code Generation Coverage
The sample modules SHALL enable all available code generation features when generating sources using the shared SDD model from `common-sample`.

#### Scenario: Gradle sample enables all generator features
- **WHEN** `./gradlew :gradle-sample:generateSddSources` runs using the Gradle plugin
- **THEN** every available code generation feature is enabled and produces artifacts under the configured Gradle output dirs

#### Scenario: Maven sample enables all generator features
- **WHEN** `mvn -pl maven-sample sdd:generate` runs using the Maven plugin
- **THEN** every available code generation feature is enabled and produces artifacts under the configured Maven output dirs

### Requirement: Sample Module Integration Tests for Generated Code
The sample modules SHALL provide an automated integration test suite (sourced from `common-sample`) that regenerates and applies the SDD DDL to PostgreSQL, boots the generated Spring Boot application, and exercises only the generated REST entry points (MVC layer) and state transitions end-to-end using MockMvc, assuming an existing PostgreSQL instance is reachable (developer machine, Testcontainers, or CI service container).

#### Scenario: Gradle sample integration tests
- **WHEN** `./gradlew :gradle-sample:test` runs with PostgreSQL connection properties configured via environment or Gradle properties
- **THEN** the build regenerates DDL from `common-sample`'s `sdd.yaml`, applies it to a clean database, starts the generated application, and all MockMvc-based REST endpoint tests pass

#### Scenario: Maven sample integration tests
- **WHEN** `mvn -pl maven-sample verify` runs with PostgreSQL connection properties configured via environment variables or Maven properties
- **THEN** the build regenerates DDL from `common-sample`'s `sdd.yaml`, applies it to a clean database, starts the generated application, and all MockMvc-based REST endpoint tests pass

## ADDED Requirements
### Requirement: Shared Sample Fixture Module
The project SHALL provide a `common-sample` module that owns the SDD model, generated source baseline, and reusable integration test fixtures so Gradle and Maven sample modules avoid duplicating assets.

#### Scenario: Shared model and tests reused by sample builds
- **WHEN** either sample module (`gradle-sample` or `maven-sample`) runs its build
- **THEN** the module depends on `common-sample` to obtain the SDD model and test fixtures, reusing the same MockMvc suites without duplication

#### Scenario: Test updates propagate to both sample variants
- **WHEN** integration tests or fixtures are modified in `common-sample`
- **THEN** the changes take effect in both sample builds without additional copy steps
