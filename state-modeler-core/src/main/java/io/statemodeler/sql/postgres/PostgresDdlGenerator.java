package io.statemodeler.sql.postgres;

import io.statemodeler.core.*;
import io.statemodeler.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PostgreSQL-specific DDL generator implementation.
 * Generates DDL following SDD patterns with PostgreSQL syntax.
 */
public final class PostgresDdlGenerator implements DdlGenerator {

    private static final String DIALECT = "postgres";

    @Override
    public String generateDdl(SddModel model) throws DdlGenerationException {
        try {
            var plan = generateSqlPlan(model);
            return renderDdl(plan, model.database().schema());
        } catch (Exception e) {
            throw new DdlGenerationException("Failed to generate PostgreSQL DDL", e);
        }
    }

    @Override
    public String getDialect() {
        return DIALECT;
    }

    /**
     * Generate an abstract SQL plan from the SDD model.
     */
    private SqlPlan generateSqlPlan(SddModel model) {
        var tables = new ArrayList<TableDefinition>();
        var views = new ArrayList<ViewDefinition>();
        var constraints = new ArrayList<ConstraintDefinition>();

        // Generate entity tables and state tables for each entity
        for (var entity : model.entities().values()) {
            // Main entity table
            tables.add(generateEntityTable(entity, model.database().schema()));

            // State tables
            for (var state : entity.states().values()) {
                tables.add(generateStateTable(entity, state, model.database().schema()));
            }

            // Extension tables
            for (var extension : entity.extensions().values()) {
                tables.add(generateExtensionTable(
                        entity, extension, model.database().schema()));
            }

            // OR transition mapping tables
            for (var state : entity.states().values()) {
                if (state.hasOrTransitions()) {
                    tables.add(generateOrTransitionTable(
                            entity, state, model.database().schema()));
                    constraints.add(generateOrTransitionConstraint(entity, state));
                }
            }

            // Projection views
            for (var projection : entity.projections().values()) {
                views.add(generateProjectionView(
                        entity, projection, model.database().schema()));
            }
        }

        return new SqlPlan(tables, views, constraints);
    }

    /**
     * Render the SQL plan to PostgreSQL DDL.
     */
    private String renderDdl(SqlPlan plan, String schema) {
        var ddl = new StringBuilder();

        // Schema creation if specified
        if (schema != null && !schema.equals("public")) {
            ddl.append("CREATE SCHEMA IF NOT EXISTS ").append(schema).append(";\n\n");
        }

        // Tables
        for (var table : plan.tables()) {
            ddl.append(renderTable(table)).append("\n\n");
        }

        // Constraints
        for (var constraint : plan.constraints()) {
            ddl.append(renderConstraint(constraint)).append("\n\n");
        }

        // Views
        for (var view : plan.views()) {
            ddl.append(renderView(view)).append("\n\n");
        }

        return ddl.toString().trim();
    }

    private TableDefinition generateEntityTable(EntityDef entity, String schema) {
        var columns = new ArrayList<ColumnDefinition>();

        // Add ID column
        columns.add(new ColumnDefinition(
                entity.id().name(),
                entity.id().type(),
                entity.id().nullable(),
                entity.id().primaryKey(),
                entity.id().defaultValue(),
                null,
                null));

        // Add entity attributes
        for (var attr : entity.attributes().values()) {
            columns.add(new ColumnDefinition(
                    attr.name(), attr.type(), attr.nullable(), attr.primaryKey(), attr.defaultValue(), null, null));
        }

        return new TableDefinition(
                entity.table(), schema, columns, List.of(entity.id().name()));
    }

