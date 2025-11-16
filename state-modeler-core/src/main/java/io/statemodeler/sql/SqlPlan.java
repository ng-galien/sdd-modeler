package io.statemodeler.sql;

import java.util.List;
import java.util.Objects;

/**
 * Represents an abstract SQL plan containing tables, constraints, and views.
 * This is dialect-agnostic and can be rendered to specific SQL dialects.
 */
public record SqlPlan(
        List<TableDefinition> tables,
        List<ViewDefinition> views,
        List<ConstraintDefinition> constraints) {

    public SqlPlan {
        tables = List.copyOf(Objects.requireNonNull(tables, "tables cannot be null"));
        views = List.copyOf(Objects.requireNonNull(views, "views cannot be null"));
        constraints = List.copyOf(Objects.requireNonNull(constraints, "constraints cannot be null"));
    }

    @Override
    public String toString() {
        return "SqlPlan{"
                + "tables=" + tables.size()
                + ", views=" + views.size()
                + ", constraints=" + constraints.size()
                + '}';
    }
}
