package io.statemodeler.sql;

/**
 * Abstract representation of a SQL constraint definition.
 */
public record ConstraintDefinition(String name, String table, ConstraintType type, String definition) {

    public ConstraintDefinition {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        if (table == null) throw new IllegalArgumentException("table cannot be null");
        if (type == null) throw new IllegalArgumentException("type cannot be null");
        if (definition == null) throw new IllegalArgumentException("definition cannot be null");
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
