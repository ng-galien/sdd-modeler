package io.statemodeler.sql;

import java.util.Objects;

/**
 * Abstract representation of a SQL column definition.
 */
public final class ColumnDefinition {
    private final String name;
    private final String type;
    private final boolean nullable;
    private final boolean primaryKey;
    private final String defaultValue;
    private final String foreignKeyTable;
    private final String foreignKeyColumn;

    public ColumnDefinition(
            String name,
            String type,
            boolean nullable,
            boolean primaryKey,
            String defaultValue,
            String foreignKeyTable,
            String foreignKeyColumn) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.nullable = nullable;
        this.primaryKey = primaryKey;
        this.defaultValue = defaultValue; // Can be null
        this.foreignKeyTable = foreignKeyTable; // Can be null
        this.foreignKeyColumn = foreignKeyColumn; // Can be null
    }

    public String name() {
        return name;
    }

    public String type() {
        return type;
    }

    public boolean nullable() {
        return nullable;
    }

    public boolean primaryKey() {
        return primaryKey;
    }

    public String defaultValue() {
        return defaultValue;
    }

    public String foreignKeyTable() {
        return foreignKeyTable;
    }

    public String foreignKeyColumn() {
        return foreignKeyColumn;
    }

    public boolean hasForeignKey() {
        return foreignKeyTable != null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ColumnDefinition that = (ColumnDefinition) obj;
        return nullable == that.nullable
                && primaryKey == that.primaryKey
                && Objects.equals(name, that.name)
                && Objects.equals(type, that.type)
                && Objects.equals(defaultValue, that.defaultValue)
                && Objects.equals(foreignKeyTable, that.foreignKeyTable)
                && Objects.equals(foreignKeyColumn, that.foreignKeyColumn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, nullable, primaryKey, defaultValue, foreignKeyTable, foreignKeyColumn);
    }

    @Override
    public String toString() {
        return "ColumnDefinition{"
                + "name='" + name + '\''
                + ", type='" + type + '\''
                + ", nullable=" + nullable
                + ", primaryKey=" + primaryKey
                + '}';
    }
}