    private TableDefinition generateStateTable(EntityDef entity, StateDef state, String schema) {
        var columns = new ArrayList<ColumnDefinition>();

        // Primary key
        columns.add(new ColumnDefinition("id", "SERIAL", false, true, null, null, null));

        // Entity reference
        columns.add(new ColumnDefinition(
                entity.name() + "_id",
                "INTEGER",
                false,
                false,
                null,
                entity.table(),
                entity.id().name()));

        // Timestamp
        columns.add(new ColumnDefinition("created_at", "TIMESTAMPTZ", false, false, "NOW()", null, null));

        // Previous state references (for transitions)
        if (!state.initial()) {
            if (state.hasOrTransitions()) {
                // OR transitions use mapping table
                columns.add(new ColumnDefinition(
                        "previous_source_id", "INTEGER", false, false, null, state.name() + "_source", "id"));
            } else {
                // Simple transitions
                for (var fromState : state.from()) {
                    columns.add(new ColumnDefinition(
                            "previous_" + fromState + "_id",
                            "INTEGER",
                            false,
                            false,
                            null,
                            entity.name() + "_" + fromState,
                            "id"));
                }
            }
        }

        // State-specific attributes
        for (var attr : state.attributes().values()) {
            columns.add(new ColumnDefinition(
                    attr.name(), attr.type(), attr.nullable(), attr.primaryKey(), attr.defaultValue(), null, null));
        }

        return new TableDefinition(state.table(), schema, columns, List.of("id"));
    }

    private TableDefinition generateExtensionTable(EntityDef entity, ExtensionDef extension, String schema) {
        var columns = new ArrayList<ColumnDefinition>();

        // Primary key references the state table
        var targetState = entity.states().get(extension.targetState());
        columns.add(new ColumnDefinition(
                extension.targetState() + "_id", "INTEGER", false, true, null, targetState.table(), "id"));

        // Extension attributes
        for (var attr : extension.attributes().values()) {
            columns.add(new ColumnDefinition(
                    attr.name(), attr.type(), attr.nullable(), attr.primaryKey(), attr.defaultValue(), null, null));
        }

        // Updated timestamp for mutable extensions
        columns.add(new ColumnDefinition("updated_at", "TIMESTAMPTZ", false, false, "NOW()", null, null));

        return new TableDefinition(extension.table(), schema, columns, List.of(extension.targetState() + "_id"));
    }

    private TableDefinition generateOrTransitionTable(EntityDef entity, StateDef state, String schema) {
        var columns = new ArrayList<ColumnDefinition>();

        // Primary key
        columns.add(new ColumnDefinition("id", "SERIAL", false, true, null, null, null));

        // References to possible source states
        for (var fromState : state.fromAnyOf()) {
            var sourceState = entity.states().get(fromState);
            columns.add(new ColumnDefinition(
                    fromState + "_state_id",
                    "INTEGER",
                    true, // nullable - only one will be set
                    false,
                    null,
                    sourceState.table(),
                    "id"));
        }

        return new TableDefinition(state.name() + "_source", schema, columns, List.of("id"));
    }

    private ConstraintDefinition generateOrTransitionConstraint(EntityDef entity, StateDef state) {
        var constraintName = state.name() + "_source_check";
        var tableName = state.name() + "_source";

        // Generate CHECK constraint ensuring exactly one source is set
        var conditions = state.fromAnyOf().stream()
                .map(fromState -> fromState + "_state_id IS NOT NULL")
                .collect(Collectors.joining(" OR "));

        var exclusiveConditions = new ArrayList<String>();
        for (int i = 0; i < state.fromAnyOf().size(); i++) {
            for (int j = i + 1; j < state.fromAnyOf().size(); j++) {
                var state1 = state.fromAnyOf().get(i);
                var state2 = state.fromAnyOf().get(j);
                exclusiveConditions.add(
                        "NOT (" + state1 + "_state_id IS NOT NULL AND " + state2 + "_state_id IS NOT NULL)");
            }
        }

        var definition = "(" + conditions + ") AND " + String.join(" AND ", exclusiveConditions);

        return new ConstraintDefinition(
                constraintName, tableName, ConstraintDefinition.ConstraintType.CHECK, definition);
    }

    private ViewDefinition generateProjectionView(EntityDef entity, ProjectionDef projection, String schema) {
        return switch (projection.kind()) {
            case INTERVALS -> generateIntervalsView(entity, projection, schema);
            case CURRENT_STATE -> generateCurrentStateView(entity, projection, schema);
        };
    }

