package io.statemodeler.sql.postgres;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PostgresFunctionGeneratorTest {

    @Test
    void shouldGenerateFunctionDefinitionForDomainStateSync() {
        var generator = new PostgresFunctionGenerator();

        var idAttr = new io.statemodeler.core.AttributeDef("id", "BIGINT", false, true, null, null);
        var entity = new io.statemodeler.core.EntityDef("order", "orders", idAttr, java.util.Map.of(), java.util.Map.of(), java.util.Map.of(), java.util.Map.of());

        var func = generator.generateSyncDomainStateFunction(entity, "public");

        assertNotNull(func);
        assertEquals("sync_order_state", func.name());
        assertEquals("public", func.schema());
        assertEquals("TRIGGER", func.returnType());
        assertEquals("plpgsql", func.language());
        assertTrue(func.body().contains("INSERT INTO public.order_state"));
    }
}
