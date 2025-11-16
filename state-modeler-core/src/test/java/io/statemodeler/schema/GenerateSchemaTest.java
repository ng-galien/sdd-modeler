package io.statemodeler.schema;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class GenerateSchemaTest {

    @Test
    void generateAndSaveSchema() throws Exception {
        // Generate schema
        SddSchemaGenerator generator = new SddSchemaGenerator();
        String schema = generator.generateSchema();

        // Show current working directory
        System.out.println("💾 Working directory: " + System.getProperty("user.dir"));

        // Save to test temp directory (don't pollute project root in tests)
        Path tempDir = Files.createTempDirectory("sdd-schema-test");
        Path schemaFile = tempDir.resolve("sdd-model-schema.json");
        Files.writeString(schemaFile, schema);

        System.out.println("✅ Schema generated and saved to: " + schemaFile);
        System.out.println("\n📋 Schema structure validated ✅");

        // Basic validation
        assert schema.contains("\"type\" : \"object\"");
        assert schema.contains("\"version\"");
        assert schema.contains("\"entities\"");
        assert schema.contains("\"database\"");
        assert schema.contains("$schema");
        assert schema.contains("schemas.statemodeler.io");

        System.out.println("\n✅ Schema validation passed!");
        System.out.println("🎯 Note: Schema is auto-generated in src/main/resources during build");
    }
}
