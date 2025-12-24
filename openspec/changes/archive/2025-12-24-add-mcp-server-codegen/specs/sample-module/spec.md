## ADDED Requirements
### Requirement: Sample Module Code Generation Coverage
The sample module SHALL enable all available code generation features when generating sources.

#### Scenario: All feature toggles enabled
- **WHEN** the sample module runs code generation
- **THEN** every available code generation feature is enabled and produces artifacts

### Requirement: Sample Module Integration Tests for Generated Code
The sample module SHALL include integration tests that compile and validate generated code.

#### Scenario: Integration tests run
- **WHEN** sample module integration tests execute
- **THEN** the generated sources compile and the tests exercise generated entry points
