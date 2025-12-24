# Change: Add MCP server code generation

## Why
Code generation needs to support an MCP server output and allow users to toggle generated artifacts.

## What Changes
- Add MCP server generation as a new codegen feature using the Spring AI MCP Server Boot Starter.
- Introduce a configuration DTO to toggle generated artifacts (controller, MCP server).
- Configure the sample module to enable all codegen features and add integration tests that validate generated code.

## Impact
- Affected specs: generate-code
- Affected code: generator pipeline, configuration parsing, sample module integration tests
