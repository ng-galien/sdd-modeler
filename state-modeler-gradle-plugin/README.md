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

Configure the plugin using the `sdd` extension:

```kotlin
sdd {
    // Path to your SDD model file (YAML or JSON)
    // Default: src/main/resources/sdd.yaml
    modelFile.set(file("src/main/resources/my-model.yaml"))

    // Directory where generated code will be placed
    // Default: build/generated/sdd
    outputDir.set(layout.buildDirectory.dir("generated-sources/sdd"))

    // Target language for generation
    // Default: java
    language.set("java")

    // Whether to add the output directory to the main source set
    // Default: true
    addToSourceSet.set(true)
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

## 📝 Example Usage

```kotlin
plugins {
    java
    id("io.statemodeler.sdd-codegen")
}

sdd {
    modelFile.set(file("model/orders.yaml"))
    language.set("java")
}

// The generated code will be automatically compiled
```
