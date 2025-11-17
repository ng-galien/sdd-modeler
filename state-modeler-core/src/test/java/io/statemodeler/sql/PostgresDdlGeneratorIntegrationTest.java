package io.statemodeler.sql;

import static org.junit.jupiter.api.Assertions.*;

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
        assertTrue(ddl.contains("CREATE TABLE public.orders"));
        assertTrue(ddl.contains("id serial PRIMARY KEY"));
        assertTrue(ddl.contains("customer_id int NOT NULL"));

        // State tables in state schema (public_states)
        assertTrue(ddl.contains("CREATE SCHEMA IF NOT EXISTS public_states"));
        assertTrue(ddl.contains("CREATE TABLE public_states.order_pending"));
        assertTrue(ddl.contains("pending_reason text NOT NULL"));
    }

    @Test
    void shouldGenerateCompleteOrdersDdlFromYaml() throws Exception {
        // Load model from test resources
        var yamlLoader = new YamlModelLoader();
        var modelUrl = getClass().getClassLoader().getResource("orders-sdd-model.yaml");
        assertNotNull(modelUrl, "orders-sdd-model.yaml should exist in test resources");

        var modelPath = Paths.get(modelUrl.getPath());
        var result = yamlLoader.loadFromFile(modelPath);

        assertTrue(result.isSuccess(), "Failed to load model: " + (result.isFailure() ? result.getCause() : ""));

        var model = result.get();
        var generator = DdlGenerators.forDialect("postgres");
        var ddl = generator.generateDdl(model);

        // Verify schema creation
        assertTrue(ddl.contains("CREATE SCHEMA IF NOT EXISTS public_states"));

        // Verify entity table in entity schema
        assertTrue(ddl.contains("CREATE TABLE public.orders"));

        // Verify state tables in state schema
        assertTrue(ddl.contains("CREATE TABLE public_states.order_pending"));
        assertTrue(ddl.contains("CREATE TABLE public_states.order_paid"));
        assertTrue(ddl.contains("CREATE TABLE public_states.order_cancelled"));
        assertTrue(ddl.contains("CREATE TABLE public_states.order_refunded"));

        // Verify OR transition mapping table in state schema
        assertTrue(ddl.contains("CREATE TABLE public_states.cancelled_source"));
        // FK now added as ALTER TABLE constraints (not inline)
        assertTrue(
                ddl.contains("ALTER TABLE public_states.cancelled_source ADD CONSTRAINT cancelled_source_pending_fk"));
        assertTrue(ddl.contains("ALTER TABLE public_states.cancelled_source ADD CONSTRAINT cancelled_source_paid_fk"));

        // Verify CHECK constraint for OR transitions
        assertTrue(ddl.contains("ALTER TABLE public_states.cancelled_source ADD CONSTRAINT cancelled_source_check"));

        // Verify extension tables in state schema
        assertTrue(ddl.contains("CREATE TABLE public_states.order_paid_extensions"));
        assertTrue(ddl.contains("CREATE TABLE public_states.order_cancelled_extensions"));

        // Verify intervals view in state schema
        assertTrue(ddl.contains("CREATE VIEW public_states.order_state_intervals AS"));
        assertTrue(ddl.contains("UNION ALL"));
        assertTrue(ddl.contains("'PENDING' AS state_type"));
        assertTrue(ddl.contains("'PAID' AS state_type"));
        assertTrue(ddl.contains("'CANCELLED' AS state_type"));
        assertTrue(ddl.contains("'REFUNDED' AS state_type"));
        assertTrue(ddl.contains("start_at"));
        assertTrue(ddl.contains("end_at"));

        // Verify current state view in state schema
        assertTrue(ddl.contains("CREATE VIEW public_states.current_order_states AS"));
        assertTrue(ddl.contains("FROM public_states.order_state_intervals"));
        assertTrue(ddl.contains("WHERE end_at IS NULL"));
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
        assertTrue(ddl.contains("CREATE SCHEMA IF NOT EXISTS myapp"));
        assertTrue(ddl.contains("CREATE SCHEMA IF NOT EXISTS custom_states"));

        // Entity table in entity schema
        assertTrue(ddl.contains("CREATE TABLE myapp.documents"));

        // State table in custom state schema
        assertTrue(ddl.contains("CREATE TABLE custom_states.document_draft"));
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
        assertFalse(ddl.contains("CREATE SCHEMA IF NOT EXISTS public"));
        assertTrue(ddl.contains("CREATE SCHEMA IF NOT EXISTS states"));

        // Entity table without schema prefix (default schema)
        assertTrue(ddl.contains("CREATE TABLE tasks"));
        assertFalse(ddl.contains("CREATE TABLE public.tasks"));

        // State table in 'states' schema
        assertTrue(ddl.contains("CREATE TABLE states.task_active"));
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
        assertTrue(ddl.contains("CREATE SCHEMA IF NOT EXISTS myapp"));
        assertFalse(ddl.contains("CREATE SCHEMA IF NOT EXISTS public"));

        // Entity table in custom schema
        assertTrue(ddl.contains("CREATE TABLE myapp.projects"));

        // State table in public schema (default)
        assertTrue(ddl.contains("CREATE TABLE public.project_active"));
    }

    @Test
    void shouldGenerateWithEmptyStringSchemasDefaultingToStates() throws Exception {
        // Given - model with empty string schemas (should be treated as null)
        var database = new DatabaseConfig("postgres", "", "");
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        var initialState = new StateDef(
                "active",
                "resource_active",
                true,
                List.of(),
                List.of(),
                Map.of("status", new AttributeDef("status", "text", false, false, null, null)));

        var entity = new EntityDef(
                "resource", "resources", idAttr, Map.of(), Map.of("active", initialState), Map.of(), Map.of());

        var model = new SddModel("0.1", "test-empty-string-schema", database, Map.of("resource", entity));

        // When
        var generator = DdlGenerators.forDialect("postgres");
        var ddl = generator.generateDdl(model);

        // Then - empty strings treated as null, same behavior as null schemas
        assertFalse(ddl.contains("CREATE SCHEMA IF NOT EXISTS public"));
        assertTrue(ddl.contains("CREATE SCHEMA IF NOT EXISTS states"));

        // Entity table without schema prefix (default schema)
        assertTrue(ddl.contains("CREATE TABLE resources"));
        assertFalse(ddl.contains("CREATE TABLE .resources")); // No invalid SQL

        // State table in 'states' schema
        assertTrue(ddl.contains("CREATE TABLE states.resource_active"));
        assertFalse(ddl.contains("CREATE TABLE .resource_active")); // No invalid SQL
    }

    @Test
    void shouldGenerateIndexesOnForeignKeys() throws Exception {
        // Given - model with state transitions (which create FK relationships)
        var database = new DatabaseConfig("postgres", "public", null);
        var idAttr = new AttributeDef("id", "serial", false, true, null, null);

        var pendingState = new StateDef("pending", "order_pending", true, List.of(), List.of(), Map.of());

        var paidState = new StateDef(
                "paid",
                "order_paid",
                false,
                List.of("pending"), // transition from pending
                List.of(),
                Map.of("payment_method", new AttributeDef("payment_method", "text", false, false, null, null)));

        var entity = new EntityDef(
                "order",
                "orders",
                idAttr,
                Map.of(),
                Map.of("pending", pendingState, "paid", paidState),
                Map.of(),
                Map.of());

        var model = new SddModel("0.1", "test-indexes", database, Map.of("order", entity));

        // When
        var generator = DdlGenerators.forDialect("postgres");
        var ddl = generator.generateDdl(model);

        // Then - verify indexes are created for FK columns (except entity_id which has UNIQUE constraint)
        // No index on order_paid.order_id - UNIQUE constraint creates implicit index
        assertFalse(ddl.contains("CREATE INDEX idx_order_paid_order_id ON public_states.order_paid (order_id);"));

        // Index on order_paid.previous_pending_id (FK to order_pending)
        assertTrue(ddl.contains(
                "CREATE INDEX idx_order_paid_previous_pending_id ON public_states.order_paid (previous_pending_id);"));

        // No index on order_pending.order_id - UNIQUE constraint creates implicit index
        assertFalse(ddl.contains("CREATE INDEX idx_order_pending_order_id ON public_states.order_pending (order_id);"));

        // Verify UNIQUE constraints are created on entity_id
        assertTrue(ddl.contains("ALTER TABLE public_states.order_paid ADD CONSTRAINT order_paid_order_id_unique UNIQUE (order_id);"));
        assertTrue(ddl.contains("ALTER TABLE public_states.order_pending ADD CONSTRAINT order_pending_order_id_unique UNIQUE (order_id);"));
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
