# Implementation Plan: Java Code Generation with Pebble

This plan outlines the steps to implement Java code generation for the SDD Modeler using the Pebble template engine.

## 1. Add Dependencies

We need to add the Pebble template engine dependency to the `state-modeler-core` module.

**File:** `state-modeler-core/build.gradle.kts`

```kotlin
dependencies {
    // ... existing dependencies
    
    // Pebble Template Engine
    implementation("io.pebbletemplates:pebble:3.2.3")
}
```

## 2. Define Generator Architecture

We will create a new package `io.statemodeler.codegen` in `state-modeler-core` to house the generation logic.

### 2.1 CodeGenerator Interface

Define a generic interface for code generation to allow for multiple language implementations.

**File:** `state-modeler-core/src/main/java/io/statemodeler/codegen/CodeGenerator.java`

```java
package io.statemodeler.codegen;

import io.statemodeler.core.SddModel;
import java.util.Map;

public interface CodeGenerator {
    /**
     * Generates source code for the given SDD model.
     * @param model The SDD model to generate code for.
     * @return A map where key is the file path (relative to source root) and value is the file content.
     */
    Map<String, String> generate(SddModel model);
    
    /**
     * The language this generator targets (e.g., "java", "python").
     */
    String getLanguage();
}
```

### 2.2 PebbleCodeGenerator Implementation

Implement the interface using Pebble, capable of supporting multiple languages via templates.

**File:** `state-modeler-core/src/main/java/io/statemodeler/codegen/PebbleCodeGenerator.java`

```java
package io.statemodeler.codegen;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.statemodeler.core.EntityDef;
import io.statemodeler.core.SddModel;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class PebbleCodeGenerator implements CodeGenerator {

    private final PebbleEngine engine;
    private final String language;

    public PebbleCodeGenerator(String language) {
        this.engine = new PebbleEngine.Builder().build();
        this.language = language;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public Map<String, String> generate(SddModel model) {
        Map<String, String> generatedFiles = new HashMap<>();
        
        // Example: Generate ADT for each entity
        for (EntityDef entity : model.entities().values()) {
            String content = generateEntity(entity, model);
            String filename = resolveFilename(entity, model); 
            generatedFiles.put(filename, content);
        }
        
        return generatedFiles;
    }

    private String generateEntity(EntityDef entity, SddModel model) {
        // Template path: templates/<language>/entity.<ext>.pebble
        String templatePath = "templates/" + language + "/entity." + getExtension() + ".pebble";
        PebbleTemplate template = engine.getTemplate(templatePath);
        Map<String, Object> context = new HashMap<>();
        context.put("entity", entity);
        context.put("model", model);
        // Pass generic options
        context.put("options", model.database().generatorOptions());
        
        Writer writer = new StringWriter();
        try {
            template.evaluate(writer, context);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate entity " + entity.name(), e);
        }
    }
    
    private String getExtension() {
        return switch(language) {
            case "java" -> "java";
            case "python" -> "py";
            default -> "txt";
        };
    }

    private String resolveFilename(EntityDef entity, SddModel model) {
        // Simple strategy for now, can be improved
        if ("java".equals(language)) {
            String pkg = model.database().generatorOptions().getOrDefault("packageName", "com.example");
            return pkg.replace('.', '/') + "/" + entity.name() + ".java";
        }
        return entity.name() + "." + getExtension();
    }
}
```

## 3. Create Pebble Templates

Create the templates in `src/main/resources/templates/java`.

**File:** `state-modeler-core/src/main/resources/templates/java/entity.java.pebble`

```java
package {{ options.packageName }};

import java.util.UUID;
import java.time.Instant;

/**
 * Generated ADT for {{ entity.name }} states.
 */
public sealed interface {{ entity.name }}State {

    {% for state in entity.states.values() %}
    /**
     * State: {{ state.name }}
     */
    record {{ state.name }}(
        {% for attribute in state.attributes.values() %}
        {{ attribute.type }} {{ attribute.name }}{% if not loop.last %}, {% endif %}
        {% endfor %}
    ) implements {{ entity.name }}State {}
    {% endfor %}
}
```

**Note on Type Mapping:** The `attribute.type` in the model might be a SQL type (e.g., "VARCHAR"). We will need to implement a custom Pebble filter or a helper in the generator to map these to Java types (e.g., "String"). For this plan, we assume the types are compatible or mapped.

## 4. Update DatabaseConfig

The `DatabaseConfig` record currently lacks a way to store generator-specific options (like `packageName` for Java). We will add a generic `generatorOptions` map.

**File:** `state-modeler-core/src/main/java/io/statemodeler/core/DatabaseConfig.java`

```java
public record DatabaseConfig(
        String dialect, 
        @Nullable String schema, 
        @Nullable String stateSchema,
        Map<String, String> generatorOptions // New generic field
) {
    public DatabaseConfig {
         // ... existing checks
         if (generatorOptions == null) generatorOptions = Map.of();
    }
    // ... existing logic
}
```

Alternatively, the package name can be passed as a CLI argument and threaded through to the generator. For this plan, we assume adding it to the config is the preferred approach for consistency.

## 5. Integration

Integrate the `PebbleCodeGenerator` into the CLI commands (e.g., a new `GenerateCommand` or extending `SqlCommand`) to trigger the generation and write files to disk. The command should allow specifying the target language.
