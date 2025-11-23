package io.statemodeler.sql;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.core.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DdlGeneratorsTest {

    @Test
    void shouldCreatePostgresGenerator() {
        var generator = DdlGenerators.forDialect("postgres");

        assertNotNull(generator);
        assertEquals("postgres", generator.getDialect());
    }

    @Test
    void shouldSupportPostgresDialects() {
        assertTrue(DdlGenerators.isSupported("postgres"));
        assertTrue(DdlGenerators.isSupported("postgresql"));
        assertTrue(DdlGenerators.isSupported("POSTGRES"));
    }

    @Test
    void shouldRejectUnsupportedDialect() {
        var ex = assertThrows(IllegalArgumentException.class, () -> DdlGenerators.forDialect("mysql"));
        assertTrue(ex.getMessage().contains("Unsupported SQL dialect: mysql"));
    }

    @Test
    void shouldGenerateBasicDdlForSimpleModel() throws Exception {
        var generator = DdlGenerators.forDialect("postgres");
        var model = createSimpleModel();

        var ddl = generator.generateDdl(model);

        assertFalse(ddl.isBlank());
        assertTrue(ddl.contains("CREATE TABLE"));
        assertTrue(ddl.contains("orders"));
    }

    private SddModel createSimpleModel() {
        var database = new DatabaseConfig("postgres", "public", null, java.util.Map.of());

        // Simple entity with one initial state
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var customerIdAttr = new AttributeDef("customer_id", "int", false, false, null, "Customer reference");

        var pendingState = new StateDef(
                "pending",
                "order_pending",
                true, // initial
                List.of(), // no from states (initial)
                List.of(), // no OR transitions
                Map.of("pending_reason", new AttributeDef("pending_reason", "text", false, false, null, null)));

        var entity = new EntityDef(
                "order",
                "orders",
                idAttr,
                Map.of("customer_id", customerIdAttr),
                Map.of("pending", pendingState),
                Map.of(), // no extensions
                Map.of()); // no projections

        return new SddModel("0.1.0", "test-model", database, Map.of("order", entity));
    }
}
