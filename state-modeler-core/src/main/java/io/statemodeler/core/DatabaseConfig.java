package io.statemodeler.core;

import org.jspecify.annotations.Nullable;

/**
 * Database configuration for SQL generation.
 * Supports separate schemas for entities and state tables to reinforce the State-Driven Design ADT.
 *
 * @param dialect SQL dialect (e.g., "postgres")
 * @param schema Schema for entity tables (can be null for default schema)
 * @param stateSchema Schema for state tables, extensions, OR transitions, and projections.
 *                    If null, defaults to {@code schema + "_states"} or "states" if schema is null.
 */
public record DatabaseConfig(
        String dialect, @Nullable String schema, @Nullable String stateSchema) {

    public DatabaseConfig {
        if (dialect == null) throw new IllegalArgumentException("dialect cannot be null");
        // schema and stateSchema can be null for defaults
    }

    /**
     * Get the effective state schema name.
     * Defaults to {@code schema + "_states"} if stateSchema is null, or "states" if both are null.
     */
    public String effectiveStateSchema() {
        if (stateSchema != null) {
            return stateSchema;
        }
        return schema != null ? schema + "_states" : "states";
    }
}
