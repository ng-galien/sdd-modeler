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
    private final PostgresTableGenerator tableGenerator;

    public PostgresDdlGenerator() {
        this.tableGenerator = new PostgresTableGenerator();
    }

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
            tables.add(tableGenerator.generateEntityTable(entity, entitySchema));

            // OR transition mapping tables in state schema (created WITHOUT FK to avoid circular dependency)
            for (var state : entity.states().values()) {
                if (state.hasOrTransitions()) {
                    var orTable = tableGenerator.generateOrTransitionTable(entity, state, entitySchema, stateSchema);
                    tables.add(orTable);
                    constraints.add(generateOrTransitionConstraint(entity, state, stateSchema));
                }
            }

            // State tables in state schema (created WITHOUT FK to avoid circular dependencies)
            for (var state : entity.states().values()) {
                var stateTable = tableGenerator.generateStateTable(entity, state, entitySchema, stateSchema);
                tables.add(stateTable);
            }

            // Extension tables in state schema (created WITHOUT FK to avoid circular dependencies)
            for (var extension : entity.extensions().values()) {
                var extensionTable =
                        tableGenerator.generateExtensionTable(entity, extension, entitySchema, stateSchema);
                tables.add(extensionTable);
            }

            // Add constraints in dependency order:
            // STEP 1: Add UNIQUE constraints first (they become targets for composite FKs)
            for (var state : entity.states().values()) {
                var uniqueConstraints = generateStateUniqueConstraints(entity, state, stateSchema);
                constraints.addAll(uniqueConstraints);
            }
            for (var state : entity.states().values()) {
                if (state.hasOrTransitions()) {
                    var uniqueConstraints = generateOrTransitionUniqueConstraints(entity, state, stateSchema);
                    constraints.addAll(uniqueConstraints);
                }
            }

            // STEP 2: Add FK constraints (after UNIQUE constraints are created)
            // 2a. FK from OR mapping tables to entity and state tables
            for (var state : entity.states().values()) {
                if (state.hasOrTransitions()) {
                    var fkConstraints = generateOrTransitionForeignKeys(entity, state, entitySchema, stateSchema);
                    constraints.addAll(fkConstraints);
                    // Generate indexes for FK columns
                    for (var fk : fkConstraints) {
                        indexes.add(generateIndexForForeignKey(fk, entity, state.name() + "_source"));
                    }
                }
            }
            // 2b. FK from state tables to entity and previous states
            for (var state : entity.states().values()) {
                var fkConstraints = generateStateForeignKeys(entity, state, entitySchema, stateSchema);
                constraints.addAll(fkConstraints);
                // Generate indexes for FK columns (skip entity_id FK - UNIQUE constraint creates implicit index)
                for (var fk : fkConstraints) {
                    if (fk.type() == ConstraintDefinition.ConstraintType.FOREIGN_KEY
                            && !fk.name().contains("_" + entity.name() + "_id_fk")) {
                        indexes.add(generateIndexForForeignKey(fk, entity, state.table()));
                    }
                }
            }
            // 2c. FK from extension tables to state tables
            for (var extension : entity.extensions().values()) {
                var fk = generateExtensionForeignKey(entity, extension, stateSchema);
                constraints.add(fk);
                // Generate index for FK column
                indexes.add(generateIndexForForeignKey(fk, entity, extension.table()));
            }

            // Projection views in state schema (intervals MUST be before current_state)
            entity.projections().values().stream()
                    .sorted((p1, p2) -> {
                        // intervals before current_state (current_state depends on intervals view)
                        boolean p1IsIntervals =
                                p1.kind() == io.statemodeler.core.ProjectionDef.ProjectionKind.INTERVALS;
                        boolean p2IsIntervals =
                                p2.kind() == io.statemodeler.core.ProjectionDef.ProjectionKind.INTERVALS;
                        if (p1IsIntervals && !p2IsIntervals) return -1;
                        if (!p1IsIntervals && p2IsIntervals) return 1;
                        return p1.name().compareTo(p2.name()); // stable sort for same kind
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

    private List<ConstraintDefinition> generateOrTransitionForeignKeys(
            EntityDef entity, StateDef state, String entitySchema, String stateSchema) {
        var constraints = new ArrayList<ConstraintDefinition>();
        var tableName = stateSchema + "." + state.name() + "_source";

        // Add FK to entity table first
        var entityFkName = state.name() + "_source_" + entity.name() + "_id_fk";
        var entityFkDef =
                "FOREIGN KEY (" + entity.name() + "_id) REFERENCES " + entitySchema + "." + entity.table() + "(id)";
        constraints.add(new ConstraintDefinition(
                entityFkName, tableName, ConstraintDefinition.ConstraintType.FOREIGN_KEY, entityFkDef));

        // Add composite FK constraints to source state tables
        // (x_state_id, entity_id) -> (id, entity_id) ensures sources are from same aggregate
        for (var fromState : state.fromAnyOf()) {
            var sourceState = entity.states().get(fromState);
            var constraintName = state.name() + "_source_" + fromState + "_fk";
            var fkDefinition = "FOREIGN KEY (" + fromState + "_state_id, " + entity.name() + "_id) REFERENCES "
                    + stateSchema + "." + sourceState.table() + "(id, " + entity.name() + "_id)";
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

    private List<ConstraintDefinition> generateStateUniqueConstraints(
            EntityDef entity, StateDef state, String stateSchema) {
        var constraints = new ArrayList<ConstraintDefinition>();
        var tableName = stateSchema + "." + state.table();

        // UNIQUE constraint on entity_id to enforce SDD invariant:
        // An entity can only have ONE entry in each state table (prevents cyclic transitions)
        var uniqueEntityName = state.table() + "_" + entity.name() + "_id_unique";
        var uniqueEntityDef = "UNIQUE (" + entity.name() + "_id)";
        constraints.add(new ConstraintDefinition(
                uniqueEntityName, tableName, ConstraintDefinition.ConstraintType.UNIQUE, uniqueEntityDef));

        // UNIQUE composite constraint (id, entity_id) to serve as target for composite foreign keys
        // This ensures transitions stay within the same aggregate
        var uniqueCompositeName = state.table() + "_id_" + entity.name() + "_id_unique";
        var uniqueCompositeDef = "UNIQUE (id, " + entity.name() + "_id)";
        constraints.add(new ConstraintDefinition(
                uniqueCompositeName, tableName, ConstraintDefinition.ConstraintType.UNIQUE, uniqueCompositeDef));

        return constraints;
    }

    private List<ConstraintDefinition> generateOrTransitionUniqueConstraints(
            EntityDef entity, StateDef state, String stateSchema) {
        var constraints = new ArrayList<ConstraintDefinition>();
        var tableName = stateSchema + "." + state.name() + "_source";

        // Add UNIQUE composite constraint (id, entity_id) for this mapping table
        var uniqueName = state.name() + "_source_id_" + entity.name() + "_id_unique";
        var uniqueDef = "UNIQUE (id, " + entity.name() + "_id)";
        constraints.add(
                new ConstraintDefinition(uniqueName, tableName, ConstraintDefinition.ConstraintType.UNIQUE, uniqueDef));

        return constraints;
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
                // Composite FK to OR mapping table: ensures mapping is for same aggregate
                // (previous_source_id, entity_id) -> (id, entity_id)
                var orFkName = state.table() + "_previous_source_id_fk";
                var orFkDef = "FOREIGN KEY (previous_source_id, " + entity.name() + "_id) REFERENCES " + stateSchema
                        + "." + state.name() + "_source(id, " + entity.name() + "_id)";
                constraints.add(new ConstraintDefinition(
                        orFkName, tableName, ConstraintDefinition.ConstraintType.FOREIGN_KEY, orFkDef));
            } else {
                // Composite FK to previous state tables: ensures transitions stay within same aggregate
                // (previous_x_id, entity_id) -> (id, entity_id)
                for (var fromState : state.from()) {
                    var fromStateTable = entity.states().get(fromState).table();
                    var prevFkName = state.table() + "_previous_" + fromState + "_id_fk";
                    var prevFkDef = "FOREIGN KEY (previous_" + fromState + "_id, " + entity.name() + "_id) REFERENCES "
                            + stateSchema + "." + fromStateTable + "(id, " + entity.name() + "_id)";
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

    private IndexDefinition generateIndexForForeignKey(ConstraintDefinition fk, EntityDef entity, String tableName) {
        // Extract column names from FK definition: "FOREIGN KEY (col1, col2) REFERENCES ..."
        // Supports both simple and composite foreign keys
        var fkDef = fk.definition();
        var startIdx = fkDef.indexOf('(') + 1;
        var endIdx = fkDef.indexOf(')');
        var columnsPart = fkDef.substring(startIdx, endIdx).trim();

        // Split by comma and trim each column name
        var columns = List.of(columnsPart.split(",")).stream().map(String::trim).toList();

        // Generate index name: idx_tablename_col1_col2
        var indexName = "idx_" + tableName + "_" + String.join("_", columns);
        var schema =
                fk.table().contains(".") ? fk.table().substring(0, fk.table().indexOf('.')) : "public";

        return new IndexDefinition(indexName, tableName, schema, columns, false);
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

        return def.toString();
    }

    private String renderIndex(IndexDefinition index) {
        var ddl = new StringBuilder();
        ddl.append("CREATE INDEX ").append(index.name());
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
                "ALTER TABLE " + constraint.table() + " ADD CONSTRAINT " + constraint.name() + " "
                        + constraint.definition() + ";";
            case PRIMARY_KEY ->
                throw new IllegalStateException(
                        "PRIMARY_KEY constraints should be defined inline in CREATE TABLE, not via ALTER TABLE");
        };
    }

    private String renderView(ViewDefinition view) {
        return "CREATE VIEW " + view.fullName() + " AS\n" + view.query() + ";";
    }
}
