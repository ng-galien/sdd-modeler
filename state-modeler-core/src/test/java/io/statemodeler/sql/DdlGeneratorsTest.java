package io.statemodeler.sql;

import static org.assertj.core.api.Assertions.*;

import io.statemodeler.core.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DdlGeneratorsTest {

    @Test
    void shouldCreatePostgresGenerator() {
        var generator = DdlGenerators.forDialect("postgres");

        assertThat(generator).isNotNull();
        assertThat(generator.getDialect()).isEqualTo("postgres");
    }

    @Test
    void shouldSupportPostgresDialects() {
        assertThat(DdlGenerators.isSupported("postgres")).isTrue();
        assertThat(DdlGenerators.isSupported("postgresql")).isTrue();
        assertThat(DdlGenerators.isSupported("POSTGRES")).isTrue();
    }

    @Test
    void shouldRejectUnsupportedDialect() {
        assertThatThrownBy(() -> DdlGenerators.forDialect("mysql"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported SQL dialect: mysql");
    }

    @Test
    void shouldGenerateBasicDdlForSimpleModel() throws Exception {
        var generator = DdlGenerators.forDialect("postgres");
        var model = createSimpleModel();

        var ddl = generator.generateDdl(model);

        assertThat(ddl).isNotBlank();
        assertThat(ddl).contains("CREATE TABLE");
        assertThat(ddl).contains("orders");
    }

    private SddModel createSimpleModel() {
        var database = new DatabaseConfig("postgres", "public", null);

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

        return new SddModel("0.1", "test-model", database, Map.of("order", entity));
    }
}
