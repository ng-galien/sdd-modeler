package io.statemodeler.sql.postgres;

import io.statemodeler.core.EntityDef;
import io.statemodeler.core.ExtensionDef;
import io.statemodeler.core.StateDef;
import io.statemodeler.sql.ConstraintDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates PostgreSQL constraint definitions for SDD entities, states, and
 * extensions.
 * Handles UNIQUE constraints, foreign keys, and CHECK constraints for OR
 * transitions.
 */
final class PostgresConstraintGenerator {

    private static String toSnake(String s) {
        if (s == null) return null;
        return s.replaceAll("(?<=[A-Za-z0-9])(?=[A-Z])", "_").toLowerCase();
    }

    /**
     * Generate UNIQUE constraints for state table.
     * Creates two UNIQUE constraints:
     * 1. UNIQUE(entity_id) - enforces SDD invariant (prevents cyclic transitions)
     * 2. UNIQUE(id, entity_id) - composite target for foreign keys (ensures
     * aggregate integrity)
     *
     * @param entity      entity definition
     * @param state       state definition
     * @param stateSchema schema for state tables
     * @return list of UNIQUE constraints
     */
    List<ConstraintDefinition> generateStateUniqueConstraints(EntityDef entity, StateDef state, String stateSchema) {
        var constraints = new ArrayList<ConstraintDefinition>();
        var tableName = stateSchema + "." + state.table();

        // UNIQUE constraint on entity_id to enforce SDD invariant:
        // An entity can only have ONE entry in each state table (prevents cyclic
        // transitions)
        var uniqueEntityName = state.table() + "_" + toSnake(entity.name()) + "_id_unique";
        var uniqueEntityDef = "UNIQUE (" + toSnake(entity.name()) + "_id)";
        constraints.add(new ConstraintDefinition(
                uniqueEntityName, tableName, ConstraintDefinition.ConstraintType.UNIQUE, uniqueEntityDef));

        // UNIQUE composite constraint (id, entity_id) to serve as target for composite
        // foreign keys
        // This ensures transitions stay within the same aggregate
        var uniqueCompositeName = state.table() + "_id_" + toSnake(entity.name()) + "_id_unique";
        var uniqueCompositeDef = "UNIQUE (id, " + toSnake(entity.name()) + "_id)";
        constraints.add(new ConstraintDefinition(
                uniqueCompositeName, tableName, ConstraintDefinition.ConstraintType.UNIQUE, uniqueCompositeDef));

        return constraints;
    }

    /**
     * Generate UNIQUE constraints for OR transition mapping table.
     * Creates UNIQUE(id, entity_id) for composite FK target.
     *
     * @param entity      entity definition
     * @param state       state definition with OR transitions
     * @param stateSchema schema for state tables
     * @return list of UNIQUE constraints
     */
    List<ConstraintDefinition> generateOrTransitionUniqueConstraints(
            EntityDef entity, StateDef state, String stateSchema) {
        var constraints = new ArrayList<ConstraintDefinition>();
        var tableName = stateSchema + "." + state.name() + "_source";

        // Add UNIQUE composite constraint (id, entity_id) for this mapping table
        var uniqueName = state.name() + "_source_id_" + toSnake(entity.name()) + "_id_unique";
        var uniqueDef = "UNIQUE (id, " + toSnake(entity.name()) + "_id)";
        constraints.add(
                new ConstraintDefinition(uniqueName, tableName, ConstraintDefinition.ConstraintType.UNIQUE, uniqueDef));

        return constraints;
    }

    /**
     * Generate foreign key constraints for state table.
     * Creates FKs to entity table and previous state tables (simple or OR
     * transitions).
     * All FKs to state tables are composite: (column, entity_id) -> (id,
     * entity_id).
     *
     * @param entity       entity definition
     * @param state        state definition
     * @param entitySchema schema for entity tables
     * @param stateSchema  schema for state tables
     * @return list of FK constraints
     */
    List<ConstraintDefinition> generateStateForeignKeys(
            EntityDef entity, StateDef state, String entitySchema, String stateSchema) {
        var constraints = new ArrayList<ConstraintDefinition>();
        var tableName = stateSchema + "." + state.table();

        // FK to entity table
        var entityFkName = state.table() + "_" + toSnake(entity.name()) + "_id_fk";
        var entityFkDef = "FOREIGN KEY (" + toSnake(entity.name()) + "_id) REFERENCES " + entitySchema + "."
                + entity.table() + "(id)";
        constraints.add(new ConstraintDefinition(
                entityFkName, tableName, ConstraintDefinition.ConstraintType.FOREIGN_KEY, entityFkDef));

        // FK to previous states
        if (!state.initial()) {
            if (state.hasOrTransitions()) {
                // Composite FK to OR mapping table: ensures mapping is for same aggregate
                // (previous_source_id, entity_id) -> (id, entity_id)
                var orFkName = state.table() + "_previous_source_id_fk";
                var orFkDef = "FOREIGN KEY (previous_source_id, " + entity.name() + "_id) REFERENCES "
                        + stateSchema
                        + "." + state.name() + "_source(id, " + entity.name() + "_id)";
                constraints.add(new ConstraintDefinition(
                        orFkName, tableName, ConstraintDefinition.ConstraintType.FOREIGN_KEY, orFkDef));
            } else {
                // Composite FK to previous state tables: ensures transitions stay within same
                // aggregate
                // (previous_x_id, entity_id) -> (id, entity_id)
                for (var fromState : state.from()) {
                    var fromStateTable = entity.states().get(fromState).table();
                    var prevFkName = state.table() + "_previous_" + toSnake(fromState) + "_id_fk";
                    var prevFkDef = "FOREIGN KEY (previous_" + toSnake(fromState) + "_id, " + toSnake(entity.name())
                            + "_id) REFERENCES "
                            + stateSchema + "." + fromStateTable + "(id, " + toSnake(entity.name())
                            + "_id)";
                    constraints.add(new ConstraintDefinition(
                            prevFkName, tableName, ConstraintDefinition.ConstraintType.FOREIGN_KEY, prevFkDef));
                }
            }
        }

        return constraints;
    }

