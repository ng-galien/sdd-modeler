package io.statemodeler.sql;

import java.util.List;
import java.util.Objects;

/**
 * Represents an abstract SQL plan containing tables, constraints, views, and
 * indexes.
 * This is dialect-agnostic and can be rendered to specific SQL dialects.
 */
public record SqlPlan(
        List<TableDefinition> tables,
        List<ViewDefinition> views,
        List<ConstraintDefinition> constraints,
        List<IndexDefinition> indexes,
        List<FunctionDefinition> functions,
        List<TriggerDefinition> triggers) {

    public SqlPlan {
        tables = List.copyOf(Objects.requireNonNull(tables, "tables cannot be null"));
        views = List.copyOf(Objects.requireNonNull(views, "views cannot be null"));
        constraints = List.copyOf(Objects.requireNonNull(constraints, "constraints cannot be null"));
        indexes = List.copyOf(Objects.requireNonNull(indexes, "indexes cannot be null"));
        functions = List.copyOf(Objects.requireNonNull(functions, "functions cannot be null"));
        triggers = List.copyOf(Objects.requireNonNull(triggers, "triggers cannot be null"));
    }

    @Override
    public String toString() {
        return "SqlPlan{"
                + "tables=" + tables.size()
                + ", views=" + views.size()
                + ", constraints=" + constraints.size()
                + ", indexes=" + indexes.size()
                + ", functions=" + functions.size()
                + ", triggers=" + triggers.size()
                + '}';
    }
}
