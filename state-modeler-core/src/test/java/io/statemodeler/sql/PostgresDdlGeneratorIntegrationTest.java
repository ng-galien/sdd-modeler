package io.statemodeler.sql;

import static org.assertj.core.api.Assertions.*;

import io.statemodeler.core.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PostgresDdlGeneratorIntegrationTest {

    @Test
    void shouldGenerateEntityTableDdl() throws Exception {
        var generator = DdlGenerators.forDialect("postgres");
        var model = createSimpleOrderModel();

        var ddl = generator.generateDdl(model);

        System.out.println("Generated DDL:");
        System.out.println(ddl);

        assertThat(ddl).contains("CREATE TABLE public.orders");
        assertThat(ddl).contains("id serial NOT NULL");
        assertThat(ddl).contains("customer_id int NOT NULL");
        assertThat(ddl).contains("CREATE TABLE public.order_pending");
        assertThat(ddl).contains("pending_reason text NOT NULL");
    }

    private SddModel createSimpleOrderModel() {
        var database = new DatabaseConfig("postgres", "public");

        // Entity attributes
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var customerIdAttr = new AttributeDef("customer_id", "int", false, false, null, "Customer reference");
        var totalAmountAttr = new AttributeDef("total_amount", "numeric(10,2)", false, false, null, null);

        // Initial state: pending
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
                Map.of("customer_id", customerIdAttr, "total_amount", totalAmountAttr),
                Map.of("pending", pendingState),
                Map.of(), // no extensions
                Map.of()); // no projections

        return new SddModel("0.1", "simple-order-model", database, Map.of("order", entity));
    }
}
