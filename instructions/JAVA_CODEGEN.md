# Java Code Generation Overview

This document summarizes what the Java code generator produces from an SDD model and which features you get out of the box.

Generation is driven by the `database.generator_options.packageName` option in your SDD model (e.g. `com.example.leadcrm.domain`). All Java classes are generated under that package, plus a few non‑Java support files.

At the moment only the `java` language is supported.

## Per‑entity generated artifacts

For each `EntityDef` in the model, the generator produces a small Spring Data / Spring Boot stack.

### Identity and state types

- `{{package}}/{{Entity}}Id.java`
  - A Java `record` wrapping a `UUID`.
  - JSON‑friendly (`@JsonCreator` and `@JsonValue`) and used everywhere as the strong type for the entity id.

- `{{package}}/{{Entity}}State.java`
  - A **sealed interface** representing the state ADT for the entity.
  - Contains one nested `record` per state (e.g. `New`, `Contacted`, `Qualified`…), each annotated with:
    - `@Table("<state_table>")`
    - `@Id` / `@Column` mappings for the technical id and the entity id column.
    - `@JsonProperty` on every column.
  - All constructor parameters are `requireNonNull`, so generated state objects enforce non‑null state attributes.

- `{{package}}/{{Entity}}DomainState.java`
  - A Spring Data `record` mapped to the `<entity>_state` table.
  - Stores the **current state projection** per entity id:
    - `entityId` (`{{Entity}}Id`)
    - `stateType` (string enum of state names, e.g. `"NEW"`, `"QUALIFIED"`)
    - `stateRowId` (FK to the concrete state row)
    - `stateAt` (`Instant` timestamp)
    - `stateJson` (JSON snapshot of the state row)

- `{{package}}/{{Entity}}DomainStateRepository.java`
  - A Spring Data `CrudRepository<{{Entity}}DomainState, {{Entity}}Id>`.
  - Used by the service layer to list all entities with their current state.

### DTOs and converters

- `{{package}}/{{Entity}}Dto.java`
  - A Java `record` DTO exposing:
    - the typed id (`{{Entity}}Id`), and
    - all stable entity attributes mapped to appropriate Java types (`String`, `BigDecimal`, `Instant`, `UUID`, `List<T>`, …).
  - Types are inferred from the PostgreSQL‐style `type` fields in the model via `JavaContextBuilder.mapSqlTypeToJavaType`.

- `{{package}}/{{Entity}}Converters.java`
  - Holds Spring `Converter`s between `UUID` and `{{Entity}}Id`:
    - `UuidTo{{Entity}}IdConverter` (reading)
    - `{{Entity}}IdToUuidConverter` (writing)
  - Used by the shared configuration to integrate with Spring Data JDBC and Web bindings.

### Repositories

- `{{package}}/{{State}}Repository.java` (one per state)
  - A Spring Data `CrudRepository<{{Entity}}State.{{State}}, {{Entity}}Id>`.
  - Gives you basic CRUD operations for each concrete state table (insert, find by id, delete).

### Services

- `{{package}}/{{Entity}}Service.java`
  - A service interface centered on **state transitions**.
  - Generated content:
    - `record {{Entity}}StateInfo(String stateType, {{Entity}}State state)` — view model used by `findAll()`.
    - `List<{{Entity}}StateInfo> findAll()` — lists all current states using the domain state repository.
    - For each non‑initial state with a `from`/`from_any_of` transition:
      - `{{Entity}}Dto transitionTo{{State}}({{Entity}}Id id, TransitionTo{{State}}Command command);`
      - `record TransitionTo{{State}}Command(...)` built from the target state attributes, with `requireNonNull` checks for non‑nullable fields.

- `{{package}}/Default{{Entity}}Service.java`
  - Default implementation of `{{Entity}}Service` using Spring Data repositories and `ObjectMapper`.
  - Responsibilities:
    - `findAll()`
      - Reads all `{{Entity}}DomainState` rows via the domain state repository.
      - Deserializes `stateJson` into the appropriate nested state record (`{{Entity}}State.New`, `{{Entity}}State.Qualified`, …).
      - Returns a `List<{{Entity}}StateInfo>`.
    - `transitionTo{{State}}(...)` for each state with incoming transitions:
      - Locates the current state row via the configured `from` repositories.
      - Deletes the previous state row.
      - Inserts a new `{{Entity}}State.{{State}}` row populated from the command record.
      - Returns a `{{Entity}}Dto` (with TODO placeholders for stable attributes that are not yet fetched).
  - All transition methods are marked `@Transactional` to keep state changes consistent.

