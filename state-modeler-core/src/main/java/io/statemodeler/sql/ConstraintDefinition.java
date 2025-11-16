package io.statemodeler.sql;

import java.util.Objects;

/**
 * Abstract representation of a SQL constraint definition.
 */
public final class ConstraintDefinition {
    private final String name;
    private final String table;
    private final ConstraintType type;
    private final String definition;

    public ConstraintDefinition(String name, String table, ConstraintType type, String definition) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.table = Objects.requireNonNull(table, "table cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.definition = Objects.requireNonNull(definition, "definition cannot be null");
    }

    public String name() {
        return name;
    }

    public String table() {
        return table;
    }

    public ConstraintType type() {
        return type;
    }

    public String definition() {
        return definition;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ConstraintDefinition that = (ConstraintDefinition) obj;
        return Objects.equals(name, that.name)
                && Objects.equals(table, that.table)
                && type == that.type
                && Objects.equals(definition, that.definition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, table, type, definition);
    }

    @Override
    public String toString() {
        return "ConstraintDefinition{" + "name='" + name + '\'' + ", table='" + table + '\'' + ", type=" + type + '}';
    }

    /**
     * Types of SQL constraints.
     */
    public enum ConstraintType {
        CHECK,
        FOREIGN_KEY,
        UNIQUE,
        PRIMARY_KEY
    }
}
