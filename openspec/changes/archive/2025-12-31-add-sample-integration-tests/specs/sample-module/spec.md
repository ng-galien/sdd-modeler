## MODIFIED Requirements
### Requirement: Sample Module Integration Tests for Generated Code
The sample module SHALL provide an automated integration test suite that regenerates and applies the SDD DDL to PostgreSQL, boots the generated Spring Boot application, and exercises only the generated REST entry points (MVC layer) and state transitions end-to-end using MockMvc, assuming an existing PostgreSQL instance is reachable (developer machine or CI service container).

-#### Scenario: MVC integration tests run against configured PostgreSQL
- **WHEN** `./gradlew :sample:integrationTest` runs with PostgreSQL connection properties configured via environment or Gradle properties
- **THEN** the build regenerates DDL from `src/main/resources/sdd.yaml`, applies it to a clean database, starts the generated application, and all MockMvc-based REST endpoint tests for the generated Lead domain pass
#### Scenario: MVC integration tests run in default test source set
- **WHEN** `./gradlew :sample:test` runs with PostgreSQL connection properties configured via environment or Gradle properties
- **THEN** the build regenerates DDL from `src/main/resources/sdd.yaml`, applies it to a clean database, starts the generated application, and all MockMvc-based REST endpoint tests for the generated Lead domain pass
