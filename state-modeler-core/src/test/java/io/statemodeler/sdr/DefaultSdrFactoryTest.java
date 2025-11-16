package io.statemodeler.sdr;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultSdrFactoryTest {

    private DefaultSdrFactory factory;

    private static final String YAML_MODEL = """
            version: "0.1"
            name: "test-model"
            database:
              dialect: postgres
              schema: public
            entities:
              order:
                table: orders
                id:
                  name: id
                  type: serial
                  primary_key: true
                states:
                  pending:
                    initial: true
                    table: order_pending
                    attributes:
                      reason:
                        type: text
                        nullable: false
            """;

    private static final String JSON_MODEL = """
            {
              "version": "0.1",
              "name": "test-model",
              "database": {
                "dialect": "postgres",
                "schema": "public"
              },
              "entities": {
                "order": {
                  "table": "orders",
                  "id": {
                    "name": "id",
                    "type": "serial",
                    "primary_key": true
                  },
                  "states": {
                    "pending": {
                      "initial": true,
                      "table": "order_pending",
                      "attributes": {
                        "reason": {
                          "type": "text",
                          "nullable": false
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

    @BeforeEach
    void setUp() {
        factory = new DefaultSdrFactory();
    }

    @Test
    void shouldCreateSdrFromYaml() {
        // When
        var sdr = factory.create(YAML_MODEL, "application/yaml", "postgres");

        // Then
        assertNotNull(sdr);
        assertEquals("application/yaml", sdr.contentType());
        assertNotNull(sdr.schema());
        assertNotNull(sdr.ddl());
        assertNotNull(sdr.schemaHash());
        assertNotNull(sdr.ddlHash());
        assertEquals("1.0.0", sdr.version());

        // Verify DDL contains expected elements
        assertTrue(sdr.ddl().contains("CREATE TABLE"));
        assertTrue(sdr.ddl().contains("orders"));
        assertTrue(sdr.ddl().contains("order_pending"));
    }

    @Test
    void shouldCreateSdrFromJson() {
        // When
        var sdr = factory.create(JSON_MODEL, "application/json", "postgres");

        // Then
        assertNotNull(sdr);
        assertEquals("application/json", sdr.contentType());
        assertNotNull(sdr.schema());
        assertNotNull(sdr.ddl());
        assertNotNull(sdr.schemaHash());
        assertNotNull(sdr.ddlHash());
        assertEquals("1.0.0", sdr.version());
    }

    @Test
    void shouldProduceIdenticalHashesForYamlAndJson() {
        // When
        var yamlSdr = factory.create(YAML_MODEL, "application/yaml", "postgres");
        var jsonSdr = factory.create(JSON_MODEL, "application/json", "postgres");

        // Then - hashes should be identical (format-independent)
        assertEquals(yamlSdr.schemaHash(), jsonSdr.schemaHash());
        assertEquals(yamlSdr.schema(), jsonSdr.schema()); // Canonical JSON should match
    }

    @Test
    void shouldProduceDeterministicHashes() {
        // When
        var sdr1 = factory.create(YAML_MODEL, "application/yaml", "postgres");
        var sdr2 = factory.create(YAML_MODEL, "application/yaml", "postgres");

        // Then
        assertEquals(sdr1.schemaHash(), sdr2.schemaHash());
        assertEquals(sdr1.schema(), sdr2.schema());
    }

    @Test
    void shouldRejectNullModelSource() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class, () -> factory.create(null, "application/yaml", "postgres"));
        assertTrue(exception.getMessage().contains("modelSource"));
    }

    @Test
    void shouldRejectBlankModelSource() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class, () -> factory.create("  ", "application/yaml", "postgres"));
        assertTrue(exception.getMessage().contains("modelSource"));
    }

    @Test
    void shouldRejectNullContentType() {
        // When/Then
        var exception =
                assertThrows(IllegalArgumentException.class, () -> factory.create(YAML_MODEL, null, "postgres"));
        assertTrue(exception.getMessage().contains("contentType"));
    }

    @Test
    void shouldRejectNullDialect() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class, () -> factory.create(YAML_MODEL, "application/yaml", null));
        assertTrue(exception.getMessage().contains("sqlDialect"));
    }

    @Test
    void shouldRejectUnsupportedContentType() {
        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class, () -> factory.create(YAML_MODEL, "application/xml", "postgres"));
        assertTrue(exception.getMessage().contains("Unsupported content type"));
    }

    @Test
    void shouldRejectInvalidYaml() {
        // Given
        String invalidYaml = "invalid: yaml: content:";

        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class, () -> factory.create(invalidYaml, "application/yaml", "postgres"));
        assertTrue(exception.getMessage().contains("Failed to parse model"));
        assertNotNull(exception.getCause()); // Should have wrapped exception
    }

    @Test
    void shouldRejectInvalidJson() {
        // Given
        String invalidJson = "{invalid json}";

        // When/Then
        var exception = assertThrows(
                IllegalArgumentException.class, () -> factory.create(invalidJson, "application/json", "postgres"));
        assertTrue(exception.getMessage().contains("Failed to parse model"));
        assertNotNull(exception.getCause()); // Should have wrapped exception
    }

    @Test
    void shouldSupportAlternativeYamlContentTypes() {
        // When
        var sdr1 = factory.create(YAML_MODEL, "text/yaml", "postgres");
        var sdr2 = factory.create(YAML_MODEL, "application/x-yaml", "postgres");

        // Then
        assertNotNull(sdr1);
        assertNotNull(sdr2);
        assertEquals(sdr1.schemaHash(), sdr2.schemaHash());
    }

    @Test
    void shouldSupportAlternativeJsonContentTypes() {
        // When
        var sdr = factory.create(JSON_MODEL, "text/json", "postgres");

        // Then
        assertNotNull(sdr);
    }

    @Test
    void shouldReturnCorrectVersion() {
        // When
        String version = factory.version();

        // Then
        assertEquals("1.0.0", version);
    }

    @Test
    void shouldProduceCanonicalJsonSchema() {
        // When
        var sdr = factory.create(YAML_MODEL, "application/yaml", "postgres");

        // Then
        String schema = sdr.schema();

        // Should be valid JSON
        assertTrue(schema.startsWith("{"));
        assertTrue(schema.endsWith("}"));

        // Should be compact (no indentation)
        assertFalse(schema.contains("\n"));
        assertFalse(schema.contains("  "));
    }

    @Test
    void shouldHandleDifferentFieldOrdering() {
        // Given - YAML with different field order
        String yaml1 = """
                version: "0.1"
                name: "test"
                database:
                  dialect: postgres
                entities: {}
                """;

        String yaml2 = """
                name: "test"
                database:
                  dialect: postgres
                version: "0.1"
                entities: {}
                """;

        // When
        var sdr1 = factory.create(yaml1, "application/yaml", "postgres");
        var sdr2 = factory.create(yaml2, "application/yaml", "postgres");

        // Then - should produce same canonical representation
        assertEquals(sdr1.schemaHash(), sdr2.schemaHash());
    }
}
