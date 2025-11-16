package io.statemodeler.sql;

import org.jspecify.annotations.Nullable;

/**
 * Abstract representation of a SQL view definition.
 */
public record ViewDefinition(String name, @Nullable String schema, String query) {

    public ViewDefinition {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        if (query == null) throw new IllegalArgumentException("query cannot be null");
    }

    public String fullName() {
        return (schema != null && !schema.isEmpty()) ? schema + "." + name : name;
    }
}
