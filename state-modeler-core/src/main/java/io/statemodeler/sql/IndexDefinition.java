package io.statemodeler.sql;

import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Abstract representation of a SQL index definition.
 * Indexes improve query performance, especially on foreign key columns.
 *
 * @param name Index name (e.g., "idx_order_paid_order_id")
 * @param table Table name the index is on
 * @param schema Schema for the table (can be null for default schema)
 * @param columns List of column names to index
 * @param unique Whether this is a unique index
 */
public record IndexDefinition(
        String name, String table, @Nullable String schema, List<String> columns, boolean unique) {

    public IndexDefinition {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        if (table == null) throw new IllegalArgumentException("table cannot be null");
        Objects.requireNonNull(columns, "columns cannot be null");
        if (columns.isEmpty()) throw new IllegalArgumentException("columns cannot be empty");

        columns = List.copyOf(columns);
    }

    /**
     * Get the full table name (schema-qualified if schema is present).
     */
    public String fullTableName() {
        return (schema != null && !schema.isEmpty()) ? schema + "." + table : table;
    }
}
