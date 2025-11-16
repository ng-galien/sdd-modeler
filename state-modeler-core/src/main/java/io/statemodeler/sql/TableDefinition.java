package io.statemodeler.sql;

import java.util.List;
import java.util.Objects;

/**
 * Abstract representation of a SQL table definition.
 */
public final class TableDefinition {
    private final String name;
    private final String schema;
    private final List<ColumnDefinition> columns;
    private final List<String> primaryKey;

    public TableDefinition(String name, String schema, List<ColumnDefinition> columns, List<String> primaryKey) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.schema = schema; // Can be null
        this.columns = List.copyOf(Objects.requireNonNull(columns, "columns cannot be null"));
        this.primaryKey = List.copyOf(Objects.requireNonNull(primaryKey, "primaryKey cannot be null"));
    }

    public String name() {
        return name;
    }

    public String schema() {
        return schema;
    }

    public List<ColumnDefinition> columns() {
        return columns;
    }

    public List<String> primaryKey() {
        return primaryKey;
    }

    public String fullName() {
        return schema != null ? schema + "." + name : name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TableDefinition that = (TableDefinition) obj;
        return Objects.equals(name, that.name)
                && Objects.equals(schema, that.schema)
                && Objects.equals(columns, that.columns)
                && Objects.equals(primaryKey, that.primaryKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, schema, columns, primaryKey);
    }

    @Override
    public String toString() {
        return "TableDefinition{" + "name='" + fullName() + '\'' + ", columns=" + columns.size() + '}';
    }
}
