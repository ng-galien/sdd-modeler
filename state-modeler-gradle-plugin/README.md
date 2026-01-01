# SDD Modeler Gradle Plugin

A Gradle plugin to integrate State-Driven Design (SDD) code generation into your build process.

## 📦 Installation

Apply the plugin in your `build.gradle.kts`:

```kotlin
plugins {
    id("io.statemodeler.sdd-codegen") version "0.1.0" // Replace with actual version
}
```

## ⚙️ Configuration

Configure the plugin using the `sddCodegen` extension:

```kotlin
sddCodegen {
    // Path to your SDD model file (YAML or JSON)
    // Default: src/main/resources/sdd.yaml
    modelFile.set(file("src/main/resources/my-model.yaml"))

    // Directory where generated code will be placed
    // Default: build/generated/sdd
    outputDir.set(layout.buildDirectory.dir("generated-sources/sdd"))

    // Target language for generation (default: java)
    language.set("java")

    // Codegen toggles (default: all true)
    generateController.set(true)
    generateRepository.set(true)
    generateMcp.set(true)

    // Whether to add the output directory to the main source set
    // Default: true
    addToSourceSet.set(true)

    // DDL generation output directory (default: build/generated/sdd/ddl)
    ddlOutputDir.set(layout.buildDirectory.dir("generated/sdd/ddl"))

    // Liquibase YAML output (default: false -> generates schema.sql)
    liquibase.set(false)
}
```

## 🚀 Tasks

The plugin adds the following tasks:

### `generateSddCode`
Generates code from the configured SDD model.

- **Inputs**: The model file specified in `modelFile`.
- **Outputs**: The directory specified in `outputDir`.
- **Behavior**:
    - Validates the model before generation.
    - Generates code using the specified language generator.
    - Fails the build if validation errors occur.

This task is automatically wired into the build lifecycle if `addToSourceSet` is true (default).

### `generateSddDdl`
Generates DDL from the configured SDD model.

- **Outputs**: `schema.sql` by default, or `changelog.yaml` when `liquibase=true`.
- **Behavior**:
    - Validates the model before generation.
    - Uses the dialect specified in the model (PostgreSQL supported).
    - Fails on validation errors or incoherent codegen toggles (REST/MCP without repository/service).

## 📝 Example Usage

```kotlin
plugins {
    java
    id("io.statemodeler.sdd-codegen")
}

sddCodegen {
    modelFile.set(file("model/orders.yaml"))
    language.set("java")
    generateController.set(true)
    generateRepository.set(true)
    generateMcp.set(false)
}

// The generated code will be automatically compiled
```

## 🛠️ Development notes

This repository is configured for local development with a Gradle composite build. The root `settings.gradle.kts` includes a dependency substitution so the plugin can be built and tested against the local `:state-modeler-core` project without publishing the core module to a Maven repository.

- To build the plugin during local development (uses dependency substitution if available):

```bash
./gradlew :state-modeler-gradle-plugin:build
```

- If you prefer to publish the `state-modeler-core` artifact to your local Maven repository instead of using dependency substitution:

```bash
./gradlew :state-modeler-core:publishToMavenLocal
./gradlew :state-modeler-gradle-plugin:build
```

If you run into resolution problems, ensure your root settings include the `includeBuild("state-modeler-gradle-plugin")` and the substitution rule that maps `io.statemodeler:state-modeler-core` to `:state-modeler-core`.
