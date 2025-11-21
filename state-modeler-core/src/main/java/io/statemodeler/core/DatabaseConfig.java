package io.statemodeler.core;

import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Database configuration for SQL generation.
 * Supports separate schemas for entities and state tables to reinforce the State-Driven Design ADT.
 *
 * @param dialect SQL dialect (e.g., "postgres")
 * @param schema Schema for entity tables (can be null or empty for default schema)
 * @param stateSchema Schema for state tables, extensions, OR transitions, and projections.
 *                    If null or empty, defaults to {@code schema + "_states"} or "states" if schema is also null/empty.
 *                    Empty strings are treated the same as null.
 */
public record DatabaseConfig(
        String dialect, @Nullable String schema, @Nullable String stateSchema, Map<String, String> generatorOptions) {

    public DatabaseConfig {
        if (dialect == null) throw new IllegalArgumentException("dialect cannot be null");
        // schema and stateSchema can be null or empty for defaults
        if (generatorOptions == null) generatorOptions = Map.of();
    }

    /**
     * Get the effective state schema name.
     * Defaults to {@code schema + "_states"} if stateSchema is null/empty, or "states" if both are null/empty.
     * Empty strings are treated the same as null.
     */
    public String effectiveStateSchema() {
        if (stateSchema != null && !stateSchema.isEmpty()) {
            return stateSchema;
        }
        return (schema != null && !schema.isEmpty()) ? schema + "_states" : "states";
    }
}
