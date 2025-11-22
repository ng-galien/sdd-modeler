package io.statemodeler.sql.postgres;

import io.statemodeler.core.EntityDef;
import io.statemodeler.core.ProjectionDef;
import io.statemodeler.sql.ViewDefinition;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Generates PostgreSQL view definitions for SDD projections (intervals and current_state).
 */
final class PostgresViewGenerator {

    /**
     * Generate projection view based on projection kind.
     *
     * @param entity entity definition
     * @param projection projection definition
     * @param entitySchema schema for entity tables
     * @param stateSchema schema for state tables and views
     * @return view definition
     */
    ViewDefinition generateProjectionView(
            EntityDef entity, ProjectionDef projection, String entitySchema, String stateSchema) {
        return switch (projection.kind()) {
            case INTERVALS -> generateIntervalsView(entity, projection, entitySchema, stateSchema);
            case CURRENT_STATE -> generateCurrentStateView(entity, projection, stateSchema);
        };
    }

    /**
     * Generate intervals view showing state timeline with start_at and end_at.
     * Uses UNION ALL across all state tables, calculating end_at with LEAD() logic.
     *
     * @param entity entity definition
     * @param projection projection definition
     * @param entitySchema schema for entity tables
     * @param stateSchema schema for state tables
     * @return view definition for intervals
     */
    private ViewDefinition generateIntervalsView(
            EntityDef entity, ProjectionDef projection, String entitySchema, String stateSchema) {
        var sql = new StringBuilder();
        var entityIdColumn = entity.name() + "_id";
        var unionParts = new ArrayList<String>();

        // Generate a UNION ALL part for each state
        for (var stateEntry : entity.states().entrySet()) {
            var stateName = stateEntry.getKey();
            var state = stateEntry.getValue();
            var stateTable = state.table();
            var stateAlias = stateName.substring(0, Math.min(3, stateName.length()));

            var part = new StringBuilder();
            part.append("SELECT\n");
            part.append("    ")
                    .append("e.")
                    .append(entity.id().name())
                    .append(" AS ")
                    .append(entityIdColumn)
                    .append(",\n");
            part.append("    '").append(stateName.toUpperCase()).append("' AS state_type,\n");
            part.append("    ").append(stateAlias).append(".created_at AS start_at,\n");

            // Calculate end_at based on transitions FROM this state
            var endAtClauses = new ArrayList<String>();

            // Find all states that can follow this state (simple transitions)
            for (var nextState : entity.states().values()) {
                if (nextState.from().contains(stateName)) {
                    var minClause = String.format(
                            "(SELECT MIN(created_at) FROM %s.%s WHERE previous_%s_id = %s.id)",
                            stateSchema, nextState.table(), stateName, stateAlias);
                    endAtClauses.add(minClause);
                }
            }

            // Find states that can follow via OR transitions
            for (var nextState : entity.states().values()) {
                if (nextState.hasOrTransitions() && nextState.fromAnyOf().contains(stateName)) {
                    var sourceTable = nextState.name() + "_source";
                    var sourceColumn = stateName + "_state_id";
                    var minClause = String.format(
                            "(SELECT MIN(ns.created_at) FROM %s.%s ns JOIN %s.%s src ON src.id = ns.previous_source_id WHERE src.%s = %s.id)",
                            stateSchema, nextState.table(), stateSchema, sourceTable, sourceColumn, stateAlias);
                    endAtClauses.add(minClause);
                }
            }

            if (endAtClauses.isEmpty()) {
                // Final state - no transitions out
                part.append("    NULL::TIMESTAMPTZ AS end_at\n");
            } else {
                // Calculate minimum of all possible next states
                part.append("    COALESCE(\n");
                part.append("        LEAST(");
                part.append(String.join(", ", endAtClauses));
                part.append("),\n");
                part.append("        NULL\n");
                part.append("    ) AS end_at\n");
            }

            part.append("FROM ")
                    .append(entitySchema)
                    .append(".")
                    .append(entity.table())
                    .append(" e\n");
            part.append("JOIN ")
                    .append(stateSchema)
                    .append(".")
                    .append(stateTable)
                    .append(" ")
                    .append(stateAlias)
                    .append(" ON ")
                    .append(stateAlias)
                    .append(".")
                    .append(entityIdColumn)
                    .append(" = e.")
                    .append(entity.id().name());

            unionParts.add(part.toString());
        }

        sql.append(String.join("\n\nUNION ALL\n\n", unionParts));

        return new ViewDefinition(projection.viewName(), stateSchema, sql.toString());
    }

    /**
     * Generate current_state view filtering intervals WHERE end_at IS NULL.
     * Depends on intervals view being created first.
     *
     * @param entity entity definition
     * @param projection projection definition
     * @param schema schema for views
     * @return view definition for current_state
     */
    private ViewDefinition generateCurrentStateView(EntityDef entity, ProjectionDef projection, String schema) {
        // Current state view is simple: filter intervals where end_at IS NULL
        var intervalsViewName = findIntervalsViewName(entity).orElse(entity.name() + "_state_intervals");

        var sql = new StringBuilder();
        sql.append("SELECT\n");
        sql.append("    ").append(entity.name()).append("_id,\n");
        sql.append("    state_type,\n");
        sql.append("    start_at\n");
        sql.append("FROM ").append(schema).append(".").append(intervalsViewName).append("\n");
        sql.append("WHERE end_at IS NULL");

        return new ViewDefinition(projection.viewName(), schema, sql.toString());
    }

    /**
     * Find the intervals view name for an entity (if defined in projections).
     *
     * @param entity entity definition
     * @return Optional containing the intervals view name if present, otherwise Optional.empty()
     */
    private Optional<String> findIntervalsViewName(EntityDef entity) {
        for (var projection : entity.projections().values()) {
            if (projection.kind() == ProjectionDef.ProjectionKind.INTERVALS) {
                return Optional.of(projection.viewName());
            }
        }
        return Optional.empty();
    }
}
