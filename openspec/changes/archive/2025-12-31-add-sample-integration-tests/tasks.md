## 1. Implementation
- [x] 1.1 Add PostgreSQL-backed integration test configuration for the sample module using an existing Postgres instance (no Testcontainers).
- [x] 1.2 Implement MockMvc-only integration tests that apply generated DDL, boot the generated Spring Boot app, and exercise REST endpoints/state transitions (MVC layer only) in the default `test` source set.
- [x] 1.3 Wire tests into Gradle default lifecycle (`test`/`check`) and document required Postgres connection settings in `sample/README.md` (no skip logic).
- [x] 1.4 Ensure CI/dev environments connect to the provided Postgres service (fail fast if unreachable rather than skipping).

## 2. Validation
- [x] 2.1 `./gradlew :sample:test`
- [x] 2.2 `openspec validate add-sample-integration-tests --strict`
