package io.statemodeler.core;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DatabaseConfigTest {

    @Test
    void shouldCreateValidDatabaseConfig() {
        // Given & When
        var config = new DatabaseConfig("postgres", "public", null);

        // Then
        assertEquals("postgres", config.dialect());
        assertEquals("public", config.schema());
        assertNull(config.stateSchema());
        assertEquals("public_states", config.effectiveStateSchema());
    }

    @Test
    void shouldCreateConfigWithNullSchema() {
        // Given & When (schema can be null for default schema)
        var config = new DatabaseConfig("mysql", null, null);

        // Then
        assertEquals("mysql", config.dialect());
        assertNull(config.schema());
        assertNull(config.stateSchema());
        assertEquals("states", config.effectiveStateSchema());
    }

    @Test
    void shouldRejectNullDialect() {
        var ex = assertThrows(IllegalArgumentException.class, () -> new DatabaseConfig(null, "public", null));
        assertTrue(ex.getMessage().contains("dialect cannot be null"));
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        var config1 = new DatabaseConfig("postgres", "public", null);
        var config2 = new DatabaseConfig("postgres", "public", null);
        var config3 = new DatabaseConfig("mysql", "public", null);
        var config4 = new DatabaseConfig("postgres", "private", null);
        var config5 = new DatabaseConfig("postgres", "public", "custom_states");

        // Then
        assertEquals(config2, config1);
        assertNotEquals(config3, config1);
        assertNotEquals(config4, config1);
        assertNotEquals(config5, config1);
        assertEquals(config2.hashCode(), config1.hashCode());
    }

    @Test
    void shouldImplementToString() {
        // Given
        var config = new DatabaseConfig("postgres", "public", null);

        // When
        var result = config.toString();

        // Then
        assertTrue(result.contains("DatabaseConfig"));
        assertTrue(result.contains("dialect=postgres"));
        assertTrue(result.contains("schema=public"));
    }

    @Test
    void shouldHandleVariousDialects() {
        // Test common SQL dialects
        var postgres = new DatabaseConfig("postgres", "public", null);
        var mysql = new DatabaseConfig("mysql", "test", null);
        var sqlite = new DatabaseConfig("sqlite", null, null);
        var sqlserver = new DatabaseConfig("sqlserver", "dbo", null);

        assertEquals("postgres", postgres.dialect());
        assertEquals("mysql", mysql.dialect());
        assertEquals("sqlite", sqlite.dialect());
        assertEquals("sqlserver", sqlserver.dialect());
    }

    @Test
    void shouldHandleEmptyStringSchema() {
        // Given & When - empty string schema should be treated as null
        var config = new DatabaseConfig("postgres", "", null);

        // Then
        assertTrue(config.schema().isEmpty());
        assertEquals("postgres", config.dialect());
        assertEquals("states", config.effectiveStateSchema()); // empty treated as null
    }

    @Test
    void shouldHandleEmptyStringStateSchema() {
        // Given & When - empty string stateSchema should be treated as null
        var configWithSchema = new DatabaseConfig("postgres", "myapp", "");
        var configWithoutSchema = new DatabaseConfig("postgres", "", "");

        // Then - empty stateSchema falls back to default
        assertEquals("myapp_states", configWithSchema.effectiveStateSchema());
        assertEquals("states", configWithoutSchema.effectiveStateSchema());
    }

    @Test
    void shouldUseCustomStateSchema() {
        // Given & When
        var config = new DatabaseConfig("postgres", "public", "custom_states");

        // Then
        assertEquals("custom_states", config.stateSchema());
        assertEquals("custom_states", config.effectiveStateSchema());
    }

    @Test
    void shouldDefaultStateSchemaWhenNull() {
        // Given & When
        var configWithSchema = new DatabaseConfig("postgres", "myapp", null);
        var configWithoutSchema = new DatabaseConfig("postgres", null, null);

        // Then
        assertEquals("myapp_states", configWithSchema.effectiveStateSchema());
        assertEquals("states", configWithoutSchema.effectiveStateSchema());
    }
}
