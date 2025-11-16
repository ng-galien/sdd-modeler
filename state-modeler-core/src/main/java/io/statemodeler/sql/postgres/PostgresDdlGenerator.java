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
    public String generateDdl(SddModel model) {
        try {
            var plan = generateSqlPlan(model);
            return renderDdl(plan, model.database().schema(), model.database().effectiveStateSchema());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PostgreSQL DDL", e);
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
        var indexes = new ArrayList<IndexDefinition>();

        var entitySchema = model.database().schema();
        var stateSchema = model.database().effectiveStateSchema();

        // Generate entity tables and state tables for each entity
        for (var entity : model.entities().values()) {
            // Main entity table in entity schema
            tables.add(generateEntityTable(entity, entitySchema));

            // OR transition mapping tables in state schema (created WITHOUT FK to avoid circular dependency)
            for (var state : entity.states().values()) {
                if (state.hasOrTransitions()) {
                    var orTable = generateOrTransitionTable(entity, state, entitySchema, stateSchema);
                    tables.add(orTable);
                    indexes.addAll(generateIndexesForTable(orTable));
                    constraints.add(generateOrTransitionConstraint(entity, state, stateSchema));
                }
            }

            // State tables in state schema (created WITHOUT FK to avoid circular dependencies)
            for (var state : entity.states().values()) {
                var stateTable = generateStateTable(entity, state, entitySchema, stateSchema);
                tables.add(stateTable);
                indexes.addAll(generateIndexesForTable(stateTable));
            }

            // Extension tables in state schema (created WITHOUT FK to avoid circular dependencies)
            for (var extension : entity.extensions().values()) {
                var extensionTable = generateExtensionTable(entity, extension, entitySchema, stateSchema);
                tables.add(extensionTable);
                indexes.addAll(generateIndexesForTable(extensionTable));
            }

            // Add ALL FK constraints at the end (after all tables are created)
            // 1. FK from OR mapping tables to state tables
            for (var state : entity.states().values()) {
                if (state.hasOrTransitions()) {
                    var fkConstraints = generateOrTransitionForeignKeys(entity, state, stateSchema);
                    constraints.addAll(fkConstraints);
                    // Generate indexes for FK columns
                    for (var fk : fkConstraints) {
                        indexes.add(generateIndexForForeignKey(fk, entity, state.name() + "_source"));
                    }
                }
            }
            // 2. FK from state tables to entity and previous states
            for (var state : entity.states().values()) {
                var fkConstraints = generateStateForeignKeys(entity, state, entitySchema, stateSchema);
                constraints.addAll(fkConstraints);
                // Generate indexes for FK columns
                for (var fk : fkConstraints) {
                    indexes.add(generateIndexForForeignKey(fk, entity, state.table()));
                }
            }
            // 3. FK from extension tables to state tables
            for (var extension : entity.extensions().values()) {
                var fk = generateExtensionForeignKey(entity, extension, stateSchema);
                constraints.add(fk);
                // Generate index for FK column
                indexes.add(generateIndexForForeignKey(fk, entity, extension.table()));
            }

            // Projection views in state schema (intervals MUST be before current_state)
            entity.projections().values().stream()
                    .sorted((p1, p2) -> {
                        // intervals before current_state
                        if (p1.kind() == io.statemodeler.core.ProjectionDef.ProjectionKind.INTERVALS) return -1;
                        if (p2.kind() == io.statemodeler.core.ProjectionDef.ProjectionKind.INTERVALS) return 1;
                        return 0;
                    })
                    .forEach(projection ->
                            views.add(generateProjectionView(entity, projection, entitySchema, stateSchema)));
        }

        return new SqlPlan(tables, views, constraints, indexes);
    }

    /**
     * Render the SQL plan to PostgreSQL DDL.
     */
    private String renderDdl(SqlPlan plan, String entitySchema, String stateSchema) {
        var ddl = new StringBuilder();

        // Schema creation for entity schema if specified (non-null, non-empty, non-public)
        if (entitySchema != null && !entitySchema.isEmpty() && !entitySchema.equals("public")) {
            ddl.append("CREATE SCHEMA IF NOT EXISTS ").append(entitySchema).append(";\n\n");
        }

        // Schema creation for state schema (always create if different from public)
        if (stateSchema != null && !stateSchema.isEmpty() && !stateSchema.equals("public")) {
            ddl.append("CREATE SCHEMA IF NOT EXISTS ").append(stateSchema).append(";\n\n");
        }

        // Tables
        for (var table : plan.tables()) {
            ddl.append(renderTable(table)).append("\n\n");
        }

        // Constraints
        for (var constraint : plan.constraints()) {
            ddl.append(renderConstraint(constraint)).append("\n\n");
        }

        // Indexes
        for (var index : plan.indexes()) {
            ddl.append(renderIndex(index)).append("\n\n");
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

    private TableDefinition generateStateTable(
            EntityDef entity, StateDef state, String entitySchema, String stateSchema) {
        var columns = new ArrayList<ColumnDefinition>();

        // Primary key
        columns.add(new ColumnDefinition("id", "SERIAL", false, true, null, null, null));

        // Entity reference (FK added later as constraint)
        columns.add(new ColumnDefinition(entity.name() + "_id", "INTEGER", false, false, null, null, null));

        // Timestamp
        columns.add(new ColumnDefinition("created_at", "TIMESTAMPTZ", false, false, "NOW()", null, null));

        // Previous state references (for transitions)
        if (!state.initial()) {
            if (state.hasOrTransitions()) {
                // OR transitions use mapping table (FK added later as constraint)
                columns.add(new ColumnDefinition("previous_source_id", "INTEGER", false, false, null, null, null));
            } else {
                // Simple transitions (FK added later as constraint)
                for (var fromState : state.from()) {
                    columns.add(new ColumnDefinition(
                            "previous_" + fromState + "_id", "INTEGER", false, false, null, null, null));
                }
            }
        }

        // State-specific attributes
        for (var attr : state.attributes().values()) {
            columns.add(new ColumnDefinition(
                    attr.name(), attr.type(), attr.nullable(), attr.primaryKey(), attr.defaultValue(), null, null));
        }

        return new TableDefinition(state.table(), stateSchema, columns, List.of("id"));
    }

    private TableDefinition generateExtensionTable(
            EntityDef entity, ExtensionDef extension, String entitySchema, String stateSchema) {
        var columns = new ArrayList<ColumnDefinition>();

        // Primary key references the state table (FK added later as constraint)
        columns.add(new ColumnDefinition(extension.targetState() + "_id", "INTEGER", false, true, null, null, null));

        // Extension attributes
        for (var attr : extension.attributes().values()) {
            columns.add(new ColumnDefinition(
                    attr.name(), attr.type(), attr.nullable(), attr.primaryKey(), attr.defaultValue(), null, null));
        }

        // Updated timestamp for mutable extensions
        columns.add(new ColumnDefinition("updated_at", "TIMESTAMPTZ", false, false, "NOW()", null, null));

        return new TableDefinition(extension.table(), stateSchema, columns, List.of(extension.targetState() + "_id"));
    }

    private TableDefinition generateOrTransitionTable(
            EntityDef entity, StateDef state, String entitySchema, String stateSchema) {
        var columns = new ArrayList<ColumnDefinition>();

        // Primary key
        columns.add(new ColumnDefinition("id", "SERIAL", false, true, null, null, null));

        // References to possible source states (FK added separately to avoid circular dependency)
        for (var fromState : state.fromAnyOf()) {
            columns.add(new ColumnDefinition(
                    fromState + "_state_id",
                    "INTEGER",
                    true, // nullable - only one will be set
                    false,
                    null,
                    null, // No FK here - added later as constraint
                    null));
        }

        return new TableDefinition(state.name() + "_source", stateSchema, columns, List.of("id"));
    }

    private List<ConstraintDefinition> generateOrTransitionForeignKeys(
            EntityDef entity, StateDef state, String stateSchema) {
        var constraints = new ArrayList<ConstraintDefinition>();
        var tableName = stateSchema + "." + state.name() + "_source";

        // Add FK constraints to source state tables
        for (var fromState : state.fromAnyOf()) {
            var sourceState = entity.states().get(fromState);
            var constraintName = state.name() + "_source_" + fromState + "_fk";
            var fkDefinition = "FOREIGN KEY (" + fromState + "_state_id) REFERENCES " + stateSchema + "."
                    + sourceState.table() + "(id)";
            constraints.add(new ConstraintDefinition(
                    constraintName, tableName, ConstraintDefinition.ConstraintType.FOREIGN_KEY, fkDefinition));
        }

        return constraints;
    }

    private ConstraintDefinition generateOrTransitionConstraint(EntityDef entity, StateDef state, String stateSchema) {
        var constraintName = state.name() + "_source_check";
        var tableName = stateSchema + "." + state.name() + "_source";

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

    private List<ConstraintDefinition> generateStateForeignKeys(
            EntityDef entity, StateDef state, String entitySchema, String stateSchema) {
        var constraints = new ArrayList<ConstraintDefinition>();
        var tableName = stateSchema + "." + state.table();

        // FK to entity table
        var entityFkName = state.table() + "_" + entity.name() + "_id_fk";
        var entityFkDef =
                "FOREIGN KEY (" + entity.name() + "_id) REFERENCES " + entitySchema + "." + entity.table() + "(id)";
        constraints.add(new ConstraintDefinition(
                entityFkName, tableName, ConstraintDefinition.ConstraintType.FOREIGN_KEY, entityFkDef));

        // FK to previous states
        if (!state.initial()) {
            if (state.hasOrTransitions()) {
                // FK to OR mapping table
                var orFkName = state.table() + "_previous_source_id_fk";
                var orFkDef = "FOREIGN KEY (previous_source_id) REFERENCES " + stateSchema + "." + state.name()
                        + "_source(id)";
                constraints.add(new ConstraintDefinition(
                        orFkName, tableName, ConstraintDefinition.ConstraintType.FOREIGN_KEY, orFkDef));
            } else {
                // FK to previous state tables
                for (var fromState : state.from()) {
                    var fromStateTable = entity.states().get(fromState).table();
                    var prevFkName = state.table() + "_previous_" + fromState + "_id_fk";
                    var prevFkDef = "FOREIGN KEY (previous_" + fromState + "_id) REFERENCES " + stateSchema + "."
                            + fromStateTable + "(id)";
                    constraints.add(new ConstraintDefinition(
                            prevFkName, tableName, ConstraintDefinition.ConstraintType.FOREIGN_KEY, prevFkDef));
                }
            }
        }

        return constraints;
    }

    private ConstraintDefinition generateExtensionForeignKey(
            EntityDef entity, ExtensionDef extension, String stateSchema) {
        var tableName = stateSchema + "." + extension.table();
        var targetState = entity.states().get(extension.targetState());
        var fkName = extension.table() + "_" + extension.targetState() + "_id_fk";
        var fkDef = "FOREIGN KEY (" + extension.targetState() + "_id) REFERENCES " + stateSchema + "."
                + targetState.table() + "(id)";
        return new ConstraintDefinition(fkName, tableName, ConstraintDefinition.ConstraintType.FOREIGN_KEY, fkDef);
    }

    /**
     * Generate indexes for all foreign key columns in a table.
     * Creates one index per FK column to optimize JOIN performance.
     */
    private List<IndexDefinition> generateIndexesForTable(TableDefinition table) {
        var indexes = new ArrayList<IndexDefinition>();

        for (var column : table.columns()) {
            if (column.hasForeignKey()) {
                var indexName = "idx_" + table.name() + "_" + column.name();
                indexes.add(
                        new IndexDefinition(indexName, table.name(), table.schema(), List.of(column.name()), false));
            }
        }

        return indexes;
    }

    private IndexDefinition generateIndexForForeignKey(ConstraintDefinition fk, EntityDef entity, String tableName) {
        // Extract column name from FK definition: "FOREIGN KEY (column_name) REFERENCES ..."
        var fkDef = fk.definition();
        var startIdx = fkDef.indexOf('(') + 1;
        var endIdx = fkDef.indexOf(')');
        var columnName = fkDef.substring(startIdx, endIdx).trim();

        var indexName = "idx_" + tableName + "_" + columnName;
        var schema =
                fk.table().contains(".") ? fk.table().substring(0, fk.table().indexOf('.')) : "public";

        return new IndexDefinition(indexName, tableName, schema, List.of(columnName), false);
    }

    private ViewDefinition generateProjectionView(
            EntityDef entity, ProjectionDef projection, String entitySchema, String stateSchema) {
        return switch (projection.kind()) {
            case INTERVALS -> generateIntervalsView(entity, projection, entitySchema, stateSchema);
            case CURRENT_STATE -> generateCurrentStateView(entity, projection, stateSchema);
        };
    }

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
        sql.append("FROM ").append(schema).append(".").append(intervalsViewName).append("\n");
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

        if (column.primaryKey()) {
            def.append(" PRIMARY KEY");
        }

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

    private String renderIndex(IndexDefinition index) {
        var ddl = new StringBuilder();
        ddl.append("CREATE ");

        if (index.unique()) {
            ddl.append("UNIQUE ");
        }

        ddl.append("INDEX ").append(index.name());
        ddl.append(" ON ").append(index.fullTableName());
        ddl.append(" (").append(String.join(", ", index.columns())).append(");");

        return ddl.toString();
    }

    private String renderConstraint(ConstraintDefinition constraint) {
        return switch (constraint.type()) {
            case CHECK ->
                "ALTER TABLE " + constraint.table() + " ADD CONSTRAINT " + constraint.name() + " CHECK ("
                        + constraint.definition() + ");";
            case FOREIGN_KEY ->
                "ALTER TABLE " + constraint.table() + " ADD CONSTRAINT " + constraint.name() + " "
                        + constraint.definition() + ";";
            case UNIQUE ->
                "ALTER TABLE " + constraint.table() + " ADD CONSTRAINT " + constraint.name() + " UNIQUE ("
                        + constraint.definition() + ");";
            case PRIMARY_KEY ->
                "ALTER TABLE " + constraint.table() + " ADD CONSTRAINT " + constraint.name() + " PRIMARY KEY ("
                        + constraint.definition() + ");";
        };
    }

    private String renderView(ViewDefinition view) {
        return "CREATE VIEW " + view.fullName() + " AS\n" + view.query() + ";";
    }
}
