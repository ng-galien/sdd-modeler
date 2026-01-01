## 1. Implementation
- [ ] 1.1 Add Gradle plugin DSL options (default true) to toggle controller, MCP server, and repository generation; fail fast when REST/MCP enabled without repo/service.
- [ ] 1.2 Wire plugin tasks to pass new flags to the code generator using DSL-sourced values only.
- [ ] 1.3 Add `generateSddDdl` task with output path configuration.
- [ ] 1.4 Support Liquibase YAML changelog output (SQL payload in YAML) as an option for DDL generation.
- [ ] 1.5 Update docs/samples to demonstrate DSL flags, coherent-config failure, and DDL/Liquibase usage.
- [ ] 1.6 Add tests covering DSL options, failure on incoherent config, and DDL/Liquibase outputs.
