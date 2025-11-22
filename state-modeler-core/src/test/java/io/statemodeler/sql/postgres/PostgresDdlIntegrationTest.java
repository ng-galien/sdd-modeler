package io.statemodeler.sql.postgres;

import static org.junit.jupiter.api.Assertions.*;

import io.statemodeler.core.SddModel;
import io.statemodeler.dsl.YamlModelLoader;
import io.statemodeler.sql.DdlGenerators;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for PostgreSQL DDL generation.
 *
 * <p>Uses Testcontainers to spin up a real PostgreSQL instance and verify that generated DDL can
 * be executed without errors.
 */
@Testcontainers
class PostgresDdlIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sdd_test")
            .withUsername("test")
            .withPassword("test");

    @BeforeEach
    void cleanDatabase() throws Exception {
        // Clean up database before each test to ensure isolation
        try (Connection conn = DriverManager.getConnection(
                        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                Statement stmt = conn.createStatement()) {
            stmt.execute("DROP SCHEMA IF EXISTS public_states CASCADE");
            stmt.execute("DROP SCHEMA IF EXISTS public CASCADE");
            stmt.execute("CREATE SCHEMA public");
        }
    }

    @Test
    void shouldExecuteGeneratedDdlSuccessfully() throws Exception {
        // Given - Load the orders example model
        var yamlLoader = new YamlModelLoader();
        Path modelPath = Path.of("src/test/resources/orders-sdd-model.yaml");
        assertTrue(Files.exists(modelPath), "Test model file should exist");

        var modelResult = yamlLoader.loadFromFile(modelPath);
        assertTrue(modelResult.isSuccess(), "Model should load successfully");
        SddModel model = modelResult.get();

        // When - Generate DDL
        var generator = DdlGenerators.forDialect("postgres");
        String ddl = generator.generateDdl(model);

        assertNotNull(ddl);
        assertFalse(ddl.isBlank(), "DDL should not be blank");

        // Then - Execute DDL in PostgreSQL container
        try (Connection conn =
                DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            try (Statement stmt = conn.createStatement()) {
                // Split DDL by statements (each ending with semicolon + newline)
                // and execute them one by one
                for (String statement : ddl.split(";\\s*\\n")) {
                    statement = statement.trim();
                    if (!statement.isEmpty()) {
                        // Debugging print removed: rely on test failure logs
                        stmt.execute(statement + ";");
                    }
                }
            }

            // Verify entity table was created in public schema
            try (var stmt = conn.createStatement();
                    var rs = stmt.executeQuery(
                            "SELECT tablename FROM pg_catalog.pg_tables WHERE schemaname = 'public' ORDER BY tablename")) {

                var tables = new java.util.ArrayList<String>();
                while (rs.next()) {
                    tables.add(rs.getString("tablename"));
                }

                // Verify expected entity table exists
                assertTrue(tables.contains("orders"), "Entity table 'orders' should exist in public schema");
            }

            // Verify state tables were created in public_states schema
            try (var stmt = conn.createStatement();
                    var rs = stmt.executeQuery(
                            "SELECT tablename FROM pg_catalog.pg_tables WHERE schemaname = 'public_states' ORDER BY tablename")) {

                var stateTables = new java.util.ArrayList<String>();
                while (rs.next()) {
                    stateTables.add(rs.getString("tablename"));
                }

                // Verify expected state tables exist
                assertTrue(stateTables.contains("order_pending"), "State table 'order_pending' should exist");
                assertTrue(stateTables.contains("order_paid"), "State table 'order_paid' should exist");
                assertTrue(stateTables.contains("order_refunded"), "State table 'order_refunded' should exist");
                assertTrue(stateTables.contains("order_cancelled"), "State table 'order_cancelled' should exist");
                assertTrue(
                        stateTables.contains("order_paid_extensions"),
                        "Extension table 'order_paid_extensions' should exist");
                assertTrue(
                        stateTables.contains("cancelled_source"),
                        "OR transition mapping table 'cancelled_source' should exist");
            }

            // Verify views were created
            try (var stmt = conn.createStatement();
                    var rs = stmt.executeQuery(
                            "SELECT viewname FROM pg_catalog.pg_views WHERE schemaname = 'public_states' ORDER BY viewname")) {

                var views = new java.util.ArrayList<String>();
                while (rs.next()) {
                    views.add(rs.getString("viewname"));
                }

                assertTrue(
                        views.contains("order_state_intervals"), "Interval view 'order_state_intervals' should exist");
                assertTrue(
                        views.contains("current_order_states"),
                        "Current state view 'current_order_states' should exist");
            }
        }
    }

    @Test
    void shouldExecuteDdlWithConstraintsAndIndexes() throws Exception {
        // Given
        var yamlLoader = new YamlModelLoader();
        Path modelPath = Path.of("src/test/resources/orders-sdd-model.yaml");
        var model = yamlLoader.loadFromFile(modelPath).get();
        var generator = DdlGenerators.forDialect("postgres");
        String ddl = generator.generateDdl(model);

        // When - Execute DDL
        try (Connection conn =
                DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            try (Statement stmt = conn.createStatement()) {
                for (String statement : ddl.split(";\\s*\\n")) {
                    statement = statement.trim();
                    if (!statement.isEmpty()) {
                        stmt.execute(statement + ";");
                    }
                }
            }

            // Then - Verify foreign key constraints exist
            try (var stmt = conn.createStatement();
                    var rs = stmt.executeQuery(
                            "SELECT conname FROM pg_constraint WHERE contype = 'f' ORDER BY conname")) {

                var fkConstraints = new java.util.ArrayList<String>();
                while (rs.next()) {
                    fkConstraints.add(rs.getString("conname"));
                }

                assertFalse(fkConstraints.isEmpty(), "Should have foreign key constraints");
                // Verify at least one FK to entity table exists (named like "order_paid_order_id_fk")
                assertTrue(
                        fkConstraints.stream().anyMatch(fk -> fk.contains("order_id_fk")),
                        "Should have FK constraints referencing entity table");
            }

            // Verify indexes were created (including automatic FK indexes)
            try (var stmt = conn.createStatement();
                    var rs = stmt.executeQuery(
                            "SELECT indexname FROM pg_indexes WHERE schemaname IN ('public', 'public_states') ORDER BY indexname")) {

                var indexes = new java.util.ArrayList<String>();
                while (rs.next()) {
                    indexes.add(rs.getString("indexname"));
                }

                assertFalse(indexes.isEmpty(), "Should have indexes");
                // Verify automatic FK indexes exist (pattern: idx_<table>_<column>)
                assertTrue(
                        indexes.stream().anyMatch(idx -> idx.startsWith("idx_order_")),
                        "Should have automatic FK indexes");
            }
        }
    }

    @Test
    void shouldHandleInsertIntoGeneratedSchema() throws Exception {
        // Given - Setup schema
        var yamlLoader = new YamlModelLoader();
        Path modelPath = Path.of("src/test/resources/orders-sdd-model.yaml");
        var model = yamlLoader.loadFromFile(modelPath).get();
        var generator = DdlGenerators.forDialect("postgres");
        String ddl = generator.generateDdl(model);

        try (Connection conn =
                DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {

            try (Statement stmt = conn.createStatement()) {
                for (String statement : ddl.split(";\\s*\\n")) {
                    statement = statement.trim();
                    if (!statement.isEmpty()) {
                        stmt.execute(statement + ";");
                    }
                }
            }

            // When - Insert test data
            try (Statement stmt = conn.createStatement()) {
                // Insert entity
                stmt.execute("INSERT INTO orders (customer_id, total_amount) VALUES (123, 99.99)");

                // Insert initial state
                stmt.execute(
                        "INSERT INTO public_states.order_pending (order_id, pending_reason) VALUES (1, 'awaiting payment')");
            }

            // Then - Verify data was inserted
            try (var stmt = conn.createStatement();
                    var rs = stmt.executeQuery("SELECT COUNT(*) FROM orders")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "Should have 1 order");
            }

            try (var stmt = conn.createStatement();
                    var rs = stmt.executeQuery("SELECT COUNT(*) FROM public_states.order_pending")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "Should have 1 pending state");
            }

            // Verify current state view
            try (var stmt = conn.createStatement();
                    var rs = stmt.executeQuery("SELECT state_type FROM public_states.current_order_states")) {
                assertTrue(rs.next());
                assertEquals("PENDING", rs.getString("state_type"), "Current state should be 'PENDING'");
            }
        }
    }
}
