## 1. Maven plugin
- [x] 1.1 Scaffold Maven plugin module (packaging, coordinates, plugin descriptor) mirroring Gradle plugin capabilities.
- [ ] 1.2 Implement goals for code generation/DDL/apply and hook into lifecycle; ensure config parity with Gradle plugin.
- [ ] 1.3 Add tests for Maven plugin goals (unit/functional) covering configuration and execution paths.

## 2. Sample modules
- [x] 2.1 Extract shared model, generated sources, and integration test harness into `common-sample`.
- [x] 2.2 Create `gradle-sample` module using the Gradle plugin and reusing `common-sample` tests.
- [x] 2.3 Create `maven-sample` module using the Maven plugin with identical tests reused from `common-sample`.

## 3. Build/CI wiring
- [x] 3.1 Wire Gradle build to include new modules without local publishing; ensure tasks for both sample variants.
- [x] 3.2 Add CI coverage for Gradle and Maven sample executions (including database/testcontainers setup).
- [ ] 3.3 Update docs/README for using Maven plugin and running sample variants.