    /**
     * Generate foreign key constraints for OR transition mapping table.
     * Creates FKs to entity table and all source state tables.
     * FKs to state tables are composite: (x_state_id, entity_id) -> (id,
     * entity_id).
     *
     * @param entity       entity definition
     * @param state        state definition with OR transitions
     * @param entitySchema schema for entity tables
     * @param stateSchema  schema for state tables
     * @return list of FK constraints
     */
    List<ConstraintDefinition> generateOrTransitionForeignKeys(
            EntityDef entity, StateDef state, String entitySchema, String stateSchema) {
        var constraints = new ArrayList<ConstraintDefinition>();
        var tableName = stateSchema + "." + state.name() + "_source";

        // Add FK to entity table first
        var entityFkName = state.name() + "_source_" + toSnake(entity.name()) + "_id_fk";
        var entityFkDef = "FOREIGN KEY (" + toSnake(entity.name()) + "_id) REFERENCES " + entitySchema + "."
                + entity.table() + "(id)";
        constraints.add(new ConstraintDefinition(
                entityFkName, tableName, ConstraintDefinition.ConstraintType.FOREIGN_KEY, entityFkDef));

        // Add composite FK constraints to source state tables
        // (x_state_id, entity_id) -> (id, entity_id) ensures sources are from same
        // aggregate
        for (var fromState : state.fromAnyOf()) {
            var sourceState = entity.states().get(fromState);
            var constraintName = state.name() + "_source_" + toSnake(fromState) + "_fk";
            var fkDefinition = "FOREIGN KEY (" + toSnake(fromState) + "_state_id, " + toSnake(entity.name())
                    + "_id) REFERENCES "
                    + stateSchema + "." + sourceState.table() + "(id, " + toSnake(entity.name()) + "_id)";
            constraints.add(new ConstraintDefinition(
                    constraintName, tableName, ConstraintDefinition.ConstraintType.FOREIGN_KEY, fkDefinition));
        }

        return constraints;
    }

    /**
     * Generate CHECK constraint for OR transition mapping table.
     * Ensures exactly one source state is set (XOR logic).
     *
     * @param entity      entity definition
     * @param state       state definition with OR transitions
     * @param stateSchema schema for state tables
     * @return CHECK constraint
     */
    ConstraintDefinition generateOrTransitionConstraint(EntityDef entity, StateDef state, String stateSchema) {
        var constraintName = state.name() + "_source_check";
        var tableName = stateSchema + "." + state.name() + "_source";

        // Generate CHECK constraint ensuring exactly one source is set
        var conditions = state.fromAnyOf().stream()
                .map(fromState -> toSnake(fromState) + "_state_id IS NOT NULL")
                .collect(Collectors.joining(" OR "));

        var exclusiveConditions = new ArrayList<String>();
        for (int i = 0; i < state.fromAnyOf().size(); i++) {
            for (int j = i + 1; j < state.fromAnyOf().size(); j++) {
                var state1 = state.fromAnyOf().get(i);
                var state2 = state.fromAnyOf().get(j);
                exclusiveConditions.add("NOT (" + toSnake(state1) + "_state_id IS NOT NULL AND " + toSnake(state2)
                        + "_state_id IS NOT NULL)");
            }
        }

        var definition = "(" + conditions + ") AND " + String.join(" AND ", exclusiveConditions);

        return new ConstraintDefinition(
                constraintName, tableName, ConstraintDefinition.ConstraintType.CHECK, definition);
    }

    /**
     * Generate foreign key constraint for extension table.
     * Creates FK to target state table.
     *
     * @param entity      entity definition
     * @param extension   extension definition
     * @param stateSchema schema for state/extension tables
     * @return FK constraint
     */
    ConstraintDefinition generateExtensionForeignKey(EntityDef entity, ExtensionDef extension, String stateSchema) {
        var tableName = stateSchema + "." + extension.table();
        var targetState = entity.states().get(extension.targetState());
        var fkName = extension.table() + "_" + toSnake(extension.targetState()) + "_id_fk";
        var fkDef = "FOREIGN KEY (" + toSnake(extension.targetState()) + "_id) REFERENCES " + stateSchema + "."
                + targetState.table() + "(id)";
        return new ConstraintDefinition(fkName, tableName, ConstraintDefinition.ConstraintType.FOREIGN_KEY, fkDef);
    }

    /**
     * Generate foreign key constraint for domain state table.
     * Creates FK from domain_state table to entity table.
     *
     * @param entity       entity definition
     * @param entitySchema schema for entity tables
     * @param stateSchema  schema for state/domain_state tables
     * @return FK constraint
     */
    ConstraintDefinition generateDomainStateForeignKey(EntityDef entity, String entitySchema, String stateSchema) {
        var entityIdColumn = toSnake(entity.name()) + "_id";
        var snakeName = toSnake(entity.name());
        var domainStateTable = stateSchema + "." + snakeName + "_state";
        var fkName = toSnake(entity.name()) + "_state_" + entityIdColumn + "_fk";
        var fkDef = "FOREIGN KEY (" + entityIdColumn + ") REFERENCES " + entitySchema + "." + entity.table() + "(id)";
        return new ConstraintDefinition(
                fkName, domainStateTable, ConstraintDefinition.ConstraintType.FOREIGN_KEY, fkDef);
    }
}
