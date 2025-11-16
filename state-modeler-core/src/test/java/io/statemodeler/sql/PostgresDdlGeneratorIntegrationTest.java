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

        // Entity table in entity schema (public)
        assertThat(ddl).contains("CREATE TABLE public.orders");
        assertThat(ddl).contains("id serial NOT NULL");
        assertThat(ddl).contains("customer_id int NOT NULL");

        // State tables in state schema (public_states)
        assertThat(ddl).contains("CREATE SCHEMA IF NOT EXISTS public_states");
        assertThat(ddl).contains("CREATE TABLE public_states.order_pending");
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

        // Verify schema creation
        assertThat(ddl).contains("CREATE SCHEMA IF NOT EXISTS public_states");

        // Verify entity table in entity schema
        assertThat(ddl).contains("CREATE TABLE public.orders");

        // Verify state tables in state schema
        assertThat(ddl).contains("CREATE TABLE public_states.order_pending");
        assertThat(ddl).contains("CREATE TABLE public_states.order_paid");
        assertThat(ddl).contains("CREATE TABLE public_states.order_cancelled");
        assertThat(ddl).contains("CREATE TABLE public_states.order_refunded");

        // Verify OR transition mapping table in state schema
        assertThat(ddl).contains("CREATE TABLE public_states.cancelled_source");
        assertThat(ddl).contains("pending_state_id INTEGER REFERENCES order_pending(id)");
        assertThat(ddl).contains("paid_state_id INTEGER REFERENCES order_paid(id)");

        // Verify CHECK constraint for OR transitions
        assertThat(ddl).contains("ALTER TABLE cancelled_source ADD CONSTRAINT cancelled_source_check");

        // Verify extension tables in state schema
        assertThat(ddl).contains("CREATE TABLE public_states.order_paid_extensions");
        assertThat(ddl).contains("CREATE TABLE public_states.order_cancelled_extensions");

        // Verify intervals view in state schema
        assertThat(ddl).contains("CREATE VIEW public_states.order_state_intervals AS");
        assertThat(ddl).contains("UNION ALL");
        assertThat(ddl).contains("'PENDING' AS state_type");
        assertThat(ddl).contains("'PAID' AS state_type");
        assertThat(ddl).contains("'CANCELLED' AS state_type");
        assertThat(ddl).contains("'REFUNDED' AS state_type");
        assertThat(ddl).contains("start_at");
        assertThat(ddl).contains("end_at");

        // Verify current state view in state schema
        assertThat(ddl).contains("CREATE VIEW public_states.current_order_states AS");
        assertThat(ddl).contains("FROM order_state_intervals");
        assertThat(ddl).contains("WHERE end_at IS NULL");
    }

    @Test
    void shouldGenerateWithCustomStateSchema() throws Exception {
        // Given - model with custom state schema
        var database = new DatabaseConfig("postgres", "myapp", "custom_states");
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);
        var nameAttr = new AttributeDef("name", "text", false, false, null, null);

        var initialState = new StateDef(
                "draft",
                "document_draft",
                true,
                List.of(),
                List.of(),
                Map.of("content", new AttributeDef("content", "text", false, false, null, null)));

        var entity = new EntityDef(
                "document",
                "documents",
                idAttr,
                Map.of("name", nameAttr),
                Map.of("draft", initialState),
                Map.of(),
                Map.of());

        var model = new SddModel("0.1", "test-custom-schema", database, Map.of("document", entity));

        // When
        var generator = DdlGenerators.forDialect("postgres");
        var ddl = generator.generateDdl(model);

        // Then - verify custom schemas are created
        assertThat(ddl).contains("CREATE SCHEMA IF NOT EXISTS myapp");
        assertThat(ddl).contains("CREATE SCHEMA IF NOT EXISTS custom_states");

        // Entity table in entity schema
        assertThat(ddl).contains("CREATE TABLE myapp.documents");

        // State table in custom state schema
        assertThat(ddl).contains("CREATE TABLE custom_states.document_draft");
    }

    @Test
    void shouldGenerateWithNullSchemaDefaultingToStates() throws Exception {
        // Given - model with null schemas (tests default behavior)
        var database = new DatabaseConfig("postgres", null, null);
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        var initialState = new StateDef(
                "active",
                "task_active",
                true,
                List.of(),
                List.of(),
                Map.of("status", new AttributeDef("status", "text", false, false, null, null)));

        var entity =
                new EntityDef("task", "tasks", idAttr, Map.of(), Map.of("active", initialState), Map.of(), Map.of());

        var model = new SddModel("0.1", "test-null-schema", database, Map.of("task", entity));

        // When
        var generator = DdlGenerators.forDialect("postgres");
        var ddl = generator.generateDdl(model);

        // Then - no CREATE SCHEMA for public (default), but CREATE SCHEMA for states
        assertThat(ddl).doesNotContain("CREATE SCHEMA IF NOT EXISTS public");
        assertThat(ddl).contains("CREATE SCHEMA IF NOT EXISTS states");

        // Entity table without schema prefix (default schema)
        assertThat(ddl).contains("CREATE TABLE tasks");
        assertThat(ddl).doesNotContain("CREATE TABLE public.tasks");

        // State table in 'states' schema
        assertThat(ddl).contains("CREATE TABLE states.task_active");
    }

    @Test
    void shouldGenerateWithPublicStateSchema() throws Exception {
        // Given - model with custom entity schema but 'public' state schema
        var database = new DatabaseConfig("postgres", "myapp", "public");
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        var initialState = new StateDef(
                "active",
                "project_active",
                true,
                List.of(),
                List.of(),
                Map.of("status", new AttributeDef("status", "text", false, false, null, null)));

        var entity = new EntityDef(
                "project", "projects", idAttr, Map.of(), Map.of("active", initialState), Map.of(), Map.of());

        var model = new SddModel("0.1", "test-public-state-schema", database, Map.of("project", entity));

        // When
        var generator = DdlGenerators.forDialect("postgres");
        var ddl = generator.generateDdl(model);

        // Then - CREATE SCHEMA for myapp, but NOT for public (state schema)
        assertThat(ddl).contains("CREATE SCHEMA IF NOT EXISTS myapp");
        assertThat(ddl).doesNotContain("CREATE SCHEMA IF NOT EXISTS public");

        // Entity table in custom schema
        assertThat(ddl).contains("CREATE TABLE myapp.projects");

        // State table in public schema (default)
        assertThat(ddl).contains("CREATE TABLE public.project_active");
    }

    private SddModel createSimpleOrderModel() {
        var database = new DatabaseConfig("postgres", "public", null);

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
