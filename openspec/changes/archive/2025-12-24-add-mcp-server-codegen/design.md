## Context
The generator currently emits Spring Boot artifacts without a dedicated configuration DTO to control feature toggles. Adding MCP server generation requires a clear toggle surface and defaults that preserve existing behavior.

## Goals / Non-Goals
- Goals: add MCP server code generation; introduce a configuration DTO with explicit toggles; keep default behavior backward compatible.
- Non-Goals: redesign existing generator templates or change current package naming conventions.

## Decisions
- Decision: introduce a code generation configuration DTO (Java record or equivalent) with boolean flags like `generateController` and `generateMcp`.
- Decision: default `generateController` to true to preserve current outputs; default `generateMcp` to false (opt-in).
- Decision: MCP server artifacts follow the existing base package naming scheme, reuse generated DTOs/services, and target the Spring AI MCP Server Boot Starter with `spring.ai.mcp.server.*` configuration.

## Risks / Trade-offs
- Adding a new DTO may require updates across CLI/Gradle/plugin entry points.

## Migration Plan
- Accept configuration DTO with defaults; existing inputs remain valid without changes.

## Open Questions
- None.
