package io.statemodeler.sql.postgres;

import io.statemodeler.core.EntityDef;
import io.statemodeler.core.StateDef;
import io.statemodeler.sql.FunctionDefinition;
import java.util.List;

/**
 * Generates PostgreSQL function definitions for SDD domain state
 * synchronization.
 */
final class PostgresFunctionGenerator {

    /**
     * Generate the generic domain state sync function that updates the domain_state
     * table.
     *
     * <p>
     * This function is called by triggers on each state table and maintains the
     * current state of
     * each entity in a centralized domain_state table.
     *
     * @param entity      entity definition
     * @param stateSchema schema where state tables and the function will be created
     * @return function definition for sync_domain_state
     */
    FunctionDefinition generateSyncDomainStateFunction(EntityDef entity, String stateSchema) {
        var entityIdColumn = entity.name() + "_id";
        var stateTableName = entity.name() + "_state";

        String body = String.format(
                """
                        DECLARE
                            v_state_type text := TG_ARGV[0];
                        BEGIN
                            INSERT INTO %s.%s (%s, state_type, state_row_id, state_at, state_json)
                            VALUES (
                                NEW.%s,
                                v_state_type,
                                NEW.id,
                                NEW.created_at,
                                to_jsonb(NEW)
                            )
                            ON CONFLICT (%s) DO UPDATE
                            SET
                                state_type = EXCLUDED.state_type,
                                state_row_id = EXCLUDED.state_row_id,
                                state_at = EXCLUDED.state_at,
                                state_json = EXCLUDED.state_json,
                                updated_at = NOW();

                            RETURN NEW;
                        END;
                        """,
                stateSchema,
                stateTableName,
                entityIdColumn,
                entityIdColumn,
                entityIdColumn);

        return new FunctionDefinition(
                "sync_" + entity.name() + "_state", stateSchema, List.of(), "TRIGGER", "plpgsql", body);
    }

    /**
     * Render function definition to PostgreSQL DDL.
     *
     * @param function function definition to render
     * @return PostgreSQL CREATE FUNCTION statement
     */
    String renderFunction(FunctionDefinition function) {
        var params = String.join(", ", function.parameters());
        return String.format(
                "CREATE OR REPLACE FUNCTION %s.%s(%s) RETURNS %s AS $$\n%s$$ LANGUAGE %s;",
                function.schema(),
                function.name(),
                params,
                function.returnType(),
                function.body(),
                function.language());
    }
}
