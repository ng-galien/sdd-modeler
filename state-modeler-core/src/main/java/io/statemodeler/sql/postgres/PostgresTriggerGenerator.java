package io.statemodeler.sql.postgres;

import io.statemodeler.core.EntityDef;
import io.statemodeler.core.StateDef;
import io.statemodeler.sql.TriggerDefinition;
import java.util.List;

/**
 * Generates PostgreSQL trigger definitions for SDD domain state
 * synchronization.
 */
final class PostgresTriggerGenerator {

    /**
     * Generate trigger to sync domain state after state table insert.
     *
     * <p>
     * This trigger calls the sync_domain_state function whenever a new row is
     * inserted into a
     * state table, passing the state type as an argument.
     *
     * @param entity      entity definition
     * @param state       state definition
     * @param stateSchema schema where state tables and triggers are created
     * @return trigger definition for state sync
     */
    TriggerDefinition generateStateSyncTrigger(EntityDef entity, StateDef state, String stateSchema) {
        var triggerName = state.table() + "_sync_" + entity.name() + "_state";
        var tableName = stateSchema + "." + state.table();
        var functionName = stateSchema + ".sync_" + entity.name() + "_state";
        var stateTypeArg = "'" + state.name().toUpperCase() + "'";

        return new TriggerDefinition(
                triggerName, tableName, "AFTER", List.of("INSERT"), "ROW", functionName, List.of(stateTypeArg));
    }

    // Trigger DDL rendering is done in Pebble templates; legacy string-based
    // rendering methods were removed to keep generator logic focused on
    // generating definitions rather than rendering SQL text.
}
