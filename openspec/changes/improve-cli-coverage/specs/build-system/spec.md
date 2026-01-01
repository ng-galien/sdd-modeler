## ADDED Requirements
### Requirement: CLI Coverage Threshold
The CLI package under `state-modeler-app/src/main/java/io/statemodeler/cli` SHALL maintain at least 75% line coverage, and critical helpers (RepositoryMixin, CliCommandHelpers, Main) SHALL have tests covering normal and error paths.

#### Scenario: RepositoryMixin validation covered
- **WHEN** repository options are misconfigured (e.g., missing directory, invalid path)
- **THEN** tests assert the mixin reports errors/usage without uncaught exceptions

#### Scenario: CLI entrypoint smoke-tested
- **WHEN** the CLI `Main` is invoked with a representative command or help flag
- **THEN** execution completes without error and expected output/help is produced

#### Scenario: Helper utilities covered
- **WHEN** helper methods in `CliCommandHelpers` and `util` are exercised across success and failure cases
- **THEN** tests assert correct behavior, contributing to the 85% coverage target
