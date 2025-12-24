## ADDED Requirements
### Requirement: Centralized Gradle Conventions
The build SHALL centralize shared Gradle configuration at the root level to minimize per-module
duplication and keep module scripts focused on module-specific concerns.

#### Scenario: Shared settings defined once
- **WHEN** a configuration applies to multiple modules
- **THEN** it is declared once at the root and applied consistently to subprojects

### Requirement: Version Catalog Usage
Dependency and plugin versions MUST be managed through the Gradle version catalog and referenced
by aliases in build scripts.

#### Scenario: Adding a dependency
- **WHEN** a new dependency or plugin is introduced
- **THEN** its version is defined in the version catalog and referenced by alias

### Requirement: Lean Default Build
The default build lifecycle SHALL avoid unnecessary heavy tasks and keep essential tasks fast and
predictable.

#### Scenario: Non-essential tasks
- **WHEN** a task is expensive or optional
- **THEN** it is opt-in and not wired into the default lifecycle

### Requirement: Minimal Custom Build Logic
Custom Gradle logic MUST be limited to cases where standard Gradle DSL or plugins are insufficient.

#### Scenario: Choosing between custom and standard logic
- **WHEN** a build behavior can be expressed with standard Gradle DSL or a widely used plugin
- **THEN** custom build logic is not introduced

### Requirement: CI Plugin Management for Sample Build
The build SHALL support GitHub CI building the `sample` module using the project Gradle plugin
module without relying on local publishing or manual setup steps.

#### Scenario: Building sample in CI
- **WHEN** `.github/workflows/ci.yml` runs the build for `sample`
- **THEN** the Gradle plugin module is resolved in the same build and the build succeeds without
  local publishing steps
