package io.statemodeler.sql;

import org.jspecify.annotations.Nullable;

/**
 * Abstract representation of a SQL column definition.
 */
public record ColumnDefinition(
        String name,
        String type,
        boolean nullable,
        boolean primaryKey,
        @Nullable String defaultValue,
        @Nullable String foreignKeyTable,
        @Nullable String foreignKeyColumn) {

    public ColumnDefinition {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        if (type == null) throw new IllegalArgumentException("type cannot be null");
    }

    public boolean hasForeignKey() {
        return foreignKeyTable != null;
    }
}