- `{{package}}/{{Entity}}ServiceAutoConfiguration.java`
  - Spring Boot auto‑configuration class for the service.
  - Declares a `@Bean` of type `{{Entity}}Service` if none is already present.
  - Wires all state repositories, the domain state repository, and an `ObjectMapper` into `Default{{Entity}}Service`.
  - Each class is listed in the global `AutoConfiguration.imports` file so that Spring Boot can discover them.

### HTTP API (controller + HTTP client)

- `{{package}}/{{Entity}}Api.java`
  - A declarative HTTP client interface using Spring’s `@HttpExchange` / `@GetExchange` / `@PostExchange` annotations.
  - Base path: `/api/<entity>s` (pluralized by adding `s`).
  - For stateful entities:
    - `GET /api/<entity>s` → `List<{{Entity}}StateInfo>`
    - `GET /api/<entity>s/<state>/{id}` → `{{Entity}}Dto` per state
    - `POST /api/<entity>s/{id}/transitions/to<State>` → executes the corresponding transition using a command body.
  - For stateless entities (no `states` block): a simple `GET /api/<entity>s/{id}` endpoint.

- `{{package}}/{{Entity}}Controller.java`
  - A `@RestController` implementation of `{{Entity}}Api`.
  - For stateful entities:
    - Injects `{{Entity}}Service` and all state repositories.
    - `findAll()` delegates to the service.
    - `get<State>()` methods query the appropriate state repository and map the state record to a `{{Entity}}Dto`.
    - Transition endpoints delegate to `{{Entity}}Service.transitionTo<State>(...)` and return the resulting DTO.
  - For stateless entities:
    - Injects a single repository and exposes `get(id)` that maps the entity to `{{Entity}}Dto` or returns 404.

- `http/<entityCamel>.http`
  - A `.http` file (for IntelliJ/VS Code HTTP clients) with ready‑to‑run examples:
    - `GET` requests for listing and fetching states.
    - `POST` requests for each transition, with example JSON bodies inferred from the state attributes and their Java types.

## Model‑wide generated artifacts

Some artifacts are generated once per model, not per entity.

- `{{package}}/SddConfig.java`
  - Central Spring `@Configuration` class.
  - Defines a `JdbcCustomConversions` bean that:
    - registers `UuidTo{{Entity}}IdConverter` / `{{Entity}}IdToUuidConverter` for all entities;
    - adds JSONB converters (`PGobject` ↔ `String`) for PostgreSQL.
  - Also exposes Spring `Converter<String, {{Entity}}Id>` and `Converter<{{Entity}}Id, String>` beans for request path / query parameter binding.

- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  - A plain text file listing all `{{Entity}}ServiceAutoConfiguration` classes.
  - Enables Spring Boot’s `spring.factories`‑style auto‑configuration mechanism without extra manual wiring.

## Features provided by Java generation

Putting it all together, Java code generation gives you:

- **Type‑safe domain model**
  - Strongly‑typed IDs as small value objects.
  - Sealed state ADTs with one record per state, matching the SDD model.
  - DTOs and state records whose fields and Java types are inferred from the SQL types in the model.

- **State‑driven service layer**
  - A generated service interface and default implementation that:
    - exposes `findAll()` returning the current state for every entity;
    - exposes transition methods for each allowed state change, with command records that validate non‑nullable fields.
  - Transition logic that enforces the allowed `from` / `from_any_of` transitions from the SDD model.

- **Spring Data & Spring Boot integration**
  - Repositories for each state and for the domain state projection.
  - Auto‑configuration wiring services, repositories and `ObjectMapper`.
  - Custom converters for IDs and JSONB registered with Spring Data JDBC and the web stack.

- **HTTP API and clients**
  - REST controllers and declarative HTTP interfaces exposing the state‑driven API.
  - `.http` files for manual or automated endpoint testing.

These artifacts are generated from a single SDD model file (`sdd.yaml` or JSON), so any change to the model is reflected consistently across database schema, Java domain code, and HTTP APIs.

