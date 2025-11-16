package io.statemodeler.core;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DatabaseConfigTest {

    @Test
    void shouldCreateValidDatabaseConfig() {
        // Given & When
        var config = new DatabaseConfig("postgres", "public", null);

        // Then
        assertThat(config.dialect()).isEqualTo("postgres");
        assertThat(config.schema()).isEqualTo("public");
        assertThat(config.stateSchema()).isNull();
        assertThat(config.effectiveStateSchema()).isEqualTo("public_states");
    }

    @Test
    void shouldCreateConfigWithNullSchema() {
        // Given & When (schema can be null for default schema)
        var config = new DatabaseConfig("mysql", null, null);

        // Then
        assertThat(config.dialect()).isEqualTo("mysql");
        assertThat(config.schema()).isNull();
        assertThat(config.stateSchema()).isNull();
        assertThat(config.effectiveStateSchema()).isEqualTo("states");
    }

    @Test
    void shouldRejectNullDialect() {
        assertThatThrownBy(() -> new DatabaseConfig(null, "public", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dialect cannot be null");
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
        assertThat(config1).isEqualTo(config2);
        assertThat(config1).isNotEqualTo(config3);
        assertThat(config1).isNotEqualTo(config4);
        assertThat(config1).isNotEqualTo(config5);
        assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
    }

    @Test
    void shouldImplementToString() {
        // Given
        var config = new DatabaseConfig("postgres", "public", null);

        // When
        var result = config.toString();

        // Then
        assertThat(result)
                .contains("DatabaseConfig")
                .contains("dialect=postgres")
                .contains("schema=public");
    }

    @Test
    void shouldHandleVariousDialects() {
        // Test common SQL dialects
        var postgres = new DatabaseConfig("postgres", "public", null);
        var mysql = new DatabaseConfig("mysql", "test", null);
        var sqlite = new DatabaseConfig("sqlite", null, null);
        var sqlserver = new DatabaseConfig("sqlserver", "dbo", null);

        assertThat(postgres.dialect()).isEqualTo("postgres");
        assertThat(mysql.dialect()).isEqualTo("mysql");
        assertThat(sqlite.dialect()).isEqualTo("sqlite");
        assertThat(sqlserver.dialect()).isEqualTo("sqlserver");
    }

    @Test
    void shouldHandleEmptyStringSchema() {
        // Given & When
        var config = new DatabaseConfig("postgres", "", null);

        // Then
        assertThat(config.schema()).isEmpty();
        assertThat(config.dialect()).isEqualTo("postgres");
        assertThat(config.effectiveStateSchema()).isEqualTo("_states");
    }

    @Test
    void shouldUseCustomStateSchema() {
        // Given & When
        var config = new DatabaseConfig("postgres", "public", "custom_states");

        // Then
        assertThat(config.stateSchema()).isEqualTo("custom_states");
        assertThat(config.effectiveStateSchema()).isEqualTo("custom_states");
    }

    @Test
    void shouldDefaultStateSchemaWhenNull() {
        // Given & When
        var configWithSchema = new DatabaseConfig("postgres", "myapp", null);
        var configWithoutSchema = new DatabaseConfig("postgres", null, null);

        // Then
        assertThat(configWithSchema.effectiveStateSchema()).isEqualTo("myapp_states");
        assertThat(configWithoutSchema.effectiveStateSchema()).isEqualTo("states");
    }
}
