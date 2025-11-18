package io.statemodeler.schema;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to generate and save JSON Schema to resources directory.
 * Used by Gradle build tasks to automatically generate schema files.
 */
public final class SchemaGeneratorTask {
    private static final Logger logger = LoggerFactory.getLogger(SchemaGeneratorTask.class);

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

        logger.info("✅ JSON Schema generated successfully:");
        logger.info("📁 Location: {}", schemaFile.toAbsolutePath());
        logger.info("🔗 Schema ID: https://schemas.statemodeler.io/v1/sdd-model.json");
        logger.info("🎯 Ready for IDE integration and GitHub distribution");
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
