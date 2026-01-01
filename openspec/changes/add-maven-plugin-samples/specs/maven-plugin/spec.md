## ADDED Requirements
### Requirement: Maven Plugin Goal Parity
The Maven plugin SHALL provide goals equivalent to the Gradle plugin tasks for generating sources/resources, applying DDL, and running end-to-end verification of the generated application.

#### Scenario: Generate goal produces all artifacts
- **WHEN** `mvn sdd:generate` runs in a module with an SDD model configured
- **THEN** the generator executes and emits all supported artifacts (DDL, Java code, HTTP samples, diagrams) into the Maven output directories

#### Scenario: Apply goal updates database schema
- **WHEN** `mvn sdd:apply-ddl` runs with database connection properties supplied via Maven properties or environment variables
- **THEN** the generated DDL is applied to the target PostgreSQL instance successfully

#### Scenario: Verify goal exercises generated application
- **WHEN** `mvn sdd:verify` runs
- **THEN** it regenerates sources/DDL, boots the generated Spring Boot application, and executes the integration test suite against the generated REST endpoints

### Requirement: Maven Plugin Lifecycle Integration
The Maven plugin MUST integrate with the standard Maven lifecycle so that generation runs automatically while remaining opt-in for heavier steps.

#### Scenario: Generate goal bound to generate-sources
- **WHEN** the plugin is applied without overriding bindings
- **THEN** `sdd:generate` is bound to `generate-sources`, ensuring generated sources are available before compilation

#### Scenario: Reactor build resolves plugin without local publish
- **WHEN** the repository is built as a multi-module Maven reactor including the plugin and sample modules
- **THEN** the `maven-sample` module resolves the Maven plugin from the reactor without requiring a prior local/remote publish step

### Requirement: Maven Plugin Configuration Parity
The Maven plugin SHALL expose configuration properties equivalent to the Gradle plugin extension so users can configure the model path, output directories, package base, generation toggles, and database connection options consistently across build tools.

#### Scenario: Configure plugin via properties or environment
- **WHEN** a user sets Maven properties (or environment variables) for model location, output directories, package base, and database connection
- **THEN** the Maven plugin reads these values and passes them to the generator identically to the Gradle plugin

#### Scenario: Generation toggle compatibility
- **WHEN** a generation toggle (e.g., disable controllers) is set in the Maven plugin configuration
- **THEN** the generated artifacts match the behavior of the same toggle in the Gradle plugin
