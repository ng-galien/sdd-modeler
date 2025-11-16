package io.statemodeler.sql;

import static org.assertj.core.api.Assertions.*;

import io.statemodeler.core.*;
import io.statemodeler.dsl.YamlModelLoader;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PostgresDdlGeneratorIntegrationTest {

    @Test
    void shouldGenerateEntityTableDdl() throws Exception {
        var generator = DdlGenerators.forDialect("postgres");
        var model = createSimpleOrderModel();

        var ddl = generator.generateDdl(model);

        assertThat(ddl).contains("CREATE TABLE public.orders");
        assertThat(ddl).contains("id serial NOT NULL");
        assertThat(ddl).contains("customer_id int NOT NULL");
        assertThat(ddl).contains("CREATE TABLE public.order_pending");
        assertThat(ddl).contains("pending_reason text NOT NULL");
    }

    @Test
    void shouldGenerateCompleteOrdersDdlFromYaml() throws Exception {
        // Load model from test resources
        var yamlLoader = new YamlModelLoader();
        var modelUrl = getClass().getClassLoader().getResource("orders-sdd-model.yaml");
        assertThat(modelUrl)
                .as("orders-sdd-model.yaml should exist in test resources")
                .isNotNull();

        var modelPath = Paths.get(modelUrl.getPath());
        var result = yamlLoader.loadFromFile(modelPath);

        assertThat(result.isSuccess())
                .withFailMessage("Failed to load model: %s", result.isFailure() ? result.getCause() : "")
                .isTrue();

        var model = result.get();
        var generator = DdlGenerators.forDialect("postgres");
        var ddl = generator.generateDdl(model);

        // Verify entity table
        assertThat(ddl).contains("CREATE TABLE public.orders");

        // Verify state tables
        assertThat(ddl).contains("CREATE TABLE public.order_pending");
        assertThat(ddl).contains("CREATE TABLE public.order_paid");
        assertThat(ddl).contains("CREATE TABLE public.order_cancelled");
        assertThat(ddl).contains("CREATE TABLE public.order_refunded");

        // Verify OR transition mapping table
        assertThat(ddl).contains("CREATE TABLE public.cancelled_source");
        assertThat(ddl).contains("pending_state_id INTEGER REFERENCES order_pending(id)");
        assertThat(ddl).contains("paid_state_id INTEGER REFERENCES order_paid(id)");

        // Verify CHECK constraint for OR transitions
        assertThat(ddl).contains("ALTER TABLE cancelled_source ADD CONSTRAINT cancelled_source_check");

        // Verify extension tables
        assertThat(ddl).contains("CREATE TABLE public.order_paid_extensions");
        assertThat(ddl).contains("CREATE TABLE public.order_cancelled_extensions");

        // Verify intervals view
        assertThat(ddl).contains("CREATE VIEW public.order_state_intervals AS");
        assertThat(ddl).contains("UNION ALL");
        assertThat(ddl).contains("'PENDING' AS state_type");
        assertThat(ddl).contains("'PAID' AS state_type");
        assertThat(ddl).contains("'CANCELLED' AS state_type");
        assertThat(ddl).contains("'REFUNDED' AS state_type");
        assertThat(ddl).contains("start_at");
        assertThat(ddl).contains("end_at");

        // Verify current state view
        assertThat(ddl).contains("CREATE VIEW public.current_order_states AS");
        assertThat(ddl).contains("FROM order_state_intervals");
        assertThat(ddl).contains("WHERE end_at IS NULL");
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
