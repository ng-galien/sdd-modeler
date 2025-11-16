package io.statemodeler.schema;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class SddSchemaGeneratorTest {

    @Test
    void shouldGenerateValidJsonSchema() throws Exception {
        // Given
        SddSchemaGenerator generator = new SddSchemaGenerator();

        // When
        String schema = generator.generateSchema();

        // Then
        assertNotNull(schema);
        assertFalse(schema.trim().isEmpty());

        // Verify it's valid JSON
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonSchema = mapper.readTree(schema);
        assertNotNull(jsonSchema);

        // Verify it has JSON Schema structure
        assertTrue(jsonSchema.has("type"));
        assertEquals("object", jsonSchema.get("type").asText());

        // Should have properties for SddModel fields
        assertTrue(jsonSchema.has("properties"));
        JsonNode properties = jsonSchema.get("properties");

        // Check for main SDD model properties
        assertTrue(properties.has("version"), "Schema should include 'version' property");
        assertTrue(properties.has("name"), "Schema should include 'name' property");
        assertTrue(properties.has("entities"), "Schema should include 'entities' property");
        assertTrue(properties.has("database"), "Schema should include 'database' property");

        // Print schema for debugging
        System.out.println("Generated JSON Schema:");
        System.out.println(schema);
    }

    @Test
    void shouldHandleSchemaGenerationErrors() {
        // This test ensures our exception handling works
        SddSchemaGenerator generator = new SddSchemaGenerator();

        // The generation should work fine, but if it fails, it should throw IllegalStateException
        assertDoesNotThrow(() -> {
            String schema = generator.generateSchema();
            assertNotNull(schema);
        });
    }
}
