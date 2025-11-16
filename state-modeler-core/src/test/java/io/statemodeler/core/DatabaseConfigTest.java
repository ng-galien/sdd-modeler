package io.statemodeler.core;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DatabaseConfigTest {

    @Test
    void shouldCreateValidDatabaseConfig() {
        // Given & When
        var config = new DatabaseConfig("postgres", "public");

        // Then
        assertThat(config.dialect()).isEqualTo("postgres");
        assertThat(config.schema()).isEqualTo("public");
    }

    @Test
    void shouldCreateConfigWithNullSchema() {
        // Given & When (schema can be null for default schema)
        var config = new DatabaseConfig("mysql", null);

        // Then
        assertThat(config.dialect()).isEqualTo("mysql");
        assertThat(config.schema()).isNull();
    }

    @Test
    void shouldRejectNullDialect() {
        assertThatThrownBy(() -> new DatabaseConfig(null, "public"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dialect cannot be null");
    }

    @Test
    void shouldImplementEqualsAndHashCode() {
        // Given
        var config1 = new DatabaseConfig("postgres", "public");
        var config2 = new DatabaseConfig("postgres", "public");
        var config3 = new DatabaseConfig("mysql", "public");
        var config4 = new DatabaseConfig("postgres", "private");

        // Then
        assertThat(config1).isEqualTo(config2);
        assertThat(config1).isNotEqualTo(config3);
        assertThat(config1).isNotEqualTo(config4);
        assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
    }

    @Test
    void shouldImplementToString() {
        // Given
        var config = new DatabaseConfig("postgres", "public");

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
        var postgres = new DatabaseConfig("postgres", "public");
        var mysql = new DatabaseConfig("mysql", "test");
        var sqlite = new DatabaseConfig("sqlite", null);
        var sqlserver = new DatabaseConfig("sqlserver", "dbo");

        assertThat(postgres.dialect()).isEqualTo("postgres");
        assertThat(mysql.dialect()).isEqualTo("mysql");
        assertThat(sqlite.dialect()).isEqualTo("sqlite");
        assertThat(sqlserver.dialect()).isEqualTo("sqlserver");
    }

    @Test
    void shouldHandleEmptyStringSchema() {
        // Given & When
        var config = new DatabaseConfig("postgres", "");

        // Then
        assertThat(config.schema()).isEmpty();
        assertThat(config.dialect()).isEqualTo("postgres");
    }
}
