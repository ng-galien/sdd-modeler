# Change: Gradle plugin codegen improvements

## Why
The Gradle plugin currently exposes limited toggles for code generation and cannot emit database DDL. Teams need finer-grained control over generated artifacts (controllers, MCP server, repositories) and a way to produce DDL—including Liquibase changelog format—directly from the plugin.

## What Changes
- Add Gradle plugin DSL options to enable/disable controller, MCP, and repository generation explicitly (default: all enabled) and hard-fail on incoherent combinations (REST/MCP without repo/service).
- Extend the plugin to generate DDL from the model via a `generateSddDdl` task, with an option to output Liquibase YAML changelog files (SQL formatted inside YAML).

## Impact
- Affected specs: generate-code
- Affected code: state-modeler-gradle-plugin, code generator configuration/DTOs, documentation/examples
