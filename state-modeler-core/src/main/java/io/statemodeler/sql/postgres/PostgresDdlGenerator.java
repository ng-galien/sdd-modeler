package io.statemodeler.sql.postgres;

import io.statemodeler.core.*;
import io.statemodeler.sql.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * PostgreSQL-specific DDL generator implementation.
 * Generates DDL following SDD patterns with PostgreSQL syntax.
 */
public final class PostgresDdlGenerator implements DdlGenerator {

    private static final String DIALECT = "postgres";
    private final PostgresTableGenerator tableGenerator;
    private final PostgresConstraintGenerator constraintGenerator;
    private final PostgresViewGenerator viewGenerator;
    private final PostgresIndexGenerator indexGenerator;
    private final PostgresFunctionGenerator functionGenerator;
    private final PostgresTriggerGenerator triggerGenerator;

    public PostgresDdlGenerator() {
        this.tableGenerator = new PostgresTableGenerator();
        this.constraintGenerator = new PostgresConstraintGenerator();
        this.viewGenerator = new PostgresViewGenerator();
        this.indexGenerator = new PostgresIndexGenerator();
        this.functionGenerator = new PostgresFunctionGenerator();
        this.triggerGenerator = new PostgresTriggerGenerator();
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
        var functions = new ArrayList<FunctionDefinition>();
        var triggers = new ArrayList<TriggerDefinition>();

        var entitySchema = model.database().schema();
        var stateSchema = model.database().effectiveStateSchema();

        // Generate entity tables and state tables for each entity
        for (var entity : model.entities().values()) {
            // Main entity table in entity schema
            tables.add(tableGenerator.generateEntityTable(entity, entitySchema));

            // OR transition mapping tables in state schema (created WITHOUT FK to avoid
            // circular dependency)
            for (var state : entity.states().values()) {
                if (state.hasOrTransitions()) {
                    var orTable = tableGenerator.generateOrTransitionTable(entity, state, entitySchema, stateSchema);
                    tables.add(orTable);
                    constraints.add(constraintGenerator.generateOrTransitionConstraint(entity, state, stateSchema));
                }
            }

            // State tables in state schema (created WITHOUT FK to avoid circular
            // dependencies)
            for (var state : entity.states().values()) {
                var stateTable = tableGenerator.generateStateTable(entity, state, entitySchema, stateSchema);
                tables.add(stateTable);
            }

            // Extension tables in state schema (created WITHOUT FK to avoid circular
            // dependencies)
            for (var extension : entity.extensions().values()) {
                var extensionTable = tableGenerator.generateExtensionTable(entity, extension, entitySchema,
                        stateSchema);
                tables.add(extensionTable);
            }

            // Domain state table (projection of current state)
            var domainStateTable = tableGenerator.generateDomainStateTable(entity, stateSchema);
            tables.add(domainStateTable);

            // Generate sync function for domain state
            var syncFunction = functionGenerator.generateSyncDomainStateFunction(entity, stateSchema);
            functions.add(syncFunction);

            // Add constraints in dependency order:
            // STEP 1: Add UNIQUE constraints first (they become targets for composite FKs)
            for (var state : entity.states().values()) {
                var uniqueConstraints = constraintGenerator.generateStateUniqueConstraints(entity, state, stateSchema);
                constraints.addAll(uniqueConstraints);
            }
            for (var state : entity.states().values()) {
                if (state.hasOrTransitions()) {
                    var uniqueConstraints = constraintGenerator.generateOrTransitionUniqueConstraints(entity, state,
                            stateSchema);
                    constraints.addAll(uniqueConstraints);
                }
            }

            // STEP 2: Add FK constraints (after UNIQUE constraints are created)
            // 2a. FK from OR mapping tables to entity and state tables
            for (var state : entity.states().values()) {
                if (state.hasOrTransitions()) {
                    var fkConstraints = constraintGenerator.generateOrTransitionForeignKeys(
                            entity, state, entitySchema, stateSchema);
                    constraints.addAll(fkConstraints);
                    // Generate indexes for FK columns
                    for (var fk : fkConstraints) {
                        indexes.add(indexGenerator.generateIndexForForeignKey(fk, entity, state.name() + "_source"));
                    }
                }
            }
            // 2b. FK from state tables to entity and previous states
            for (var state : entity.states().values()) {
                var fkConstraints = constraintGenerator.generateStateForeignKeys(entity, state, entitySchema,
                        stateSchema);
                constraints.addAll(fkConstraints);
                // Generate indexes for FK columns (skip entity_id FK - UNIQUE constraint
                // creates implicit index)
                for (var fk : fkConstraints) {
                    if (fk.type() == ConstraintDefinition.ConstraintType.FOREIGN_KEY
                            && !fk.name().contains("_" + entity.name() + "_id_fk")) {
                        indexes.add(indexGenerator.generateIndexForForeignKey(fk, entity, state.table()));
                    }
                }
            }
            // 2c. FK from extension tables to state tables
            for (var extension : entity.extensions().values()) {
                var fk = constraintGenerator.generateExtensionForeignKey(entity, extension, stateSchema);
                constraints.add(fk);
                // Generate index for FK column
                indexes.add(indexGenerator.generateIndexForForeignKey(fk, entity, extension.table()));
            }
            // 2d. FK from domain_state table to entity table
            var domainStateFk = constraintGenerator.generateDomainStateForeignKey(entity, entitySchema, stateSchema);
            constraints.add(domainStateFk);

            // Generate triggers for state synchronization
            for (var state : entity.states().values()) {
                var trigger = triggerGenerator.generateStateSyncTrigger(entity, state, stateSchema);
                triggers.add(trigger);
            }

            // Projection views in state schema (intervals MUST be before current_state)
            entity.projections().values().stream()
                    .sorted((p1, p2) -> {
                        // intervals before current_state (current_state depends on intervals view)
                        boolean p1IsIntervals = p1
                                .kind() == io.statemodeler.core.ProjectionDef.ProjectionKind.INTERVALS;
                        boolean p2IsIntervals = p2
                                .kind() == io.statemodeler.core.ProjectionDef.ProjectionKind.INTERVALS;
                        if (p1IsIntervals && !p2IsIntervals)
                            return -1;
                        if (!p1IsIntervals && p2IsIntervals)
                            return 1;
                        return p1.name().compareTo(p2.name()); // stable sort for same kind
                    })
                    .forEach(projection -> views.add(
                            viewGenerator.generateProjectionView(entity, projection, entitySchema, stateSchema)));
        }

        return new SqlPlan(tables, views, constraints, indexes, functions, triggers);
    }

    /**
     * Render the SQL plan to PostgreSQL DDL.
     */
    private String renderDdl(SqlPlan plan, String entitySchema, String stateSchema) {
        var ddl = new StringBuilder();

        // Schema creation for entity schema if specified (non-null, non-empty,
        // non-public)
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

        // Functions (before constraints, so they exist when triggers reference them)
        for (var function : plan.functions()) {
            ddl.append(functionGenerator.renderFunction(function)).append("\n\n");
        }

        // Constraints
        for (var constraint : plan.constraints()) {
            ddl.append(renderConstraint(constraint)).append("\n\n");
        }

        // Triggers (after constraints, so all tables and constraints exist)
        for (var trigger : plan.triggers()) {
            ddl.append(triggerGenerator.renderTrigger(trigger)).append("\n\n");
        }

        // Indexes
        for (var index : plan.indexes()) {
            ddl.append(indexGenerator.renderIndex(index)).append("\n\n");
        }

        // Views
        for (var view : plan.views()) {
            ddl.append(renderView(view)).append("\n\n");
        }

        return ddl.toString().trim();
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
