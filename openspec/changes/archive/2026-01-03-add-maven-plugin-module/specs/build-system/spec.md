## ADDED Requirements
### Requirement: Maven Plugin Parity with Gradle Plugin
The build SHALL provide a Maven plugin module that mirrors the Gradle plugin goals, defaults, and
validation behavior for SDD code/DDL generation.

#### Scenario: Code generation goal parity
- **WHEN** a Maven user runs the plugin's code generation goal with default settings
- **THEN** the model is validated and code is generated to the default output directory with the
  same defaults as the Gradle plugin (language, add-to-source, toggles)

#### Scenario: DDL generation goal parity
- **WHEN** a Maven user runs the DDL generation goal
- **THEN** the plugin validates the model and emits schema.sql by default or a Liquibase changelog
  when the Liquibase toggle is enabled, matching Gradle plugin behavior

#### Scenario: Configuration parity
- **WHEN** a Maven user configures plugin parameters (model file path, code output dir, DDL output
  dir, generateController/repository/MCP toggles, add-to-source equivalent, Liquibase)
- **THEN** parameter names and defaults map cleanly to the Gradle plugin extension semantics so the
  same documentation applies across build tools

### Requirement: Maven Build Integration Without Publishing
The Maven plugin module MUST resolve project dependencies via the multi-module reactor so local and
CI builds do not require publishing `state-modeler-core` or the plugin to an external repository.

#### Scenario: Reactor-based dependency resolution
- **WHEN** the Maven plugin module is built in CI or locally
- **THEN** it uses module references (reactor or local project coordinates) to depend on
  `state-modeler-core`, avoiding `mvn install`/`publishToMavenLocal` steps

### Requirement: Maven Plugin Documentation and Sample
The project SHALL provide Maven-focused documentation and a sample project demonstrating plugin
usage with defaults and common overrides.

#### Scenario: Runnable Maven sample
- **WHEN** a developer runs the provided Maven sample using the plugin
- **THEN** code and DDL generation succeed with documented defaults, and toggles behave as described
  in the shared plugin docs
