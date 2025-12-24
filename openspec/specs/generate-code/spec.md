# generate-code Specification

## Purpose
TBD - created by archiving change add-mcp-server-codegen. Update Purpose after archive.
## Requirements
### Requirement: Code Generation Configuration
The system SHALL expose a code generation configuration DTO that allows toggling individual generated artifacts.

#### Scenario: Disable controller generation
- **WHEN** `generateController` is set to false
- **THEN** controller artifacts are not generated

#### Scenario: Enable MCP server generation
- **WHEN** `generateMcp` is set to true
- **THEN** MCP server artifacts are generated

### Requirement: Spring AI MCP Server Generation
The generator SHALL produce MCP server artifacts using the Spring AI MCP Server Boot Starter auto-configuration and annotations when MCP generation is enabled in the configuration.

#### Scenario: Spring MCP annotations
- **WHEN** the model defines at least one entity and `generateMcp` is true
- **THEN** MCP server components are generated under the configured base package using Spring MCP annotations for tools/resources/prompts

#### Scenario: Spring MCP protocol configuration
- **WHEN** MCP server generation is enabled
- **THEN** generated configuration exposes Spring MCP protocol properties (`spring.ai.mcp.server.stdio=true` or `spring.ai.mcp.server.protocol=SSE|STREAMABLE|STATELESS`)

#### Scenario: Spring MCP server type configuration
- **WHEN** MCP server generation is enabled
- **THEN** generated configuration exposes `spring.ai.mcp.server.type=SYNC|ASYNC` to select the Spring MCP server type

