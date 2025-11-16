package io.statemodeler.sql;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Abstract representation of a SQL table definition.
 */
public record TableDefinition(
        String name, @Nullable String schema, List<ColumnDefinition> columns, List<String> primaryKey) {

    public TableDefinition {
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        Objects.requireNonNull(columns, "columns cannot be null");
        Objects.requireNonNull(primaryKey, "primaryKey cannot be null");

        columns = List.copyOf(columns);
        primaryKey = List.copyOf(primaryKey);
    }

    public String fullName() {
        return (schema != null && !schema.isEmpty()) ? schema + "." + name : name;
    }

    @Override
    public String toString() {
        return "TableDefinition{" + "name='" + fullName() + '\'' + ", columns=" + columns.size() + '}';
    }
}
