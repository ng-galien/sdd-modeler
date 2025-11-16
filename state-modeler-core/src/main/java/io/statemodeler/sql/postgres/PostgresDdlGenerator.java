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
        // This is a complex view - simplified implementation for now
        var query = "SELECT 'TODO' AS implementation_needed";
        return new ViewDefinition(projection.viewName(), schema, query);
    }

    private ViewDefinition generateCurrentStateView(EntityDef entity, ProjectionDef projection, String schema) {
        // This is a complex view - simplified implementation for now
        var query = "SELECT 'TODO' AS implementation_needed";
        return new ViewDefinition(projection.viewName(), schema, query);
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
