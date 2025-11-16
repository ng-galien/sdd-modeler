package io.statemodeler.schema;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class to generate and save JSON Schema to resources directory.
 * Used by Gradle build tasks to automatically generate schema files.
 */
@Slf4j
public final class SchemaGeneratorTask {

    private SchemaGeneratorTask() {
        // Utility class
    }

    /**
     * Main method for Gradle task execution.
     * Generates SDD JSON Schema and saves it to src/main/resources.
     */
    public static void main(String[] args) throws Exception {
        SddSchemaGenerator generator = new SddSchemaGenerator();
        String schema = generator.generateSchema();

        // Determine target path - src/main/resources/sdd-model-schema.json
        Path resourcesDir = Paths.get("src/main/resources");
        Files.createDirectories(resourcesDir);

        Path schemaFile = resourcesDir.resolve("sdd-model-schema.json");
        Files.writeString(schemaFile, schema);

        log.info("✅ JSON Schema generated successfully:");
        log.info("📁 Location: {}", schemaFile.toAbsolutePath());
        log.info("🔗 Schema ID: https://schemas.statemodeler.io/v1/sdd-model.json");
        log.info("🎯 Ready for IDE integration and GitHub distribution");
    }

    /**
     * Generate schema and save to a specific directory.
     * @param targetDir the target directory path
     * @return the path to the generated schema file
     */
    public static Path generateSchemaToDirectory(String targetDir) throws Exception {
        SddSchemaGenerator generator = new SddSchemaGenerator();
        String schema = generator.generateSchema();

        Path outputDir = Paths.get(targetDir);
        Files.createDirectories(outputDir);

        Path schemaFile = outputDir.resolve("sdd-model-schema.json");
        Files.writeString(schemaFile, schema);

        return schemaFile;
    }
}
