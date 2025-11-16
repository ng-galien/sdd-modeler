package io.statemodeler.core;

import org.jspecify.annotations.Nullable;

/**
 * Database configuration for SQL generation.
 */
public record DatabaseConfig(String dialect, @Nullable String schema) {

    public DatabaseConfig {
        if (dialect == null) throw new IllegalArgumentException("dialect cannot be null");
        // schema can be null for default schema
    }
}
