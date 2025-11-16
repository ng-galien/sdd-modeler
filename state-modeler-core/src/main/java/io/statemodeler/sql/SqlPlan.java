package io.statemodeler.sql;

import java.util.List;
import java.util.Objects;

/**
 * Represents an abstract SQL plan containing tables, constraints, and views.
 * This is dialect-agnostic and can be rendered to specific SQL dialects.
 */
public final class SqlPlan {
    private final List<TableDefinition> tables;
    private final List<ViewDefinition> views;
    private final List<ConstraintDefinition> constraints;

    public SqlPlan(List<TableDefinition> tables, List<ViewDefinition> views, List<ConstraintDefinition> constraints) {
        this.tables = List.copyOf(Objects.requireNonNull(tables, "tables cannot be null"));
        this.views = List.copyOf(Objects.requireNonNull(views, "views cannot be null"));
        this.constraints = List.copyOf(Objects.requireNonNull(constraints, "constraints cannot be null"));
    }

    public List<TableDefinition> tables() {
        return tables;
    }

    public List<ViewDefinition> views() {
        return views;
    }

    public List<ConstraintDefinition> constraints() {
        return constraints;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SqlPlan sqlPlan = (SqlPlan) obj;
        return Objects.equals(tables, sqlPlan.tables)
                && Objects.equals(views, sqlPlan.views)
                && Objects.equals(constraints, sqlPlan.constraints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tables, views, constraints);
    }
}
