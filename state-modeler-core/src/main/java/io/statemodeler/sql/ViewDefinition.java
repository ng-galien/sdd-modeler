package io.statemodeler.sql;

/**
 * Abstract representation of a SQL view definition.
 */
public record ViewDefinition(String name, String schema, String query) {

    public ViewDefinition {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        if (query == null) throw new IllegalArgumentException("query cannot be null");
    }

    public String fullName() {
        return schema != null ? schema + "." + name : name;
    }
}
