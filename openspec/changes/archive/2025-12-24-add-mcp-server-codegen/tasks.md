## 1. Implementation
- [x] 1.1 Add a code generation configuration DTO with toggles for controller and MCP server generation.
- [x] 1.2 Wire the configuration DTO into generator options parsing and defaults.
- [x] 1.3 Implement MCP server code generation using the Spring AI MCP Server Boot Starter (annotations + `spring.ai.mcp.server.*` config), gated by the new toggle.
- [x] 1.4 Configure the sample module to enable all code generation features and update fixtures or expected outputs for the new artifacts.
- [x] 1.5 Add integration tests in the sample module that compile and exercise generated code, including MCP generation and toggle behavior.
