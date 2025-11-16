package io.statemodeler.core;

import java.util.Objects;

/**
 * Database configuration for SQL generation.
 */
public final class DatabaseConfig {
    private final String dialect;
    private final String schema;

    public DatabaseConfig(String dialect, String schema) {
        this.dialect = Objects.requireNonNull(dialect, "dialect cannot be null");
        this.schema = schema; // Can be null for default schema
    }

    public String dialect() {
        return dialect;
    }

    public String schema() {
        return schema;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DatabaseConfig that = (DatabaseConfig) obj;
        return Objects.equals(dialect, that.dialect) && Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dialect, schema);
    }

    @Override
    public String toString() {
        return "DatabaseConfig{" + "dialect='" + dialect + '\'' + ", schema='" + schema + '\'' + '}';
    }
}