    private ViewDefinition generateIntervalsView(EntityDef entity, ProjectionDef projection, String schema) {
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
                            "(SELECT MIN(created_at) FROM %s WHERE previous_%s_id = %s.id)",
                            nextState.table(), stateName, stateAlias);
                    endAtClauses.add(minClause);
                }
            }

            // Find states that can follow via OR transitions
            for (var nextState : entity.states().values()) {
                if (nextState.hasOrTransitions() && nextState.fromAnyOf().contains(stateName)) {
                    var sourceTable = nextState.name() + "_source";
                    var sourceColumn = stateName + "_state_id";
                    var minClause = String.format(
                            "(SELECT MIN(ns.created_at) FROM %s ns JOIN %s src ON src.id = ns.previous_source_id WHERE src.%s = %s.id)",
                            nextState.table(), sourceTable, sourceColumn, stateAlias);
                    endAtClauses.add(minClause);
                }
            }

            if (endAtClauses.isEmpty()) {
                // Final state - no transitions out
                part.append("    NULL AS end_at\n");
            } else {
                // Calculate minimum of all possible next states
                part.append("    COALESCE(\n");
                part.append("        LEAST(");
                part.append(String.join(", ", endAtClauses));
                part.append("),\n");
                part.append("        NULL\n");
                part.append("    ) AS end_at\n");
            }

            part.append("FROM ").append(entity.table()).append(" e\n");
            part.append("JOIN ")
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

        return new ViewDefinition(projection.viewName(), schema, sql.toString());
    }

    private ViewDefinition generateCurrentStateView(EntityDef entity, ProjectionDef projection, String schema) {
        // Current state view is simple: filter intervals where end_at IS NULL
        var intervalsViewName = findIntervalsViewName(entity);
        if (intervalsViewName == null) {
            // Fallback if no intervals view found
            intervalsViewName = entity.name() + "_state_intervals";
        }

        var sql = new StringBuilder();
        sql.append("SELECT\n");
        sql.append("    ").append(entity.name()).append("_id,\n");
        sql.append("    state_type,\n");
        sql.append("    start_at\n");
        sql.append("FROM ").append(intervalsViewName).append("\n");
        sql.append("WHERE end_at IS NULL");

        return new ViewDefinition(projection.viewName(), schema, sql.toString());
    }

    /**
     * Find the intervals view name for an entity (if defined in projections).
     */
    private String findIntervalsViewName(EntityDef entity) {
        for (var projection : entity.projections().values()) {
            if (projection.kind() == ProjectionDef.ProjectionKind.INTERVALS) {
                return projection.viewName();
            }
        }
        return null;
    }

    private String renderTable(TableDefinition table) {
        var ddl = new StringBuilder();
        ddl.append("CREATE TABLE ").append(table.fullName()).append(" (\n");

        var columnDefs = table.columns().stream().map(this::renderColumn).collect(Collectors.joining(",\n"));

        ddl.append("    ").append(columnDefs.replace("\n", "\n    "));
        ddl.append("\n);");

        return ddl.toString();
    }

    private String renderColumn(ColumnDefinition column) {
        var def = new StringBuilder();
        def.append(column.name()).append(" ").append(column.type());

        if (!column.nullable()) {
            def.append(" NOT NULL");
        }

        if (column.defaultValue() != null) {
            def.append(" DEFAULT ").append(column.defaultValue());
        }

        if (column.hasForeignKey()) {
            def.append(" REFERENCES ")
                    .append(column.foreignKeyTable())
                    .append("(")
                    .append(column.foreignKeyColumn())
                    .append(")");
        }

        return def.toString();
    }

    private String renderConstraint(ConstraintDefinition constraint) {
        return "ALTER TABLE " + constraint.table() + " ADD CONSTRAINT " + constraint.name() + " CHECK "
                + constraint.definition() + ";";
    }

    private String renderView(ViewDefinition view) {
        return "CREATE VIEW " + view.fullName() + " AS\n" + view.query() + ";";
    }
}
