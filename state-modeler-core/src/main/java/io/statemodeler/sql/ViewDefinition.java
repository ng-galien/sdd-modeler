package io.statemodeler.sql;

import java.util.Objects;

/**
 * Abstract representation of a SQL view definition.
 */
public final class ViewDefinition {
    private final String name;
    private final String schema;
    private final String query;

    public ViewDefinition(String name, String schema, String query) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.schema = schema; // Can be null
        this.query = Objects.requireNonNull(query, "query cannot be null");
    }

    public String name() {
        return name;
    }

    public String schema() {
        return schema;
    }

    public String query() {
        return query;
    }

    public String fullName() {
        return schema != null ? schema + "." + name : name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ViewDefinition that = (ViewDefinition) obj;
        return Objects.equals(name, that.name)
                && Objects.equals(schema, that.schema)
                && Objects.equals(query, that.query);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, schema, query);
    }

    @Override
    public String toString() {
        return "ViewDefinition{" + "name='" + fullName() + '\'' + '}';
    }
}
