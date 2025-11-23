# SDD Modeler Sample

A sample Spring Boot application demonstrating the usage of SDD Modeler with automatic code generation.

## 🚀 Running the Sample

```bash
./gradlew :sample:bootRun
```

The application will start on port 8080 with the following endpoints:
- `GET /api/users` - Find all users (all states)
- `GET /api/users/active/{id}` - Get user in Active state
- `GET /api/users/inactive/{id}` - Get user in Inactive state
- `POST /api/users/{id}/transitions/toInactive` - Transition to Inactive state

## 🧪 Testing

### Unit Tests
```bash
./gradlew :sample:test
```

## 📂 Structure

- `src/main/resources/sdd.yaml`: The SDD model definition
- `build.gradle.kts`: Configures the `io.statemodeler.sdd-codegen` plugin
- `build/generated/sdd/`: Generated code (automatically added to source set)
  - **Entities**: `User.java`, `Active.java`, `Inactive.java`
  - **DTOs**: `UserDto.java`, `UserState.java`
  - **Repositories**: `ActiveRepository.java`, `InactiveRepository.java`, `DomainStateRepository.java`
  - **Services**: `UserService.java` (interface), `DefaultUserService.java` (implementation), `UserServiceAutoConfiguration.java`
  - **Controllers**: `UserApi.java` (interface with `@HttpExchange`), `UserController.java` (implementation)
  - **Converters**: Various Spring Data JDBC converters
  - **HTTP Tests**: `http/user.http` - Ready-to-use HTTP requests

## 🎯 Key Features Demonstrated

### Controller Interface with HttpExchange
The generated `UserApi` interface uses Spring's `@HttpExchange` annotations, enabling:
- Declarative HTTP client generation
- Clear separation between API contract and implementation
- Type-safe client creation

### Service Layer with AutoConfiguration
The generated service layer includes:
- `UserService` interface - Service contract
- `DefaultUserService` implementation - Default behavior
- `UserServiceAutoConfiguration` with `@ConditionalOnMissingBean` - Easy customization

### State Machine Pattern
The User entity demonstrates state-driven design with:
- Two states: Active and Inactive
- Transition from any state to Inactive
- State-specific repositories and endpoints

