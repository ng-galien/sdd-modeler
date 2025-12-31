package io.statemodeler.sql.postgres;

import io.statemodeler.core.EntityDef;
import io.statemodeler.core.ExtensionDef;
import io.statemodeler.core.StateDef;
import io.statemodeler.sql.ColumnDefinition;
import io.statemodeler.sql.TableDefinition;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates PostgreSQL table definitions for SDD entities, states, extensions,
 * and OR transition
 * mapping tables.
 */
final class PostgresTableGenerator {

    private static String toSnake(String s) {
        if (s == null) return null;
        return s.replaceAll("(?<=[A-Za-z0-9])(?=[A-Z])", "_").toLowerCase();
    }

    /**
     * Generate entity table definition.
     *
     * @param entity entity definition
     * @param schema schema name for entity table
     * @return table definition for entity
     */
    TableDefinition generateEntityTable(EntityDef entity, String schema) {
        var columns = new ArrayList<ColumnDefinition>();

        // Add ID column
        columns.add(new ColumnDefinition(
                toSnake(entity.id().name()),
                entity.id().type(),
                entity.id().nullable(),
                entity.id().primaryKey(),
                entity.id().defaultValue(),
                null,
                null));

        // Add entity attributes
        for (var attr : entity.attributes().values()) {
            columns.add(new ColumnDefinition(
                    toSnake(attr.name()),
                    attr.type(),
                    attr.nullable(),
                    attr.primaryKey(),
                    attr.defaultValue(),
                    null,
                    null));
        }

        return new TableDefinition(
                entity.table(), schema, columns, List.of(toSnake(entity.id().name())));
    }

    /**
     * Generate state table definition.
     *
     * @param entity       entity definition
     * @param state        state definition
     * @param entitySchema schema for entity tables
     * @param stateSchema  schema for state tables
     * @return table definition for state
     */
    TableDefinition generateStateTable(EntityDef entity, StateDef state, String entitySchema, String stateSchema) {
        var columns = new ArrayList<ColumnDefinition>();

        // Primary key
        columns.add(new ColumnDefinition("id", "SERIAL", false, true, null, null, null));

        // Entity reference (FK added later as constraint)
        columns.add(new ColumnDefinition(
                toSnake(entity.name()) + "_id", entity.id().type(), false, false, null, null, null));

        // Timestamp
        columns.add(new ColumnDefinition("created_at", "TIMESTAMPTZ", false, false, "NOW()", null, null));

        // Previous state references (for transitions)
        if (!state.initial()) {
            if (state.hasOrTransitions()) {
                // OR transitions use mapping table (FK added later as constraint)
                columns.add(new ColumnDefinition("previous_source_id", "INTEGER", false, false, null, null, null));
            } else {
                // Simple transitions (FK added later as constraint)
                if (state.hasSimpleTransitions()) {
                    columns.add(new ColumnDefinition(
                            "previous_" + toSnake(state.from()) + "_id", "INTEGER", false, false, null, null, null));
                }
            }
        }

        // State-specific attributes - use snake_case column names
        for (var attr : state.attributes().values()) {
            columns.add(new ColumnDefinition(
                    toSnake(attr.name()), attr.type(), false, attr.primaryKey(), attr.defaultValue(), null, null));
        }

        return new TableDefinition(state.table(), stateSchema, columns, List.of("id"));
    }

    /**
     * Generate extension table definition.
     *
     * @param entity       entity definition
     * @param extension    extension definition
     * @param entitySchema schema for entity tables
     * @param stateSchema  schema for state/extension tables
     * @return table definition for extension
     */
    TableDefinition generateExtensionTable(
            EntityDef entity, ExtensionDef extension, String entitySchema, String stateSchema) {
        var columns = new ArrayList<ColumnDefinition>();

        // Primary key references the state table (FK added later as constraint)
        columns.add(new ColumnDefinition(extension.targetState() + "_id", "INTEGER", false, true, null, null, null));

        // Extension attributes - use snake_case column names
        for (var attr : extension.attributes().values()) {
            columns.add(new ColumnDefinition(
                    toSnake(attr.name()), attr.type(), false, attr.primaryKey(), attr.defaultValue(), null, null));
        }

        // Updated timestamp for mutable extensions
        columns.add(new ColumnDefinition("updated_at", "TIMESTAMPTZ", false, false, "NOW()", null, null));

        return new TableDefinition(extension.table(), stateSchema, columns, List.of(extension.targetState() + "_id"));
    }

    /**
     * Generate OR transition mapping table definition.
     *
     * @param entity       entity definition
     * @param state        state definition with OR transitions
     * @param entitySchema schema for entity tables
     * @param stateSchema  schema for state tables
     * @return table definition for OR transition mapping
     */
    TableDefinition generateOrTransitionTable(
            EntityDef entity, StateDef state, String entitySchema, String stateSchema) {
        var columns = new ArrayList<ColumnDefinition>();

        // Primary key
        columns.add(new ColumnDefinition("id", "SERIAL", false, true, null, null, null));

        // Entity reference (required for composite FK) - snake_case
        columns.add(new ColumnDefinition(
                toSnake(entity.name()) + "_id", entity.id().type(), false, false, null, null, null));

        // References to possible source states (FK added separately to avoid circular
        // dependency)
        for (var fromState : state.fromAnyOf()) {
            columns.add(new ColumnDefinition(
                    toSnake(fromState) + "_state_id",
                    "INTEGER",
                    true, // nullable - only one will be set
                    false,
                    null,
                    null, // No FK here - added later as constraint
                    null));
        }

        return new TableDefinition(state.name() + "_source", stateSchema, columns, List.of("id"));
    }

    /**
     * Generate domain state table for current state projection.
     *
     * <p>
     * This table maintains the current state of each entity instance, updated
     * automatically via
     * triggers on state tables.
     *
     * @param entity      entity definition
     * @param stateSchema schema for state tables
     * @return table definition for domain state
     */
    TableDefinition generateDomainStateTable(EntityDef entity, String stateSchema) {
        var columns = new ArrayList<ColumnDefinition>();
        var entityIdColumn = toSnake(entity.name()) + "_id";

        // Primary key (entity_id)
        // Primary key (entity_id)
        String pkType = entity.id().type();
        if ("serial".equalsIgnoreCase(pkType)) {
            pkType = "INTEGER";
        } else if ("bigserial".equalsIgnoreCase(pkType)) {
            pkType = "BIGINT";
        }
        columns.add(new ColumnDefinition(entityIdColumn, pkType, false, true, null, null, null));

        // Current state type
        columns.add(new ColumnDefinition("state_type", "TEXT", false, false, null, null, null));

        // Reference to state row
        columns.add(new ColumnDefinition("state_row_id", "BIGINT", false, false, null, null, null));

        // State timestamp
        columns.add(new ColumnDefinition("state_at", "TIMESTAMPTZ", false, false, null, null, null));

        // JSON snapshot
        columns.add(new ColumnDefinition("state_json", "JSONB", false, false, null, null, null));

        // Updated timestamp
        columns.add(new ColumnDefinition("updated_at", "TIMESTAMPTZ", false, false, "NOW()", null, null));

        var domainTableName =
                entity.name().replaceAll("(?<=[A-Za-z0-9])(?=[A-Z])", "_").toLowerCase() + "_state";
        return new TableDefinition(domainTableName, stateSchema, columns, List.of(entityIdColumn));
    }
}
