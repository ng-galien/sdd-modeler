# SDD Modeler Sample

A sample Spring Boot application demonstrating the usage of SDD Modeler.

## 🚀 Running the Sample

```bash
./gradlew :sample:bootRun
```

## 🧪 Testing

```bash
./gradlew :sample:test
```

## 📂 Structure

- `src/main/resources/sdd.yaml`: The SDD model definition.
- `build.gradle.kts`: Configures the `io.statemodeler.sdd-codegen` plugin.
- `src/main/java`: Contains the generated code (automatically added to source set) and application logic.
