package io.statemodeler.sql.postgres;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.statemodeler.core.SddModel;
import io.statemodeler.sql.*;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Pebble-based PostgreSQL DDL generator implementation.
 * Generates DDL using Pebble templates instead of string concatenation.
 */
public final class PebblePostgresDdlGenerator implements DdlGenerator {

    private static final String DIALECT = "postgres";
    private final PostgresTableGenerator tableGenerator;
    private final PostgresConstraintGenerator constraintGenerator;
    private final PostgresViewGenerator viewGenerator;
    private final PostgresIndexGenerator indexGenerator;
    private final PostgresFunctionGenerator functionGenerator;
    private final PostgresTriggerGenerator triggerGenerator;
    private final PebbleEngine engine;

    public PebblePostgresDdlGenerator() {
        this.tableGenerator = new PostgresTableGenerator();
        this.constraintGenerator = new PostgresConstraintGenerator();
        this.viewGenerator = new PostgresViewGenerator();
        this.indexGenerator = new PostgresIndexGenerator();
        this.functionGenerator = new PostgresFunctionGenerator();
        this.triggerGenerator = new PostgresTriggerGenerator();
        this.engine = new PebbleEngine.Builder().autoEscaping(false).build();
    }

    @Override
    public String generateDdl(SddModel model) {
        SqlPlan plan = generateSqlPlan(model);
        var template = engine.getTemplate("templates/sql/postgres/main.sql.pebble");
        var writer = new StringWriter();
        Map<String, Object> context = new HashMap<>();
        context.put("plan", plan);
        context.put("entitySchema", model.database().schema()); // Added back from original renderDdl
        context.put("stateSchema", model.database().effectiveStateSchema()); // Added back from original renderDdl
        context.put("version", model.version());
        try {
            template.evaluate(writer, context);
            return writer.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate DDL", e);
        }
    }

    @Override
    public String getDialect() {
        return DIALECT;
    }

    /**
     * Generate an abstract SQL plan from the SDD model.
     * Generates the SQL plan from the provided SDD model. The plan building logic
     * mirrors the prior in-memory SQL plan construction, but the final DDL
     * rendering is performed via Pebble templates.
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
                var extensionTable =
                        tableGenerator.generateExtensionTable(entity, extension, entitySchema, stateSchema);
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
                    var uniqueConstraints =
                            constraintGenerator.generateOrTransitionUniqueConstraints(entity, state, stateSchema);
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
                var fkConstraints =
                        constraintGenerator.generateStateForeignKeys(entity, state, entitySchema, stateSchema);
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
                        boolean p1IsIntervals =
                                p1.kind() == io.statemodeler.core.ProjectionDef.ProjectionKind.INTERVALS;
                        boolean p2IsIntervals =
                                p2.kind() == io.statemodeler.core.ProjectionDef.ProjectionKind.INTERVALS;
                        if (p1IsIntervals && !p2IsIntervals) return -1;
                        if (!p1IsIntervals && p2IsIntervals) return 1;
                        return p1.name().compareTo(p2.name()); // stable sort for same kind
                    })
                    .forEach(projection -> views.add(
                            viewGenerator.generateProjectionView(entity, projection, entitySchema, stateSchema)));
        }

        return new SqlPlan(tables, views, constraints, indexes, functions, triggers);
    }

    private String renderDdl(SqlPlan plan, String entitySchema, String stateSchema) {
        PebbleTemplate template = engine.getTemplate("templates/sql/postgres/main.sql.pebble");
        Map<String, Object> context = new HashMap<>();
        context.put("plan", plan);
        context.put("entitySchema", entitySchema);
        context.put("stateSchema", stateSchema);

        Writer writer = new StringWriter();
        try {
            template.evaluate(writer, context);
            return writer.toString().trim();
        } catch (Exception e) {
            throw new RuntimeException("Failed to render DDL template", e);
        }
    }
}
