## ADDED Requirements
### Requirement: Gradle Plugin Codegen Toggles
The Gradle plugin SHALL expose DSL configuration flags (default true) that enable or disable generation of controllers, MCP server artifacts, and repositories, while enforcing dependencies so REST or MCP generation cannot proceed without repository/service generation enabled.

#### Scenario: Disable controller generation via plugin
- **WHEN** the Gradle plugin configuration sets controller generation to false
- **THEN** controller artifacts are skipped while other enabled artifacts are still generated

#### Scenario: Enable MCP server generation via plugin
- **WHEN** the plugin flag for MCP server generation is set to true
- **THEN** MCP server artifacts are generated under the configured base package

#### Scenario: Repository disable rejected when REST/MCP enabled
- **WHEN** repository (and service) generation is disabled while REST controllers or MCP generation are enabled
- **THEN** the plugin fails the task with a clear message (no auto-enable) to enforce coherent configuration

### Requirement: Gradle Plugin DDL Generation
The Gradle plugin SHALL provide a `generateSddDdl` task to generate database DDL from the SDD model and support emitting either standard PostgreSQL DDL files or Liquibase-formatted YAML changelog output (SQL contained in YAML).

#### Scenario: Generate PostgreSQL DDL via plugin task
- **WHEN** the DDL generation task runs with default settings
- **THEN** PostgreSQL DDL files are written to the configured output directory

#### Scenario: Generate Liquibase changelog output
- **WHEN** the DDL generation task is invoked with Liquibase output enabled
- **THEN** the generated artifacts are Liquibase changelog files representing the model schema
